package x.cache.service;

import com.alibaba.fastjson.JSON;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import x.cache.examples.XCacheExamplesApplication;
import x.cache.examples.constants.RedisKeys;
import x.cache.examples.model.Post;
import x.cache.examples.service.PostService;
import x.cache.examples.service.PostServiceXCacheObject;

import java.util.concurrent.TimeUnit;

@SpringBootTest(classes = XCacheExamplesApplication.class)
public class PostServiceXCacheObjectTest
{


    @Autowired
    private PostServiceXCacheObject postService;


    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    public void testFind() throws Exception
    {
        redisTemplate.delete("str:example:model:post:2");

        // 第个次的话从db
        Post post = postService.find(2L);
        Assertions.assertNotNull(post);

        // 在本地缓存获取
        Post post2 = postService.find(2L);
        Post post3 = postService.find(2L);
        Assertions.assertTrue(post2 == post3);

        // 从redis获取
        TimeUnit.SECONDS.sleep(5L);
        Post post4 = postService.find(2L);
        Assertions.assertNotNull(post4);
        Assertions.assertTrue(post4 != post3);

        System.out.println(JSON.toJSONString(RedisKeys.getKeysStat()));
    }


    @Test
    public void testFindExpireAfter() throws Exception
    {
        redisTemplate.delete("str:example:model:post:2");

        // 第个次的话从db
        Post post = postService.findExpireAfter(2L, 500L, TimeUnit.DAYS.toMillis(1));
        Assertions.assertNotNull(post);

        // 在本地缓存获取
        Post post2 = postService.findExpireAfter(2L, 500L, TimeUnit.DAYS.toMillis(1));
        Post post3 = postService.findExpireAfter(2L, 500L, TimeUnit.DAYS.toMillis(1));
        Assertions.assertTrue(post2 == post3);

        // 从redis获取
        TimeUnit.SECONDS.sleep(1L);
        Post post4 = postService.findExpireAfter(2L, 500L, TimeUnit.DAYS.toMillis(1));
        Assertions.assertNotNull(post4);
        Assertions.assertTrue(post4 == post3);


        Post post5 = postService.findExpireAfter(2L, 500L, TimeUnit.DAYS.toMillis(1));
        Assertions.assertTrue(post5 != post4);

        System.out.println(JSON.toJSONString(RedisKeys.getKeysStat()));
    }
}
