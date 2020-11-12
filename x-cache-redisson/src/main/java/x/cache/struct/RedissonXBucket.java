package x.cache.struct;

import com.alibaba.fastjson.JSONObject;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.redisson.api.*;
import org.redisson.api.listener.MessageListener;
import x.cache.exception.XCacheException;
import x.cache.model.XCacheEvent;
import x.cache.model.XCacheObject;

import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class RedissonXBucket<E> implements XBucket<E>, MessageListener<XCacheEvent>
{
    private RedissonClient redisson;
    private AutoRefreshExecutor autoRefreshExecutor;
    private RedissonXBucketConfig config;
    private Cache<String, XCacheObject<E>> localCache;

    private RTopic rTopic;
    private RAtomicLong rSeq;


    public RedissonXBucket(RedissonClient redisson, AutoRefreshExecutor autoRefreshExecutor, RedissonXBucketConfig config)
    {
        this.redisson = redisson;
        this.autoRefreshExecutor = autoRefreshExecutor;
        this.config = config;

        if (config.getLocalConfig().isEnabled()) {
            this.localCache = CacheBuilder.newBuilder()
                    .expireAfterWrite(config.getLocalConfig().getTimeout(), config.getLocalConfig().getUnit())
                    .maximumSize(config.getLocalConfig().getMaximumSize())
                    .build();
        }

        if (config.getTopicConfig().isEnabled()) {
            rTopic = redisson.getTopic(config.getTopicConfig().getName(), config.getTopicConfig().getCodec());
            rTopic.addListener(XCacheEvent.class, this);
            rSeq = redisson.getAtomicLong(config.getTopicConfig().getName() + ":seq");
        }
    }


    /**
     * 在Version版本变更时候，直接返回旧缓存异步更新， 或者同步更新返回新缓存结果
     * <p>
     */
    interface UpdateHandler<E>
    {
        E handle(String key, Integer version, Callable<E> callable);
    }


    public UpdateHandler<E> syncUpdateHandler()
    {
        return (k, v, c) -> doAutoRefresh(k, v, c).getObject();
    }

    public UpdateHandler<E> asyncUpdateHandler()
    {
        return (k, v, c) -> {
            doAutoRefreshAsync(k, v, c);
            return null;
        };
    }


    @Override
    public void put(String key, E e)
    {
        XCacheObject<E> xCacheObject = ofXCacheObject(e);
        setBucket(key, xCacheObject);
        setLocalCache(key, xCacheObject);
        publishIfAuto(key, XCacheEvent.ACTION_SAVE, XCacheEvent.LEVEL_LOCAL, xCacheObject);
    }

    @Override
    public void put(String key, E e, Integer version)
    {
        XCacheObject<E> xCacheObject = XCacheObject.of(e, version);
        setBucket(key, xCacheObject);
        setLocalCache(key, xCacheObject);
        publishIfAuto(key, XCacheEvent.ACTION_SAVE, XCacheEvent.LEVEL_LOCAL, xCacheObject);
    }

    @Override
    public void del(String key)
    {
        if (config.getLocalConfig().isEnabled()) {
            localCache.invalidate(key);
        }
        getBucket(key).deleteAsync();
        publishIfAuto(key, XCacheEvent.ACTION_DEL, XCacheEvent.LEVEL_LOCAL, null);
    }

    @Override
    public E getIfPresent(String key)
    {
        return execute(key, null, (xCacheObject) -> xCacheObject.getObject());
    }

    @Override
    public E getAutoRefresh(String key, Callable<E> callable)
    {
        return execute(key, callable, (xCacheObject) -> {
            autoRefreshAsync(key, callable, xCacheObject);
            return xCacheObject.getObject();
        });
    }

    @Override
    public E autoRefreshGet(String key, Callable<E> callable)
    {
        return execute(key, callable, (xCacheObject) -> autoRefresh(key, callable, xCacheObject).getObject());
    }


    @Override
    public E getAutoRefresh(String key, Integer version, Callable<E> callable)
    {
        return execute(key, version, callable, (xCacheObject) -> {
            autoRefreshAsync(key, version, callable, xCacheObject);
            return xCacheObject.getObject();
        }, asyncUpdateHandler());
    }

    @Override
    public E autoRefreshGet(String key, Integer version, Callable<E> callable)
    {
        return execute(key, version, callable, (xCacheObject) -> autoRefresh(key, version, callable, xCacheObject).getObject(), syncUpdateHandler());
    }


    private E execute(String key, Callable<E> callable, Function<XCacheObject<E>, E> cacheHitFunc)
    {
        return execute(key, null, callable, cacheHitFunc, null);
    }

    private E execute(String key, Integer version, Callable<E> callable, Function<XCacheObject<E>, E> cacheHitFunc, UpdateHandler<E> versionChangeHandler)
    {
        // 本地缓存获取
        if (config.getLocalConfig().isEnabled()) {
            XCacheObject<E> localXCacheObject = localCache.getIfPresent(key);
            if (localXCacheObject != null && !noNeedToUpdateVersion(localXCacheObject.getVersion(), version) && versionChangeHandler != null) {
                return versionChangeHandler.handle(key, version, callable);
            }
            if (localXCacheObject != null && noNeedToUpdateVersion(localXCacheObject.getVersion(), version)) {
                return cacheHitFunc.apply(localXCacheObject);
            }
        }

        // 重redis中获取
        XCacheObject<E> redisXCacheObject = getBucket(key).get();
        if (redisXCacheObject != null && !noNeedToUpdateVersion(redisXCacheObject.getVersion(), version) && versionChangeHandler != null) {
            return versionChangeHandler.handle(key, version, callable);
        }
        if (redisXCacheObject != null && noNeedToUpdateVersion(redisXCacheObject.getVersion(), version)) {
            if (config.getLocalConfig().isEnabled()) {
                setLocalCache(key, redisXCacheObject);
            }
            return cacheHitFunc.apply(redisXCacheObject);
        }

        // 从loader中获取
        if (callable == null) {
            return null;
        }

        RLock rLock = getBucketLock(key);
        try {
            rLock.lock(2, TimeUnit.SECONDS);
            if (getBucket(key).get() != null) {
                return getBucket(key).get().getObject();
            }
            return callXCacheObject(key, version, callable).getObject();
        } finally {
            rLock.unlock();
        }
    }


    private boolean noNeedToUpdateVersion(Integer src, Integer tar)
    {
        src = src != null ? src : 0;
        tar = tar != null ? tar : 0;
        return src >= tar;
    }


    private XCacheObject<E> autoRefresh(String key, Callable<E> callable, XCacheObject<E> current)
    {
        return autoRefresh(key, null, callable, current);
    }

    private XCacheObject<E> autoRefresh(String key, Integer version, Callable<E> callable, XCacheObject<E> current)
    {
        Integer oldVersion = current.getVersion() != null ? current.getVersion() : 0;
        if (version != null && version > oldVersion) {
            return doAutoRefresh(key, version, callable);
        }
        // 判断这个缓存已经过期
        if (current.getExpireAt() != null && current.getExpireAt().before(new Date())) {
            return doAutoRefresh(key, version, callable);
        }
        return current;
    }

    private XCacheObject<E> doAutoRefresh(String key, Integer version, Callable<E> callable)
    {
        return callXCacheObject(key, version, callable);
    }

    private void autoRefreshAsync(String key, Callable<E> callable, XCacheObject<E> current)
    {
        autoRefreshAsync(key, null, callable, current);
    }

    private void autoRefreshAsync(String key, Integer version, Callable<E> callable, XCacheObject<E> current)
    {
        // 判断version是不是不一样
        Integer oldVersion = current.getVersion() != null ? current.getVersion() : 0;
        if (version != null && version > oldVersion) {
            doAutoRefreshAsync(key, version, callable);
            return;
        }

        // 判断这个缓存已经过期
        if (current.getExpireAt() != null && current.getExpireAt().before(new Date())) {
            doAutoRefreshAsync(key, version, callable);
        }
    }

    private void doAutoRefreshAsync(String key, Integer version, Callable<E> callable)
    {
        autoRefreshExecutor.asyncExec(() -> {
            RBucket<Integer> trySetBucket = getTrySetBucket(key);
            boolean trySet = trySetBucket.trySet(1, 1, TimeUnit.SECONDS);
            if (trySet) {
                callXCacheObject(key, version, callable);
            }
        });
    }


    private XCacheObject<E> callXCacheObject(String key, Integer version, Callable<E> callable)
    {
        E object = doCall(callable);
        XCacheObject<E> xCacheObject = ofXCacheObject(object, version);
        setBucket(key, xCacheObject);
        setLocalCache(key, xCacheObject);
        publishIfAuto(key, XCacheEvent.ACTION_SAVE, XCacheEvent.LEVEL_LOCAL, xCacheObject);
        return xCacheObject;
    }

    private void setLocalCache(String key, XCacheObject<E> xCacheObject)
    {
        if (config.getLocalConfig().isEnabled()) {
            localCache.put(key, xCacheObject);
        }
    }

    private void setBucket(String key, XCacheObject<E> e)
    {
        getBucket(key).set(e, config.getRedisConfig().getTimeout(), config.getRedisConfig().getUnit());
    }

    private XCacheObject<E> ofXCacheObject(E e)
    {
        return ofXCacheObject(e, null);
    }

    private XCacheObject<E> ofXCacheObject(E e, Integer version)
    {
        if (config.getObjectConfig() == null || config.getObjectConfig().getTimeout() == null) {
            return XCacheObject.of(e, version);
        }
        return XCacheObject.of(e, version, config.getObjectConfig().getTimeout(), config.getObjectConfig().getUnit());
    }


    private RLock getBucketLock(String key)
    {
        return redisson.getLock(key + ":lock");
    }

    private RBucket<Integer> getTrySetBucket(String key)
    {
        return redisson.getBucket(key + ":try_set");
    }

    private RBucket<XCacheObject<E>> getBucket(String key)
    {
        return redisson.getBucket(key);
    }

    private RBucket<Long> getSeqBucket(String key, long seq)
    {
        return redisson.getBucket(key + ":seq:" + seq);
    }


    public void publishIfAuto(String key, int action, int level, XCacheObject<E> xCacheObject)
    {
        if (config.getTopicConfig().isEnabled() && config.getTopicConfig().isAutoPublish()) {
            XCacheEvent xCacheEvent = XCacheEvent.builder()
                    .key(key)
                    .seq(rSeq.getAndIncrement())
                    .action(XCacheEvent.ACTION_SAVE)
                    .level(XCacheEvent.LEVEL_LOCAL)
                    .xCacheObject(xCacheObject)
                    .build();
            rTopic.publish(xCacheEvent);
        }
    }


    @Override
    public void onMessage(CharSequence channel, XCacheEvent msg)
    {
        switch (msg.getAction()) {
            case XCacheEvent.ACTION_SAVE:
                doActionSaveMessage(msg);
                break;
            case XCacheEvent.ACTION_DEL:
                doActionDelMessage(msg);
                break;
            default:
                throw new XCacheException("XCacheEvent没有指定action类型. msg=" + JSONObject.toJSONString(msg));
        }
    }

    private void doActionDelMessage(XCacheEvent msg)
    {
        // 清除redis缓存
        if (msg.getLevel() == XCacheEvent.LEVEL_ALL) {
            RBucket<Long> seqBucket = getSeqBucket(msg.getKey(), msg.getSeq());
            if (seqBucket.trySet(msg.getSeq(), 1, TimeUnit.MINUTES)) {
                getBucket(msg.getKey()).deleteAsync();
            }
        }
        // 清除本地缓存
        if (config.getLocalConfig().isEnabled()) {
            localCache.invalidate(msg.getKey());
        }
    }

    private void doActionSaveMessage(XCacheEvent msg)
    {
        if (config.getLocalConfig().isEnabled() && msg.getXCacheObject() != null) {
            setLocalCache(msg.getKey(), (XCacheObject<E>) msg.getXCacheObject());
        }
        if (msg.getLevel() == XCacheEvent.LEVEL_ALL) {
            RBucket<Long> seqBucket = getSeqBucket(msg.getKey(), msg.getSeq());
            if (seqBucket.trySet(msg.getSeq(), 1, TimeUnit.MINUTES)) {
                setBucket(msg.getKey(), (XCacheObject<E>) msg.getXCacheObject());
            }
        }
    }


}
