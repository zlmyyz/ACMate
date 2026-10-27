package com.itnoduck.acmate.practice;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.junit.jupiter.api.Assertions.*;
class PracticeAccessTest {
 @Test void developmentOnlyAcceptsDirectLocalRequests(){
  var access=new PracticeAccess("local");var req=new MockHttpServletRequest();req.setRemoteAddr("127.0.0.1");req.setServerName("localhost");assertTrue(access.allowed(req));
  assertFalse(new PracticeAccess("disabled").allowed(req));assertFalse(new PracticeAccess("unknown").allowed(req));
  req.addHeader("X-Forwarded-For","203.0.113.2");assertFalse(access.allowed(req));
  var remote=new MockHttpServletRequest();remote.setRemoteAddr("192.168.1.2");remote.setServerName("localhost");assertFalse(access.allowed(remote));
  assertTrue(new PracticeAccess("isolated").allowed(remote));
 }
}
