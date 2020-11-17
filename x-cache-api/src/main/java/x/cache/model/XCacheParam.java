package x.cache.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.concurrent.Callable;

@ToString
@AllArgsConstructor
@NoArgsConstructor
@Data
public class XCacheParam<E>
{
    private String key;
    private Integer version;
    private Callable<E> callable;
}
