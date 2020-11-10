package x.cache.struct;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import x.cache.model.XCacheObject;

import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class RedissonXBucket<E> implements XBucket<E>
{
    private RedissonClient redisson;
    private AutoRefreshExecutor autoRefreshExecutor;
    private RedissonXBucketConfig config;

    private Cache<String, XCacheObject<E>> localCache;

    public RedissonXBucket(RedissonClient redisson, AutoRefreshExecutor autoRefreshExecutor, RedissonXBucketConfig config)
    {
        this.redisson = redisson;
        this.autoRefreshExecutor = autoRefreshExecutor;
        this.config = config;

        if (config.getLocalConfig().isUseLocalCache()) {
            this.localCache = CacheBuilder.newBuilder()
                    .expireAfterWrite(config.getLocalConfig().getTimeout(), config.getLocalConfig().getUnit())
                    .maximumSize(config.getLocalConfig().getMaximumSize())
                    .build();
        }
    }

    @Override
    public void put(String key, E e)
    {
        XCacheObject<E> xCacheObject = ofXCacheObject(e);
        setBucket(key, xCacheObject);
        setLocalCache(key, xCacheObject);
    }

    @Override
    public void put(String key, E e, Integer version)
    {
        XCacheObject<E> xCacheObject = XCacheObject.of(e, version);
        setBucket(key, xCacheObject);
        setLocalCache(key, xCacheObject);
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
        });
    }

    @Override
    public E autoRefreshGet(String key, Integer version, Callable<E> callable)
    {
        return execute(key, version, callable, (xCacheObject) -> autoRefresh(key, version, callable, xCacheObject).getObject());
    }


    private E execute(String key, Callable<E> callable, Function<XCacheObject<E>, E> cacheHitFunc)
    {
        return execute(key, null, callable, cacheHitFunc);
    }

    private E execute(String key, Integer version, Callable<E> callable, Function<XCacheObject<E>, E> cacheHitFunc)
    {
        // 本地缓存获取
        if (config.getLocalConfig().isUseLocalCache()) {
            XCacheObject<E> localXCacheObject = localCache.getIfPresent(key);
            // 传入版本大于旧的版本
            if(localXCacheObject != null && !noNeedToUpdateVersion(localXCacheObject.getVersion(), version)){
                return callXCacheObject(key, version, callable).getObject();
            }
            if (localXCacheObject != null && noNeedToUpdateVersion(localXCacheObject.getVersion(), version)) {
                return cacheHitFunc.apply(localXCacheObject);
            }
        }

        // 重redis中获取
        XCacheObject<E> redisXCacheObject = getBucket(key).get();
        if (redisXCacheObject != null && noNeedToUpdateVersion(redisXCacheObject.getVersion(), version)) {
            // 更新本地缓存
            if (config.getLocalConfig().isUseLocalCache()) {
                setLocalCache(key, redisXCacheObject);
            }
            return cacheHitFunc.apply(redisXCacheObject);
        }

        // 从loader中获取
        if (callable == null) {
            return null;
        }

        return callXCacheObject(key, version, callable).getObject();
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
            return callXCacheObject(key, version, callable);
        }
        // 判断这个缓存已经过期
        if (current.getExpireAt() != null && current.getExpireAt().before(new Date())) {
            return callXCacheObject(key, version, callable);
        }
        return current;
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
            autoRefreshExecutor.asyncExec(() -> callXCacheObject(key, version, callable));
            return;
        }

        // 判断这个缓存已经过期
        if (current.getExpireAt() != null && current.getExpireAt().before(new Date())) {
            autoRefreshExecutor.asyncExec(() -> {
                RBucket<Integer> trySetBucket = getTrySetBucket(key);
                boolean trySet = trySetBucket.trySet(1, 1, TimeUnit.SECONDS);
                if (trySet) {
                    callXCacheObject(key, version, callable);
                }
            });
        }
    }

    private XCacheObject<E> callXCacheObject(String key, Integer version, Callable<E> callable)
    {
        E object = doCall(callable);
        XCacheObject<E> new_ = ofXCacheObject(object, version);
        setBucket(key, new_);
        setLocalCache(key, new_);
        return new_;
    }

    private void setLocalCache(String key, XCacheObject<E> xCacheObject)
    {
        if (config.getLocalConfig().isUseLocalCache()) {
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


    private RBucket<Integer> getTrySetBucket(String key)
    {
        return redisson.getBucket(key + ":try_set");
    }

    private RBucket<XCacheObject<E>> getBucket(String key)
    {
        return redisson.getBucket(key);
    }


}
