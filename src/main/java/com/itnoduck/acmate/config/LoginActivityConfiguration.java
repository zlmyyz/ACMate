package com.itnoduck.acmate.config;
import com.itnoduck.acmate.user.dto.LoginResponse;
import com.itnoduck.acmate.user.service.LoginActivityService;
import com.itnoduck.acmate.user.service.AlternativeLoginService;
import com.itnoduck.acmate.user.service.impl.UserAuthenticationServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.lang.reflect.Method;
@Configuration(proxyBeanMethods=false)
public class LoginActivityConfiguration {
    @Bean public DefaultPointcutAdvisor loginActivityAdvisor(LoginActivityService activity, com.itnoduck.acmate.security.ClientIpResolver clientIp) {
        var pointcut=new StaticMethodMatcherPointcut() {
            @Override public boolean matches(Method method,Class<?> target) {
                return (method.getName().equals("signIn")||method.getName().equals("emailLogin"))&&AlternativeLoginService.class.isAssignableFrom(target)
                        ||method.getName().equals("login")&&UserAuthenticationServiceImpl.class.isAssignableFrom(target);
            }
        };
        return new DefaultPointcutAdvisor(pointcut,(MethodInterceptor) invocation->{
            Object result=invocation.proceed();
            if(result instanceof LoginResponse user) for(Object argument:invocation.getArguments()) if(argument instanceof HttpServletRequest request) {
                try {activity.record(user.id(),clientIp.resolve(request));}
                catch(RuntimeException ignored) { /* Recording must not make authentication unavailable. */ }
                break;
            }
            return result;
        });
    }
}
