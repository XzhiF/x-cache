package x.cache.examples.service;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
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
import x.cache.model.XCacheObject;

import java.util.Date;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class PostServiceXCacheObject
{
    public static final String CACHE_POST_MODEL_KEY = RedisKeys.of(KeyType.STR, Module.EXAMPLE, "model:post:");

    // db
    @Autowired
    private PostMapper postMapper;

    // local
    private Cache<Long, XCacheObject<Post>> localCache = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.SECONDS).maximumSize(1000).build();

    // redis
    @Autowired
    private StringRedisTemplate redisTemplate;


    public Post find(Long id)
    {
        try {
            return localCache.get(id, () -> {

                String redisKey = CACHE_POST_MODEL_KEY + id;

                String postJson = redisTemplate.opsForValue().get(redisKey);

                if (StringUtils.isNotBlank(postJson)) {
                    XCacheObject<Post> postXCacheObject = JSONObject.parseObject(postJson, new TypeReference<XCacheObject<Post>>() {});
                    log.info("post obtain from redis");
                    return postXCacheObject;
                }

                // 通过db获取
                Post post = postMapper.selectById(id);
                log.info("post obtain from db");
                if (post != null) {
                    redisTemplate.opsForValue().set(redisKey, JSONObject.toJSONString(XCacheObject.of(post)), 1, TimeUnit.DAYS);
                } else {
                    redisTemplate.opsForValue().set(redisKey, JSONObject.toJSONString(XCacheObject.VOID), 1, TimeUnit.DAYS);
                }

                return XCacheObject.of(post);

            }).getObject();

        } catch (ExecutionException e) {
            throw new RuntimeException("获取Post失败。msg=" + e.getMessage(), e);
        }
    }


    /**
     * 两种策略
     * 1.需要从db获取最新
     * 2.异步刷缓存，下一次获取最新
     * @param id
     * @param expireAtMillis
     * @param redisTimoutMillis
     * @return
     */
    public Post findExpireAfter(Long id, long expireAtMillis, long redisTimoutMillis)
    {
        // 通过本地缓存获取
        XCacheObject<Post> localCachePost = localCache.getIfPresent(id);
        if (localCachePost != null) {
            log.info("post obtain from local cache");
            refreshIfExpire(localCachePost, id, expireAtMillis,redisTimoutMillis);
            return localCachePost.getObject();
        }

        // 通过redis获取
        String redisKey = CACHE_POST_MODEL_KEY + id;
        String postJson = redisTemplate.opsForValue().get(redisKey);
        if (StringUtils.isNotBlank(postJson)) {
            XCacheObject<Post> redisCachePost = JSONObject.parseObject(postJson, new TypeReference<XCacheObject<Post>>(){});
            localCache.put(id, redisCachePost);
            log.info("post obtain from redis");
            refreshIfExpire(redisCachePost, id, expireAtMillis,redisTimoutMillis);
            return redisCachePost.getObject();
        }

        // 通过db获取
        Post post = postMapper.selectById(id);
        XCacheObject<Post> dbCachePost = XCacheObject.of(post, expireAtMillis, TimeUnit.MILLISECONDS);
        redisTemplate.opsForValue().set(redisKey, JSONObject.toJSONString(dbCachePost), redisTimoutMillis, TimeUnit.MILLISECONDS);
        localCache.put(id, dbCachePost);
        log.info("post obtain from db");

        return post;
    }

    private void refreshIfExpire(XCacheObject<Post> xCacheObject, Long id, long expireAtMillis,long redisTimoutMillis)
    {
        // 判断这个缓存已经过期
        if(xCacheObject.getExpireAt() != null && xCacheObject.getExpireAt().before(new Date())) {
            String redisKey = CACHE_POST_MODEL_KEY + id;
            Post post = postMapper.selectById(id);
            XCacheObject<Post> newXCacheObject = XCacheObject.of(post, expireAtMillis, TimeUnit.MILLISECONDS);
            redisTemplate.opsForValue().set(redisKey, JSONObject.toJSONString(newXCacheObject), redisTimoutMillis, TimeUnit.MILLISECONDS);
            localCache.put(id, newXCacheObject);
            log.info("refresh: post obtain from db");
        }
    }


}
