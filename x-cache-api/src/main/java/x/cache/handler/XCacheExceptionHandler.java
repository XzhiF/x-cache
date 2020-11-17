package x.cache.handler;

import x.cache.model.XCacheParam;

public interface XCacheExceptionHandler<E>
{
    E handle(XCacheParam<E> param, Throwable throwable);
}
