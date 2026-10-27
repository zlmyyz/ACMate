package com.itnoduck.acmate.user.service;
import com.itnoduck.acmate.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class AccountMergePermissionsTest {
 @Test void administratorOnEitherSideCannotBeMergedBySelfService(){
  for(boolean sourceAdmin:List.of(true,false)){
   JdbcTemplate jdbc=mock(JdbcTemplate.class);
   when(jdbc.queryForList(anyString(),eq(1L),eq(2L))).thenReturn(List.of(Map.of("id",1L,"status",1,"is_admin",sourceAdmin?1:0),Map.of("id",2L,"status",1,"is_admin",sourceAdmin?0:1)));
   var service=new AccountMergeService(jdbc,mock(SessionRegistry.class),mock(StringRedisTemplate.class));
   assertEquals(403,assertThrows(BusinessException.class,()->service.merge(1,2)).getCode());
   verify(jdbc,never()).update(anyString(),any(Object[].class));
  }
 }
}
