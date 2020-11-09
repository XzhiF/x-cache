package x.cache.struct;

import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import x.cache.exception.XCacheException;
import x.cache.model.XCacheObject;

import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class RedissonXBucket<E> implements XBucket<E>
{
    private RedissonClient redisson;
    private AutoRefreshExecutor autoRefreshExecutor;
    private RedissonXBucketConfig config;

    public RedissonXBucket(RedissonClient redisson, AutoRefreshExecutor autoRefreshExecutor, RedissonXBucketConfig config)
    {
        this.redisson = redisson;
        this.autoRefreshExecutor = autoRefreshExecutor;
        this.config = config;
    }

    @Override
    public void put(String key, E e)
    {
        XCacheObject<E> xCacheObject = ofXCacheObject(e);
        setBucket(key, xCacheObject);
    }

    @Override
    public E getAutoRefresh(String key, Callable<E> callable)
    {
        XCacheObject<E> xCacheObject = getBucket(key).get();
        if (xCacheObject == null) {
            return null;
        }
        autoRefreshAsync(key, callable, xCacheObject);
        return xCacheObject.getObject();
    }

    @Override
    public E autoRefreshGet(String key, Callable<E> callable)
    {
        XCacheObject<E> xCacheObject = getBucket(key).get();
        if (xCacheObject == null) {
            return null;
        }
        return autoRefresh(key, callable, xCacheObject).getObject();
    }


    private XCacheObject<E> autoRefresh(String key, Callable<E> callable, XCacheObject<E> current)
    {
        // 判断这个缓存已经过期
        if (current.getExpireAt() != null && current.getExpireAt().before(new Date())) {
            E object = doCall(callable);
            XCacheObject<E> new_ = ofXCacheObject(object);
            setBucket(key, new_);
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
                }
            });
        }
    }

    private E doCall(Callable<E> callable)
    {
        try {
            return callable.call();
        } catch (Exception e) {
            throw new XCacheException(e.getMessage(), e);
        }
    }

    private void setBucket(String key, XCacheObject<E> e)
    {
        getBucket(key).set(e, config.getRedisConfig().getTimeout(), config.getRedisConfig().getUnit());
    }

    private XCacheObject<E> ofXCacheObject(E e)
    {
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
