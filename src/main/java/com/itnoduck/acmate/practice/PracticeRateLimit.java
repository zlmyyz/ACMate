package com.itnoduck.acmate.practice;
import com.itnoduck.acmate.common.exception.BusinessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import java.util.List;
@Component
public class PracticeRateLimit {
    private final StringRedisTemplate redis;
    private static final DefaultRedisScript<Long> SCRIPT=new DefaultRedisScript<>("local n=redis.call('INCR',KEYS[1]); if n==1 then redis.call('EXPIRE',KEYS[1],ARGV[1]) end; if n<=tonumber(ARGV[2]) then return 1 else return 0 end",Long.class);
    public PracticeRateLimit(StringRedisTemplate redis){this.redis=redis;}
    public void check(long userId,String action,int maximum){
        try {if(!Long.valueOf(1).equals(redis.execute(SCRIPT,List.of("acmate:practice:rate:"+action+":"+userId),"60",Integer.toString(maximum))))throw new BusinessException(429,"操作过于频繁，请稍后重试");}
        catch(org.springframework.dao.DataAccessException e){throw new BusinessException(503,"限流服务暂不可用，请稍后重试");}
    }
}
