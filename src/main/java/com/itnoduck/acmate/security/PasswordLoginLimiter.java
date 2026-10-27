package com.itnoduck.acmate.security;
import com.itnoduck.acmate.common.cache.RedisJsonCache;
import com.itnoduck.acmate.common.exception.BusinessException;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Locale;
@Component
public class PasswordLoginLimiter {
 private final StringRedisTemplate redis;
 private static final DefaultRedisScript<Long> SCRIPT=new DefaultRedisScript<>("local a=redis.call('INCR',KEYS[1]); if a==1 then redis.call('EXPIRE',KEYS[1],600) end; local b=redis.call('INCR',KEYS[2]); if b==1 then redis.call('EXPIRE',KEYS[2],600) end; if a>60 or b>10 then return 0 else return 1 end",Long.class);
 public PasswordLoginLimiter(StringRedisTemplate redis){this.redis=redis;}
 public void check(String ip,String username){
  var keys=List.of(RedisJsonCache.keyFor("acmate:password-ip",ip),RedisJsonCache.keyFor("acmate:password-account",username.strip().toLowerCase(Locale.ROOT)));
  try{if(!Long.valueOf(1).equals(redis.execute(SCRIPT,keys)))throw new BusinessException(429,"登录尝试过多，请稍后重试");}
  catch(DataAccessException e){throw new BusinessException(503,"登录保护服务暂不可用，请稍后重试");}
 }
}
