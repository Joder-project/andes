package org.andes.lock.core;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.time.Duration;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 锁池
 */
class LockPool {

    private final Cache<Object, Lock> cache;

    LockPool() {
        this.cache = CacheBuilder.newBuilder().maximumSize(Integer.MAX_VALUE)
                .expireAfterAccess(Duration.ofSeconds(60))
                .build();
    }

    Lock valueOf(Object lockObj) {
        try {
            return cache.get(lockObj, ReentrantLock::new);
        } catch (Exception ex) {
            throw new AndesException(ex);
        }
    }

}
