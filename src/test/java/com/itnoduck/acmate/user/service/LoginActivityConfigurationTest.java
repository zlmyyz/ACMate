package com.itnoduck.acmate.user.service;
import com.itnoduck.acmate.config.LoginActivityConfiguration;
import com.itnoduck.acmate.user.dto.LoginResponse;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.mockito.Mockito.*;
class LoginActivityConfigurationTest {
    @Test void emailLoginIsRecordedEvenWhenItsInternalSignInCallDoesNotCrossTheProxy() {
        var target=mock(AlternativeLoginService.class);var activity=mock(LoginActivityService.class);
        var factory=new ProxyFactory(target);factory.setProxyTargetClass(true);factory.addAdvisor(new LoginActivityConfiguration().loginActivityAdvisor(activity,new com.itnoduck.acmate.security.ClientIpResolver("")));
        var service=(AlternativeLoginService)factory.getProxy();var request=new MockHttpServletRequest();request.setRemoteAddr("192.168.1.10");var response=new MockHttpServletResponse();
        when(target.emailLogin("mail@example.com","123456",request,response)).thenReturn(new LoginResponse(1L,"member","Member","mail@example.com",null,null,false));
        service.emailLogin("mail@example.com","123456",request,response);
        verify(activity).record(1,"192.168.1.10");
    }
}
