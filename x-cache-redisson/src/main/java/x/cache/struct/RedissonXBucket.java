package x.cache.struct;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import x.cache.model.XCacheObject;

import java.util.Date;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

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
        return executeGet(key, null, (xCacheObject) -> {
            if (config.getLocalConfig().isUseLocalCache() && localCache.getIfPresent(key) == null) {
                setLocalCache(key, xCacheObject);
            }
            return xCacheObject.getObject();
        });
    }

    @Override
    public E getAutoRefresh(String key, Callable<E> callable)
    {
        return executeGet(key, callable, (xCacheObject) -> {
            autoRefreshAsync(key, callable, xCacheObject);
            if (config.getLocalConfig().isUseLocalCache() && localCache.getIfPresent(key) == null) {
                setLocalCache(key, xCacheObject);
            }
            return xCacheObject.getObject();
        });
    }

    @Override
    public E autoRefreshGet(String key, Callable<E> callable)
    {
        return executeGet(key, callable, (xCacheObject) -> autoRefresh(key, callable, xCacheObject).getObject());
    }

    private E executeGet(String key, Callable<E> callable, Function<XCacheObject<E>, E> cacheHitFunc)
    {
        // 本地缓存获取
        if (config.getLocalConfig().isUseLocalCache()) {
            XCacheObject<E> localXCacheObject = localCache.getIfPresent(key);
            if (localXCacheObject != null) {
                return cacheHitFunc.apply(localXCacheObject);
            }
        }

        // 重redis中获取
        XCacheObject<E> redisXCacheObject = getBucket(key).get();
        if (redisXCacheObject != null) {
            return cacheHitFunc.apply(redisXCacheObject);
        }

        // 从loader中获取
        if (callable == null) {
            return null;
        }

        E object = doCall(callable);
        XCacheObject<E> newXCacheObject = ofXCacheObject(object);
        setBucket(key, newXCacheObject);
        setLocalCache(key, newXCacheObject);
        return newXCacheObject.getObject();
    }


    @Override
    public E getByVersion(String key, Integer version, Callable<E> callable)
    {
        // 从本地缓存中获取
        if (config.getLocalConfig().isUseLocalCache()) {
            XCacheObject<E> localXCacheObject = obtainVersionXCacheObject(version, () -> localCache.getIfPresent(key));
            if (localXCacheObject != null) {
                return localXCacheObject.getObject();
            }
        }

        // 从redis中获取
        XCacheObject<E> redisCacheObject = obtainVersionXCacheObject(version, () -> getBucket(key).get());
        if (redisCacheObject != null) {
            setLocalCache(key, redisCacheObject);
            return redisCacheObject.getObject();
        }

        // 从callable中获取
        E object = doCall(callable);
        XCacheObject<E> new_ = XCacheObject.of(object, version);
        setBucket(key, new_);
        setLocalCache(key, new_);
        return new_.getObject();
    }

    private XCacheObject<E> obtainVersionXCacheObject(Integer version, Supplier<XCacheObject<E>> supplier)
    {
        XCacheObject<E> xCacheObject = supplier.get();
        Integer oldVersion = xCacheObject.getVersion() != null ? xCacheObject.getVersion() : 0;
        if (Objects.equals(oldVersion, version) || oldVersion >= version) {
            return xCacheObject;
        }
        return null;
    }


    private XCacheObject<E> autoRefresh(String key, Callable<E> callable, XCacheObject<E> current)
    {
        // 判断这个缓存已经过期
        if (current.getExpireAt() != null && current.getExpireAt().before(new Date())) {
            E object = doCall(callable);
            XCacheObject<E> new_ = ofXCacheObject(object);
            setBucket(key, new_);
            setLocalCache(key, new_);
            return new_;
        }
        return current;
    }

    private void autoRefreshAsync(String key, Callable<E> callable, XCacheObject<E> current)
    {
        // 判断这个缓存已经过期
        if (current.getExpireAt() != null && current.getExpireAt().before(new Date())) {
            autoRefreshExecutor.asyncExec(() -> {
                RBucket<Integer> trySetBucket = getTrySetBucket(key);
                boolean trySet = trySetBucket.trySet(1, 1, TimeUnit.SECONDS);
                if (trySet) {
                    E object = doCall(callable);
                    XCacheObject<E> new_ = ofXCacheObject(object);
                    setBucket(key, new_);
                    setLocalCache(key, new_);
                }
            });
        }
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
        if (config.getObjectConfig() == null || config.getObjectConfig().getTimeout() == null) {
            return XCacheObject.of(e);
        }
        return XCacheObject.of(e, config.getObjectConfig().getTimeout(), config.getObjectConfig().getUnit());
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
