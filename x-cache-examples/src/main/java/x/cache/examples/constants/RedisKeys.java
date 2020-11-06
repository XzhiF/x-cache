package x.cache.examples.constants;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class RedisKeys
{
    public static final String SEPARATOR_CHAR = ":";

    // 用户缓存获取系统有多少个key前缀
    private static final Map<String, AtomicLong> KEYS_STAT = new ConcurrentHashMap<>();


    public static final String of(KeyType type, Module module, String prefixOrVal)
    {
        String value = type.name().toLowerCase()
                + SEPARATOR_CHAR
                + module.name().toLowerCase()
                + SEPARATOR_CHAR
                + prefixOrVal;

        //增加调用次数
        KEYS_STAT.compute(value, (v, i) -> i == null ? new AtomicLong(0) : i).incrementAndGet();

        return value;
    }

    public static final Map<String, AtomicLong> getKeysStat()
    {
        return new HashMap<>(KEYS_STAT);
    }

}
