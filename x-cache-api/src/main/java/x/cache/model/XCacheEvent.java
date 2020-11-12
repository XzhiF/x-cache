package x.cache.model;

import lombok.*;

import java.io.Serializable;

/**
 * 用通知缓存更新人时间
 */
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"key", "seq"})
@Builder
@ToString
@Data
public class XCacheEvent implements Serializable
{
    public static final int ACTION_DEL = 0;
    public static final int ACTION_SAVE = 1;

    public static final int LEVEL_LOCAL = 0;
    public static final int LEVEL_ALL = 1;


    /**
     * 缓存的key
     */
    private String key;

    /**
     * seq用于做幂等， or在并发时，做排他
     */
    private long seq;

    /**
     * 动作：
     * add,update => save
     * remove
     */
    private int action;


    /**
     * 更新缓存的级别:
     * 0, 本地缓存
     * 1, 更新所有缓存
     */
    private int level;

    /**
     * 额外的参数对象
     */
    private XCacheObject<?> xCacheObject;

}
