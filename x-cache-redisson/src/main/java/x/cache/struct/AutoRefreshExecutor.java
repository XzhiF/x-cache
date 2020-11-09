package x.cache.struct;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

import static java.util.concurrent.ThreadPoolExecutor.*;

@Slf4j
public class AutoRefreshExecutor
{
    private ExecutorService executorService;

    public AutoRefreshExecutor()
    {
        ThreadFactory threadFactory = r -> new Thread(r, "AutoRefreshExecutor-Thread");
        RejectedExecutionHandler rejectedHandler = new CallerRunsPolicy()
        {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor e)
            {
                log.warn("[metric-performance] 后台刷新现成超过maximumPoolSize={}，请检查程序", 100);
                super.rejectedExecution(r, e);
            }
        };
        this.executorService = new ThreadPoolExecutor(
                8, 100, 3, TimeUnit.MINUTES,
                new LinkedBlockingQueue<>(100), threadFactory, rejectedHandler
        );
    }


    public void asyncExec(Runnable runnable)
    {
        executorService.execute(runnable);
    }


    void close()
    {
        executorService.shutdown();
        try {
            boolean terminated = executorService.awaitTermination(10, TimeUnit.SECONDS);
            if (!terminated) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }


}
