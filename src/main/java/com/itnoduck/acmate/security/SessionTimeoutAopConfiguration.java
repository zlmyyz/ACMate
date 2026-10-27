package com.itnoduck.acmate.security;

import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;

import java.lang.reflect.Method;

/** Applies the sliding-session interceptor to web controllers through Spring AOP. */
@Configuration(proxyBeanMethods = false)
public class SessionTimeoutAopConfiguration {

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public DefaultPointcutAdvisor sessionTimeoutAdvisor(SlidingSessionTimeoutAdvice advice) {
        StaticMethodMatcherPointcut controllerMethods = new StaticMethodMatcherPointcut() {
            @Override
            public boolean matches(Method method, Class<?> targetClass) {
                String packageName = targetClass.getPackageName();
                return packageName.equals("com.itnoduck.acmate.controller")
                        || packageName.contains(".controller.") || packageName.endsWith(".controller");
            }
        };
        return new DefaultPointcutAdvisor(controllerMethods, advice);
    }
}
