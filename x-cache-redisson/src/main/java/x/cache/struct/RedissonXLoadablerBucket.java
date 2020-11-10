package x.cache.struct;

import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.function.Function;

public class RedissonXLoadablerBucket<E> implements XLoadableBucket<E>
{
    private RedissonXBucket<E> xBucket;
    private Function<String, E> loader;

    public RedissonXLoadablerBucket(RedissonXBucket<E> xBucket, Function<String, E> loader)
    {
        this.xBucket = xBucket;
        this.loader = loader;
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
    public E getAutoRefresh(String key, Integer version, Callable<E> callable)
    {
        return xBucket.getAutoRefresh(key, version, callable);
    }

    @Override
    public E autoRefreshGet(String key, Integer version, Callable<E> callable)
    {
        return xBucket.autoRefreshGet(key, version, callable);
    }

    // enhancement


    @Override
    public E getAutoRefresh(String key)
    {
        return getAutoRefresh(key, () -> loader.apply(key));
    }

    @Override
    public E autoRefreshGet(String key)
    {
        return autoRefreshGet(key, () -> loader.apply(key));
    }

    @Override
    public E getAutoRefresh(String key, Integer version)
    {
        return getAutoRefresh(key, version, () -> loader.apply(key));
    }

    @Override
    public E autoRefreshGet(String key, Integer version)
    {
        return autoRefreshGet(key, version, () -> loader.apply(key));
    }

}
