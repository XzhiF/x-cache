package x.cache.struct;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.redisson.client.codec.Codec;
import org.redisson.codec.JsonJacksonCodec;
import x.cache.handler.ExceptionStrategy;
import x.cache.handler.UpdateStrategy;
import x.cache.model.XCacheEvent;

import java.util.concurrent.TimeUnit;

@Data
public class RedissonXBucketConfig
{
    private ObjectCacheConfig objectConfig = new ObjectCacheConfig();
    private LocalCacheConfig localConfig = new LocalCacheConfig();
    private RedisCacheConfig redisConfig = new RedisCacheConfig();
    private TopicConfig topicConfig = new TopicConfig();
    private StrategyConfig strategyConfig = new StrategyConfig();


    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class ObjectCacheConfig
    {
        private Long timeout;
        private TimeUnit unit;
        private Integer version;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class LocalCacheConfig
    {
        private boolean enabled = false;
        private Long timeout = 10L;
        private TimeUnit unit = TimeUnit.SECONDS;
        private Long maximumSize = 10_000L;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class RedisCacheConfig
    {
        private Long timeout = 10L;
        private TimeUnit unit = TimeUnit.SECONDS;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class TopicConfig
    {
        private boolean enabled = true;
        private boolean autoPublish = true;
        // 默认删除集群中的本地缓存
        private int autoPublishAction = XCacheEvent.ACTION_DEL;
        private int autoPublishLevel = XCacheEvent.LEVEL_LOCAL;
        private String name;
        private Codec codec = JsonJacksonCodec.INSTANCE;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class StrategyConfig
    {
        private UpdateStrategy versionUpdateStrategy;
        private UpdateStrategy expireUpdateStrategy;
        private ExceptionStrategy exceptionStrategy;
    }

}
