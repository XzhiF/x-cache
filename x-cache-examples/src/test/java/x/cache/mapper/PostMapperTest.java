package x.cache.mapper;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import x.cache.examples.XCacheExamplesApplication;
import x.cache.examples.model.Post;
import x.cache.examples.mapper.PostMapper;

import java.util.Date;

@SpringBootTest(classes = XCacheExamplesApplication.class)
public class PostMapperTest
{

    @Autowired
    private PostMapper postMapper;

    @Test
    public void testAdd() throws Exception
    {
        Post post = new Post();
        post.setTitle("测试一个缓存使用的文章");
        post.setAuthor("xzf");
        post.setContent("这是一个很大人文章");
        post.setReadCount(0);
        post.setUpCount(0);
        post.setStoreCount(0);
        post.setCreateTime(new Date());
        post.setUpdateTime(new Date());

        postMapper.insert(post);

        System.out.println(post.getId());
        Assertions.assertNotNull(post.getId());


    }

}
