package x.cache.handler;

import x.cache.model.XCacheParam;

public interface XCacheUpdateHandler<E>
{
    E handle(XCacheParam<E> param);
}