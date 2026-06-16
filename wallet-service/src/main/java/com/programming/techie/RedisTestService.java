package com.programming.techie;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisTestService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void test() {

        redisTemplate.opsForValue().set("test:key","hello redis");

        System.out.println(redisTemplate.opsForValue().get("test:key"));
    }
}