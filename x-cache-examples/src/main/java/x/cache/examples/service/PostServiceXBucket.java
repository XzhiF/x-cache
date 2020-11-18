package x.cache.examples.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.data.convert.EntityWriter;
import org.springframework.stereotype.Service;
import x.cache.examples.factory.XCacheComponentRegistry;
import x.cache.examples.mapper.PostMapper;
import x.cache.examples.model.Post;
import x.cache.struct.XBucket;

import java.util.List;

@Slf4j
@Service
public class PostServiceXBucket implements CommandLineRunner
{
    @Autowired
    private XCacheComponentRegistry cacheRegistry;

    @Autowired
    private PostMapper postMapper;

    @Override
    public void run(String... args) throws Exception
    {
        List<Post> posts = postMapper.selectList(new QueryWrapper<>());
        for (Post post : posts) {
            getPostBucket().put(getCacheKye(post.getId()), post);
        }
    }

    public void setPost(Post post)
    {
        getPostBucket().put(getCacheKye(post.getId()), post);
    }


    public Post getPost(Long id)
    {
        return getPostBucket().getIfPresent(getCacheKye(id));
    }

    private String getCacheKye(Long id)
    {
        return "xbucket:model:post:" + id;
    }

    private XBucket<Post> getPostBucket()
    {
        return cacheRegistry.getPostBucket();
    }


}
