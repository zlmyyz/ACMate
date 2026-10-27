package com.itnoduck.acmate.user.controller;
import com.itnoduck.acmate.user.entity.AppUser;
import com.itnoduck.acmate.user.mapper.AppUserMapper;
import com.itnoduck.acmate.security.AuthenticatedUser;
import com.itnoduck.acmate.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class CurrentUserRoleTest {
 @Test void readsGrantedAndRevokedRolesFromDatabaseInsteadOfSessionSnapshot() {
  var mapper=mock(AppUserMapper.class);
  var controller=new UserController(null,null,null,mapper);
  var principal=new AuthenticatedUser(2L,"plji","hash","old",null,null,null,false,true,List.of());
  var user=new AppUser(); user.setId(2L);user.setUsername("plji");user.setStatus(1);user.setIsAdmin(1);
  when(mapper.selectById(2L)).thenReturn(user);
  assertTrue(controller.me(principal).admin());
  user.setIsAdmin(0);
  assertFalse(controller.me(principal).admin());
  user.setStatus(0);
  assertEquals(401,assertThrows(BusinessException.class,()->controller.me(principal)).getCode());
 }
}
