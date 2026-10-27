package com.itnoduck.acmate.security;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.junit.jupiter.api.Assertions.*;
class ClientIpResolverTest {
 @Test void ignoresHeadersFromUntrustedPeersAndWalksTrustedChain(){
  var request=new MockHttpServletRequest();request.setRemoteAddr("10.0.0.2");request.addHeader("X-Forwarded-For","198.51.100.77, 203.0.113.9, 10.0.0.1");
  assertEquals("10.0.0.2",new ClientIpResolver("").resolve(request));
  assertEquals("203.0.113.9",new ClientIpResolver("10.0.0.1,10.0.0.2").resolve(request));
 }
 @Test void rejectsMalformedForwardingChain(){var request=new MockHttpServletRequest();request.setRemoteAddr("10.0.0.1");request.addHeader("X-Forwarded-For","invalid.example");assertEquals("10.0.0.1",new ClientIpResolver("10.0.0.1").resolve(request));}
}
