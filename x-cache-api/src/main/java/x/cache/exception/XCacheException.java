package x.cache.exception;

public class XCacheException extends RuntimeException
{
    public XCacheException()
    {
    }

    public XCacheException(String message)
    {
        super(message);
    }

    public XCacheException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public XCacheException(Throwable cause)
    {
        super(cause);
    }

    public XCacheException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
