package com.programming.techie.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@Service
public class RedisBalanceService {

    private static final String KEY_PREFIX = "wallet:balance:";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public BigDecimal getBalance(Long walletId) {

        String key = KEY_PREFIX + walletId;

        Object value = redisTemplate.opsForValue().get(key);

        System.out.println("REDIS GET → " + key + " = " + value);

        if (value == null) {
            return null;
        }

        return new BigDecimal(value.toString());
    }

    public void setBalance(Long walletId, BigDecimal balance) {

        String key = KEY_PREFIX + walletId;

        System.out.println("REDIS SET → " + key + " = " + balance);

        redisTemplate.opsForValue().set(
                key,
                balance,
                10,
                TimeUnit.MINUTES
        );
    }
    public void evictBalance(Long walletId) {

        String key = KEY_PREFIX + walletId;

        redisTemplate.delete(key);
    }
}
