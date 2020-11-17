package x.cache.handler;

public enum ExceptionStrategy
{
    /**
     * 抛出异常
     */
    ABORT,
    /**
     * 删除缓存
     */
    REMOVE_ABORT,
    /**
     * 返回NULL
     */
    RETURN_NULL,
    /**
     * 从Callable中获取
     */
    LOAD,
    /**
     *  读取旧的值
     */
    READ_OLD
}
