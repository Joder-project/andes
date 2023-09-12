package org.andes.lock.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.UndeclaredThrowableException;

/**
 * 生成代理对象
 */
public class LockProxyFactory {

    private final LockManager lockManager;

    public LockProxyFactory(LockManager lockManager) {
        this.lockManager = lockManager;
    }

    /**
     * 获取增强对象
     */
    @SuppressWarnings("unchecked")
    public <T> T create(T obj) {
        try {
            var proxyInfo = new ProxyInfo(obj.getClass());
            return (T) proxyInfo.newObject(lockManager, obj);
        } catch (Exception ex) {
            return obj;
        }
    }

    /**
     * 类型对应信息
     */
    static class ProxyInfo {

        final Class<?> rawClass;
        final Class<?> proxyClass;
        final Constructor<?> constructor;

        ProxyInfo(Class<?> clazz) {
            this.proxyClass = buildProxy(clazz);
            this.rawClass = clazz;
            try {
                this.constructor = this.proxyClass.getDeclaredConstructor(clazz, LockManager.class);
            } catch (NoSuchMethodException ex) {
                throw new UndeclaredThrowableException(ex);
            }
        }

        @SuppressWarnings("unchecked")
        <T> Class<T> buildProxy(Class<T> clazz) {
            try {
                return (Class<T>) Class.forName(clazz.getName() + "$Proxy");
            } catch (Exception ex) {
                throw new IllegalStateException("buildProxy error", ex);
            }
        }


        Object newObject(LockManager manager, Object old) {
            if (old == null) {
                throw new IllegalStateException("增强对象不能为空" + proxyClass.getName());
            }
            try {
                return constructor.newInstance(old, manager);
            } catch (Exception ex) {
                throw new IllegalStateException("创建增强对象失败", ex);
            }
        }
    }
}
