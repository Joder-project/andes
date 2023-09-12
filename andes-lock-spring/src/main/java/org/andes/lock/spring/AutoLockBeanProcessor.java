package org.andes.lock.spring;

import org.andes.lock.core.LockManager;
import org.andes.lock.core.LockProxyFactory;
import org.andes.lock.core.annotations.AutoLock;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AutoLockBeanProcessor implements BeanPostProcessor {

    private final LockManager lockManager;
    private final LockProxyFactory lockProxyFactory;

    public AutoLockBeanProcessor(LockManager lockManager, LockProxyFactory lockProxyFactory) {
        this.lockManager = lockManager;
        this.lockProxyFactory = lockProxyFactory;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (match(bean.getClass())) {
            var object = lockProxyFactory.create(bean);
            ReflectionUtils.doWithMethods(object.getClass(), method -> {
                if (Modifier.isStatic(method.getModifiers())) {
                    throw new IllegalStateException("不能代理static方法");
                }
                if (Modifier.isFinal(method.getModifiers())) {
                    throw new IllegalStateException("不能代理final方法");
                }
                if (Modifier.isPrivate(method.getModifiers())) {
                    throw new IllegalStateException("不能代理private方法");
                }
                var set = Arrays.stream(method.getAnnotation(AutoLock.class).exclude()).collect(Collectors.toUnmodifiableSet());
                var parameters = Stream.of(method.getParameters())
                        .filter(e -> !set.contains(e.getName()))
                        .map(Parameter::getType)
                        .toArray(Class[]::new);
                lockManager.checkCouldDeadLock(method, parameters);
            }, method -> method.isAnnotationPresent(AutoLock.class));
            return object;
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    boolean match(Class<?> type) {
        return type.isAnnotationPresent(LockClass.class);
    }
}
