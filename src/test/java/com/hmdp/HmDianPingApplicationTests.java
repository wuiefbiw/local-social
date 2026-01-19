package com.hmdp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
class HmDianPingApplicationTests {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void testRedis() {
        // 写入一条数据到 Redis
        stringRedisTemplate.opsForValue().set("test:key", "hello, redis");

        // 从 Redis 读取数据
        String value = stringRedisTemplate.opsForValue().get("test:key");

        // 打印结果
        System.out.println("value = " + value);
    }

}
