package x.cache.struct;

public interface XLoadableBucket<E> extends XBucket<E>
{

    /**
     * 先返回结果，再去刷新
     *
     * @param key
     * @return
     */
    E getAutoRefresh(String key);

    /**
     * 先刷新，同时获取结果 ,同步
     *
     * @param key
     * @return
     */
    E autoRefreshGet(String key);

    /**
     * 通过版本获取
     *
     * @param key
     * @param version
     * @return
     */
    E getAutoRefresh(String key, Integer version);


    /**
     * 先刷新，同时获取结果 ,同步
     * @param key
     * @param version
     * @return
     */
    E autoRefreshGet(String key, Integer version);
}
