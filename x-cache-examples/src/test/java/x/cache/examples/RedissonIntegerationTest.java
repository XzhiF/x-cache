package x.cache.examples;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = XCacheExamplesApplication.class)
public class RedissonIntegerationTest
{

    @Autowired
    private RedissonClient redissonClient;

    @Test
    public void testRedissionCreate() throws Exception
    {
        System.out.println(redissonClient);
        Assertions.assertNotNull(redissonClient);
    }
}
