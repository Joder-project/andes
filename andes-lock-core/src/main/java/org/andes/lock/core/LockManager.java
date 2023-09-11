package org.andes.lock.core;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 对象锁的管理
 */
@Slf4j
public class LockManager {

    private final LockPool lockPool = new LockPool();

    /**
     * 所有方法的锁链原型
     */
    private final Map<String, LockChainPrototype> prototypes = new HashMap<>();


    /**
     * 检查是否会导致死锁发生
     *
     * @param methodName 方法名
     * @param classes    需要加锁参数类型
     */
    public void checkCouldDeadLock(String methodName, Class<?>... classes) {
        if (prototypes.containsKey(methodName)) {
            throw new IllegalStateException("不能出现同类名相同方法名" + methodName);
        }
        var lockChainPrototype = new LockChainPrototype(lockPool, methodName, classes);
        var list = prototypes.values().stream().toList();
        for (LockChainPrototype chainPrototype : list) {
            if (lockChainPrototype.isConflict(chainPrototype)) {
                log.error("""
                        锁顺序冲突:
                            {}:
                            {}
                            ------------------
                            {}:
                            {}
                        """, methodName, lockChainPrototype, chainPrototype.methodName, chainPrototype);
                throw new IllegalStateException("锁顺序冲突, " + methodName + ", " + chainPrototype.methodName);
            }
        }
        prototypes.put(methodName, lockChainPrototype);
    }

    public void lock(String methodName, Object... args) {
        var chain = getChain(methodName, args);
        chain.lock();
    }

    public void unlock(String methodName, Object... args) {
        var chain = getChain(methodName, args);
        chain.unlock();
    }

    LockChain getChain(String methodName, Object... args) {
        if (!prototypes.containsKey(methodName)) {
            throw new IllegalStateException("找不到对应注册的锁链, " + methodName);
        }
        for (Object arg : args) {
            Objects.requireNonNull(arg, "加锁对象不能为空");
        }
        var lockChainPrototype = prototypes.get(methodName);
        return lockChainPrototype.newChain(args);
    }
}
