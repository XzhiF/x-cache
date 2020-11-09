package x.cache.struct;

import lombok.Data;

import java.util.concurrent.TimeUnit;

@Data
public class RedissonXBucketConfig
{
    private ObjectCacheConfig objectConfig = new ObjectCacheConfig();
    private LocalCacheConfig localConfig = new LocalCacheConfig();
    private RedisCacheConfig redisConfig = new RedisCacheConfig();

    @Data
    public static class ObjectCacheConfig
    {
        private Long timeout;
        private TimeUnit unit;
        private Integer version;
    }

    @Data
    public static class LocalCacheConfig
    {
        private Long timeout = 10L;
        private TimeUnit unit = TimeUnit.SECONDS;
    }

    @Data
    public static class RedisCacheConfig
    {
        private Long timeout = 10L;
        private TimeUnit unit = TimeUnit.SECONDS;
    }

}
