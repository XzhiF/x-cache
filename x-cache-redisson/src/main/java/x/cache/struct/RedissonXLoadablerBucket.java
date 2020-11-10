package x.cache.struct;

import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import x.cache.exception.XCacheException;
import x.cache.model.XCacheObject;

import java.util.Date;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;

public class RedissonXLoadablerBucket<E> implements XLoadableBucket<E>
{
    private RedissonXBucket<E> xBucket;
    private Function<String, E> keyLoader;
    private BiFunction<String, Integer, E> keyVersionLoader;

    public RedissonXLoadablerBucket(RedissonXBucket<E> xBucket, Function<String, E> keyLoader, BiFunction<String, Integer, E> keyVersionLoader)
    {
        this.xBucket = xBucket;
        this.keyLoader = keyLoader;
        this.keyVersionLoader = keyVersionLoader;
    }

    @Override
    public void put(String key, E e)
    {
        xBucket.put(key, e);
    }

    @Override
    public void put(String key, E e, Integer version)
    {
        xBucket.put(key, e, version);
    }

    @Override
    public E getIfPresent(String key)
    {
        return xBucket.getIfPresent(key);
    }

    @Override
    public E getAutoRefresh(String key, Callable<E> callable)
    {
        return xBucket.getAutoRefresh(key, callable);
    }

    @Override
    public E autoRefreshGet(String key, Callable<E> callable)
    {
        return xBucket.autoRefreshGet(key, callable);
    }

    @Override
    public E getByVersion(String key, Integer version, Callable<E> callable)
    {
        return xBucket.getByVersion(key, version, callable);
    }

    @Override
    public E getAutoRefresh(String key)
    {
        return getAutoRefresh(key, () -> keyLoader.apply(key));
    }

    @Override
    public E autoRefreshGet(String key)
    {
        return autoRefreshGet(key, () -> keyLoader.apply(key));
    }

    @Override
    public E getByVersion(String key, Integer version)
    {
        return getByVersion(key, version, () -> keyVersionLoader.apply(key, version));
    }

}
