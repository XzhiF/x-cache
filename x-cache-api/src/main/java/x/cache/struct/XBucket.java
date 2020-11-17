package x.cache.struct;

import x.cache.exception.XCacheException;
import x.cache.handler.XCacheExceptionHandler;
import x.cache.handler.XCacheUpdateHandler;
import x.cache.model.XCacheObject;
import x.cache.model.XCacheParam;

import java.util.concurrent.Callable;
import java.util.function.Function;

/**
 * 保存数据结构
 */
public interface XBucket<E>
{
    /**
     * 直接put进去结果
     *
     * @param key
     * @param e
     */
    void put(String key, E e);

    /**
     * 直接put进去结果
     *
     * @param key
     * @param e
     * @param version
     */
    void put(String key, E e, Integer version);


    /**
     * 删除
     *
     * @param key
     */
    void del(String key);


    /**
     * 直接获取
     *
     * @param key
     * @return
     */
    E getIfPresent(String key);

    /**
     * 先返回结果，再去刷新
     *
     * @param key
     * @param callable
     * @return
     */
    E getAutoRefresh(String key, Callable<E> callable);

    /**
     * 先刷新，同时获取结果 ,同步
     *
     * @param key
     * @param callable
     * @return
     */
    E autoRefreshGet(String key, Callable<E> callable);

    /**
     * 通过版本获取
     *
     * @param key
     * @param version
     * @param callable
     * @return
     */
    E getAutoRefresh(String key, Integer version, Callable<E> callable);

    /**
     * 通过版本获取
     *
     * @param key
     * @param version
     * @param callable
     * @return
     */
    E autoRefreshGet(String key, Integer version, Callable<E> callable);


    /**
     * 总的execute方法
     *
     * @param param
     * @param cacheHitFunc
     * @param versionChangeHandler
     * @param exceptionHandler
     * @return
     */
    E execute(XCacheParam<E> param, Function<XCacheObject<E>, E> cacheHitFunc, XCacheUpdateHandler<E> versionChangeHandler, XCacheExceptionHandler<E> exceptionHandler);


    /**
     * 封装call方法
     *
     * @param callable
     * @return
     */
    default E doCall(Callable<E> callable)
    {
        try {
            return callable.call();
        } catch (Exception e) {
            throw new XCacheException(e.getMessage(), e);
        }
    }
}
