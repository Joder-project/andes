package org.andes.lock.core;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
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
    private final Map<Method, LockChainPrototype> prototypes = new HashMap<>();


    /**
     * 检查是否会导致死锁发生
     *
     * @param method  方法
     * @param classes 需要加锁参数类型
     */
    public void checkCouldDeadLock(Method method, Class<?>... classes) {
        String methodName = method.getDeclaringClass().getName() + "." + method.getName();
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
        prototypes.put(method, lockChainPrototype);
    }

    public LockChain getLockChain(Object... args) {
        for (Object arg : args) {
            Objects.requireNonNull(arg, "加锁对象不能为空");
        }
        return lockPool.newChain(args);
    }
}
