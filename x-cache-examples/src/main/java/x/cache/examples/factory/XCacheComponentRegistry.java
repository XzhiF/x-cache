package x.cache.examples.factory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import x.cache.examples.model.Post;
import x.cache.struct.XBucket;

@Component
public class XCacheComponentRegistry
{
    @Qualifier("postXBucket")
    @Autowired
    private XBucket<Post> postXBucket;

    public XBucket<Post> getPostBucket()
    {
        return postXBucket;
    }


}
