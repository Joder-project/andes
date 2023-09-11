package org.andes.lock.core;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.Lock;

/**
 * 锁链
 */
class LockChain {

    private final List<Lock> locks;

    LockChain(List<Lock> locks) {
        this.locks = Objects.requireNonNull(locks);
    }

    void lock() {
        if (locks.isEmpty()) {
            return;
        }
        locks.forEach(Lock::lock);
    }

    void unlock() {
        if (locks.isEmpty()) {
            return;
        }
        locks.forEach(Lock::unlock);
    }
}
