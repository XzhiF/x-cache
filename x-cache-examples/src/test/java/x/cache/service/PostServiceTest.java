package x.cache.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import x.cache.examples.XCacheExamplesApplication;
import x.cache.examples.model.Post;
import x.cache.examples.service.PostService;

import java.util.concurrent.TimeUnit;

@SpringBootTest(classes = XCacheExamplesApplication.class)
@Transactional
public class PostServiceTest
{


    @Autowired
    private PostService postService;


    @Test
    public void testFind() throws Exception
    {
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
    }

    @Test
    public void testFind2() throws Exception
    {
        // 第个次的话从db
        Post post = postService.find(1L);
        Assertions.assertNull(post);

        // 在本地缓存获取
        Post post2 = postService.find(1L);
        Post post3 = postService.find(1L);
        Assertions.assertTrue(post2 == post3);

        // 从redis获取
        TimeUnit.SECONDS.sleep(5L);
        Post post4 = postService.find(1L);
        Assertions.assertNotNull(post4);
        Assertions.assertTrue(post4 != post3);
    }


}
