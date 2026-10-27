package com.itnoduck.acmate.practice;
import com.itnoduck.acmate.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class PracticeRateLimitTest {
 @Test void rejectsExcessAndUnavailableRedis(){
  StringRedisTemplate redis=mock(StringRedisTemplate.class);
  when(redis.execute(any(RedisScript.class),anyList(),any(Object[].class))).thenReturn(0L).thenThrow(new DataAccessResourceFailureException("unavailable"));
  var limiter=new PracticeRateLimit(redis);
  assertEquals(429,assertThrows(BusinessException.class,()->limiter.check(1,"run",30)).getCode());
  assertEquals(503,assertThrows(BusinessException.class,()->limiter.check(1,"run",30)).getCode());
 }
}
