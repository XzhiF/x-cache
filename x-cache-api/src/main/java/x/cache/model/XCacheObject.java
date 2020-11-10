package x.cache.model;

import lombok.*;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 缓存的包装对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class XCacheObject<E>
{
    public static final XCacheObject<Void> VOID = new XCacheObject<>(null, null, null, 1);
    public static final String VOID_VALUE = "_VOID_";

    /**
     * 真正缓存对象
     */
    private E object;

    /**
     * 版本
     */
    private Integer version;

    /**
     * 在何时过期
     */
    private Date expireAt;

    /**
     * 是否是空占位
     */
    private Integer isVoid;


    public static <E> XCacheObject<E> of(E object)
    {
        return new XCacheObject<>(object, null, null, null);
    }

    public static <E> XCacheObject<E> of(E object, Integer version)
    {
        return new XCacheObject<>(object, version, null, null);
    }

    public static <E> XCacheObject<E> of(E object, long timeout, TimeUnit unit)
    {
        Date expireAt = new Date(System.currentTimeMillis() + unit.toMillis(timeout));
        return new XCacheObject<>(object, null, expireAt, null);
    }

    public static <E> XCacheObject<E> of(E object, Integer version, long timeout, TimeUnit unit)
    {
        Date expireAt = new Date(System.currentTimeMillis() + unit.toMillis(timeout));
        return new XCacheObject<>(object, version, expireAt, null);
    }

}
