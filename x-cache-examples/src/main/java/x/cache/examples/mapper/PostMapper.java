package x.cache.examples.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import x.cache.examples.model.Post;

@Mapper
public interface PostMapper extends BaseMapper<Post>
{
}
