package org.andes.lock.spring;

import org.andes.lock.core.LockManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AutoLockConfiguration {

    @Bean
    LockManager lockManager() {
        return new LockManager();
    }

    @Bean
    AutoLockBeanProcessor autoLockBeanProcessor(LockManager lockManager) {
        return new AutoLockBeanProcessor(lockManager);
    }
}
