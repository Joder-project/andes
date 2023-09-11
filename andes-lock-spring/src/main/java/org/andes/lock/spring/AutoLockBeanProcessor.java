package org.andes.lock.spring;

import org.andes.lock.core.AutoLock;
import org.andes.lock.core.LockManager;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AutoLockBeanProcessor implements BeanPostProcessor {

    private final LockManager lockManager;

    public AutoLockBeanProcessor(LockManager lockManager) {
        this.lockManager = lockManager;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        ReflectionUtils.doWithMethods(bean.getClass(), method -> {
            var set = Arrays.stream(method.getAnnotation(AutoLock.class).exclude()).collect(Collectors.toUnmodifiableSet());
            var parameters = Stream.of(method.getParameters())
                    .filter(e -> !set.contains(e.getName()))
                    .map(Parameter::getType)
                    .toArray(Class[]::new);
            lockManager.checkCouldDeadLock(bean.getClass() + "." + method.getName(), parameters);
        }, method -> method.isAnnotationPresent(AutoLock.class));
        return bean;
    }
}
