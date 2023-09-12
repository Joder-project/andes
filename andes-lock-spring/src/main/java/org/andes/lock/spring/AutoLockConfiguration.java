package org.andes.lock.spring;

import org.andes.lock.core.LockManager;
import org.andes.lock.core.LockProxyFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AutoLockConfiguration {

    @Bean
    LockManager lockManager() {
        return new LockManager();
    }

    @Bean
    LockProxyFactory lockProxyFactory(LockManager lockManager) {
        return new LockProxyFactory(lockManager);
    }

    @Bean
    AutoLockBeanProcessor autoLockBeanProcessor(LockManager lockManager, LockProxyFactory lockProxyFactory) {
        return new AutoLockBeanProcessor(lockManager, lockProxyFactory);
    }

}
