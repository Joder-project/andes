package org.andes.lock.core;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.Lock;

/**
 * 锁链
 */
public class LockChain {

    private final List<Lock> locks;

    LockChain(List<Lock> locks) {
        this.locks = Objects.requireNonNull(locks);
    }

    public void lock() {
        if (locks.isEmpty()) {
            return;
        }
        locks.forEach(Lock::lock);
    }

    public void unlock() {
        if (locks.isEmpty()) {
            return;
        }
        for (int i = locks.size() - 1; i >= 0; i--) {
            locks.get(i).unlock();
        }
    }
}
