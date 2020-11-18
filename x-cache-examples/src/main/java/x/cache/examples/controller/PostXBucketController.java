package x.cache.examples.controller;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import x.cache.examples.model.Post;
import x.cache.examples.service.PostServiceXBucket;

import java.util.UUID;

@RestController
@RequestMapping("/post-xbucket")
public class PostXBucketController
{
    @Autowired
    private PostServiceXBucket postServiceXBucket;


    @RequestMapping("/get")
    public ResponseEntity<Post> getPost(@RequestParam("id") Long id)
    {
        return new ResponseEntity<>(postServiceXBucket.getPost(id), HttpStatus.OK);
    }


    @RequestMapping("/set")
    public ResponseEntity<String> setPost(@RequestParam("id") Long id)
    {
        Post post = postServiceXBucket.getPost(id);
        if(post != null){
            Post copy = new Post();
            BeanUtils.copyProperties(post, copy);
            copy.setTitle(UUID.randomUUID().toString());
            postServiceXBucket.setPost(copy);
        }
        return new ResponseEntity<>("success", HttpStatus.OK);
    }


}
