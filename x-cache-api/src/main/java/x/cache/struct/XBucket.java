package x.cache.struct;

import x.cache.model.XCacheObject;

import java.util.concurrent.Callable;

/**
 * 保存数据结构
 */
public interface XBucket<E>
{
    /**
     * 直接put进去结果
     * @param key
     * @param e
     */
    void put(String key, E e);

    /**
     * 先返回结果，再去刷新
     * @param key
     * @param callable
     * @return
     */
    E getAutoRefresh(String key, Callable<E> callable);

    /**
     * 先刷新，同时获取结果 ,同步
     * @param key
     * @param callable
     * @return
     */
    E autoRefreshGet(String key,Callable<E> callable);
}
