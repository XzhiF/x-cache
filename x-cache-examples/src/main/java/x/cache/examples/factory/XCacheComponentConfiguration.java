package x.cache.examples.factory;

import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.spring.starter.RedissonAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sun.security.x509.OCSPNoCheckExtension;
import x.cache.examples.model.Post;
import x.cache.model.XCacheEvent;
import x.cache.struct.AutoRefreshExecutor;
import x.cache.struct.RedissonXBucket;
import x.cache.struct.RedissonXBucketConfig;
import x.cache.struct.XBucket;

import java.util.concurrent.TimeUnit;

@Configuration
@AutoConfigureAfter(RedissonAutoConfiguration.class)
public class XCacheComponentConfiguration
{

    @Bean
    public AutoRefreshExecutor autoRefreshExecutor()
    {
        return new AutoRefreshExecutor();
    }


    @Bean(name = "postXBucket")
    public XBucket<Post> postXBucket(RedissonClient redissonClient, AutoRefreshExecutor autoRefreshExecutor)
    {
        RedissonXBucketConfig config = new RedissonXBucketConfig();
        config.setObjectConfig(new RedissonXBucketConfig.ObjectCacheConfig(1L, TimeUnit.DAYS, 0));
        config.setLocalConfig(new RedissonXBucketConfig.LocalCacheConfig(true, 1L, TimeUnit.HOURS, 10_000L));
        config.setRedisConfig(new RedissonXBucketConfig.RedisCacheConfig(2L, TimeUnit.DAYS));
        config.setTopicConfig(new RedissonXBucketConfig.TopicConfig(true, true, XCacheEvent.ACTION_DEL,XCacheEvent.LEVEL_LOCAL,"PostXBucketTopic", JsonJacksonCodec.INSTANCE));
        return new RedissonXBucket<>(redissonClient, autoRefreshExecutor, config);
    }

}
