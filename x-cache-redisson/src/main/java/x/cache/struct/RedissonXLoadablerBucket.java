package x.cache.struct;

import org.redisson.api.RedissonClient;

import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.function.Function;

public class RedissonXLoadablerBucket<E> extends RedissonXBucket<E> implements XLoadableBucket<E>
{
    private Function<String, E> loader;

    public RedissonXLoadablerBucket(RedissonClient redisson, AutoRefreshExecutor autoRefreshExecutor, RedissonXBucketConfig config, Function<String, E> loader)
    {
        super(redisson, autoRefreshExecutor, config);
        this.loader = loader;
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
