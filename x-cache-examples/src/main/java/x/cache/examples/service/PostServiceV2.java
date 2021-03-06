package x.cache.examples.service;

import com.alibaba.fastjson.JSONObject;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import x.cache.examples.constants.KeyType;
import x.cache.examples.constants.Module;
import x.cache.examples.constants.RedisKeys;
import x.cache.examples.mapper.PostMapper;
import x.cache.examples.model.Post;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class PostServiceV2
{
    public static final String CACHE_POST_MODEL_KEY = RedisKeys.of(KeyType.STR, Module.EXAMPLE, "model:post:");

    // db
    @Autowired
    private PostMapper postMapper;

    // local
    private Cache<Long, Optional<Post>> localCache = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.SECONDS).maximumSize(1000).build();

    // redis
    @Autowired
    private StringRedisTemplate redisTemplate;


    @Transactional(readOnly = true, propagation = Propagation.NOT_SUPPORTED)
    public Post find(Long id)
    {
        try {
            return localCache.get(id, () -> {

                String redisKey = CACHE_POST_MODEL_KEY + id;

                String postJson = redisTemplate.opsForValue().get(redisKey);
                if ("NULL_".equals(postJson)) {
                    log.info("post obtain from redis");
                    return Optional.empty();
                }

                if (StringUtils.isNotBlank(postJson)) {
                    Post parsePost = JSONObject.parseObject(postJson, Post.class);
                    log.info("post obtain from redis");
                    return Optional.of(parsePost);
                }

                // 通过db获取
                Post post = postMapper.selectById(id);
                log.info("post obtain from db");
                if (post != null) {
                    redisTemplate.opsForValue().set(redisKey, JSONObject.toJSONString(post), 1, TimeUnit.DAYS);
                } else {
                    redisTemplate.opsForValue().set(redisKey, "NULL_", 1, TimeUnit.DAYS);
                }

                return Optional.ofNullable(post);

            }).orElse(null);

        } catch (ExecutionException e) {
            throw new RuntimeException("获取Post失败。msg=" + e.getMessage(), e);
        }
    }

}
