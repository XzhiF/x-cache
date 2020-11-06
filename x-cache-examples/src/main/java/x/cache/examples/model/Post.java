package x.cache.examples.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

@Data
@EqualsAndHashCode(of = "id")
@TableName(value = "tb_post")
public class Post implements Serializable
{
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String author;

    private String content;

    @TableField(value = "read_count")
    private Integer readCount;

    @TableField(value = "up_count")
    private Integer upCount;

    @TableField(value = "store_count")
    private Integer storeCount;

    @TableField(value = "create_time")
    private Date createTime;

    @TableField(value = "update_time")
    private Date updateTime;

}
