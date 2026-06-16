package com.programming.techie.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
@Service
public class RedisLockService {

    private static final String LOCK_PREFIX = "lock:wallet:";

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisLockService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String acquireLock(Long walletId) {

        String key = LOCK_PREFIX + walletId;
        String lockValue = UUID.randomUUID().toString();

        Boolean success = redisTemplate.opsForValue().setIfAbsent(
                key,
                lockValue,
                10,
                TimeUnit.SECONDS
        );

        System.out.println("TRY LOCK: " + key);
        System.out.println("LOCK RESULT: " + success);

        if (Boolean.TRUE.equals(success)) {
            return lockValue;
        }

        return null;
    }

    public void releaseLock(Long walletId, String lockValue) {

        String key = LOCK_PREFIX + walletId;

        String luaScript =
                "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                        "return redis.call('del', KEYS[1]) " +
                        "else return 0 end";

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(luaScript);
        script.setResultType(Long.class);

        Long result = redisTemplate.execute(
                script,
                Collections.singletonList(key),
                lockValue
        );

        if (result != null && result == 1) {
            System.out.println("LOCK RELEASED: " + key);
        }
    }
}
