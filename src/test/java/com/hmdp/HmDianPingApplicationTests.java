package com.hmdp;

import com.hmdp.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SpringBootTest
class HmDianPingApplicationTests {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Test
    void testRedis() {
        // 写入一条数据到 Redis
        stringRedisTemplate.opsForValue().set("test:key", "hello, redis");

        // 从 Redis 读取数据
        String value = stringRedisTemplate.opsForValue().get("test:key");

        // 打印结果
        System.out.println("value = " + value);
    }

    @Test
    void testIdWorker() throws InterruptedException {
        int threadCount = 300;
        int timesPerThread = 100;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        long start = System.nanoTime();

        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < timesPerThread; j++) {
                        long id = redisIdWorker.nextId("order");
                        // 降低控制台输出量：每个线程仅打印前 3 个
                        if (j < 3) {
                            System.out.println("thread=" + threadIndex + ", seq=" + j + ", id=" + id);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean finished = latch.await(2, TimeUnit.MINUTES);
        long end = System.nanoTime();

        executor.shutdown();

        long costMs = TimeUnit.NANOSECONDS.toMillis(end - start);
        System.out.println("finished=" + finished + ", threads=" + threadCount + ", timesPerThread=" + timesPerThread
                + ", totalRequests=" + (threadCount * timesPerThread) + ", costMs=" + costMs);
    }
    }

