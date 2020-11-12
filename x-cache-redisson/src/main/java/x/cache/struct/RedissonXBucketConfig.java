package x.cache.struct;

import lombok.Data;
import org.redisson.client.codec.Codec;
import org.redisson.codec.JsonJacksonCodec;
import x.cache.model.UpdateStrategy;

import java.util.concurrent.TimeUnit;

@Data
public class RedissonXBucketConfig
{
    private ObjectCacheConfig objectConfig = new ObjectCacheConfig();
    private LocalCacheConfig localConfig = new LocalCacheConfig();
    private RedisCacheConfig redisConfig = new RedisCacheConfig();
    private TopicConfig topicConfig = new TopicConfig();


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
        private boolean enabled = false;
        private Long timeout = 10L;
        private TimeUnit unit = TimeUnit.SECONDS;
        private Long maximumSize = 10_000L;
    }

    @Data
    public static class RedisCacheConfig
    {
        private Long timeout = 10L;
        private TimeUnit unit = TimeUnit.SECONDS;
    }

    @Data
    public static class TopicConfig
    {
        private boolean enabled = true;
        private boolean autoPublish = true;
        private String name;
        private Codec codec = JsonJacksonCodec.INSTANCE;
    }

}
