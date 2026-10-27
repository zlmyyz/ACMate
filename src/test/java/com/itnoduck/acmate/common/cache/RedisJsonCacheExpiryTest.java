package com.itnoduck.acmate.common.cache;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class RedisJsonCacheExpiryTest {
 @Test void hitsDoNotRenewTtlAndMissesReloadDatabase(){
  var redis=mock(StringRedisTemplate.class);@SuppressWarnings("unchecked") ValueOperations<String,String> values=mock(ValueOperations.class);
  when(redis.opsForValue()).thenReturn(values);when(values.get("key")).thenReturn("\"old\"",null);
  var cache=new RedisJsonCache(redis,new ObjectMapper());var loads=new AtomicInteger();var type=new TypeReference<String>(){};
  assertEquals("old",cache.getOrLoad("key",type,Duration.ofSeconds(10),()->{loads.incrementAndGet();return "new";}));
  assertEquals("new",cache.getOrLoad("key",type,Duration.ofSeconds(10),()->{loads.incrementAndGet();return "new";}));
  assertEquals(1,loads.get());verify(redis,never()).expire(anyString(),any(Duration.class));verify(values).set("key","\"new\"",Duration.ofSeconds(10));
 }
}
