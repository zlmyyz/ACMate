package com.itnoduck.acmate.security;
import com.itnoduck.acmate.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class PasswordLoginLimiterTest {
 @Test void rejectsExcessAndFailsClosedWhenRedisUnavailable(){
  var redis=mock(StringRedisTemplate.class);when(redis.execute(any(RedisScript.class),anyList(),any(Object[].class))).thenReturn(0L).thenThrow(new DataAccessResourceFailureException("offline"));
  var limiter=new PasswordLoginLimiter(redis);
  assertEquals(429,assertThrows(BusinessException.class,()->limiter.check("127.0.0.1","alice")).getCode());
  assertEquals(503,assertThrows(BusinessException.class,()->limiter.check("127.0.0.1","alice")).getCode());
 }
}
