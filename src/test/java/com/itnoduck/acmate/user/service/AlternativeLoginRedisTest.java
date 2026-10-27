package com.itnoduck.acmate.user.service;

import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.config.LoginProperties;
import com.itnoduck.acmate.security.DatabaseUserDetailsService;
import com.itnoduck.acmate.user.mapper.AppUserMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import java.util.List;
import java.util.ArrayList;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real Redis Lua compatibility checks; mail delivery is mocked and test keys are removed. */
@EnabledIfSystemProperty(named="account.database-check",matches="true")
class AlternativeLoginRedisTest {
    LettuceConnectionFactory factory;
    StringRedisTemplate redis;
    AlternativeLoginService service;
    JavaMailSender sender;
    String address;
    String ip;
    final List<String> cleanup=new ArrayList<>();
    static String digest(String value) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
    @BeforeEach @SuppressWarnings("unchecked") void setup() throws Exception {
        var environment=new StandardEnvironment();
        for(String file:List.of("application.yml","application-local.yml")) {
            var yaml=new YamlPropertiesFactoryBean(); yaml.setResources(new FileSystemResource("src/main/resources/"+file));
            environment.getPropertySources().addFirst(new PropertiesPropertySource(file,yaml.getObject()));
        }
        var config=new RedisStandaloneConfiguration(environment.getProperty("spring.data.redis.host","localhost"),environment.getProperty("spring.data.redis.port",Integer.class,6379));
        config.setDatabase(environment.getProperty("spring.data.redis.database",Integer.class,0));
        String password=environment.getProperty("spring.data.redis.password",""); if(!password.isBlank()) config.setPassword(RedisPassword.of(password));
        factory=new LettuceConnectionFactory(config); factory.afterPropertiesSet(); factory.start(); redis=new StringRedisTemplate(factory);
        var properties=new LoginProperties();properties.setEmailEnabled(true);properties.setMailFrom("no-reply@example.invalid");
        ObjectProvider<JavaMailSender> mail=mock(ObjectProvider.class); sender=mock(JavaMailSender.class);
        when(mail.getIfAvailable()).thenReturn(sender);when(mail.getObject()).thenReturn(sender);
        service=new AlternativeLoginService(properties,mail,redis,mock(AppUserMapper.class),new BCryptPasswordEncoder(4),mock(DatabaseUserDetailsService.class),mock(SecurityContextRepository.class),mock(SessionAuthenticationStrategy.class));
        String suffix=java.util.UUID.randomUUID().toString().replace("-", ""); address="account-test-"+suffix+"@example.invalid"; ip="test-"+suffix;
        cleanup.add("acmate:login:rate:3600:"+digest("ip:"+ip));cleanup.add("acmate:login:rate:60:"+digest("email:"+address));
        cleanup.add(key("RESET"));cleanup.add(key("LOGIN"));
    }
    String key(String purpose) throws Exception { return "acmate:login:email-code:"+(purpose.equals("LOGIN")?"":purpose+":")+digest(address); }
    String send() {
        service.sendCode(address,ip,"RESET"); var message=org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);verify(sender).send(message.capture());
        var matcher=java.util.regex.Pattern.compile("[0-9]{6}").matcher(message.getValue().getText());assertTrue(matcher.find());return matcher.group();
    }
    @AfterEach void cleanup() { try { if(redis!=null&&!cleanup.isEmpty()) redis.delete(cleanup); } finally { if(factory!=null) factory.destroy(); } }
    @Test void legacyRedisStoresHashedCodesWithTtlAndEnforcesPurposeAndSingleUse() throws Exception {
        String code=send();
        assertNotEquals(code,redis.opsForHash().get(key("RESET"),"codeHash"));
        Long ttl=redis.getExpire(key("RESET"));assertTrue(ttl>0&&ttl<=300);
        assertThrows(BusinessException.class,()->service.verifyCode(address,code,"LOGIN"));
        service.verifyCode(address,code,"RESET");assertFalse(redis.hasKey(key("RESET")));
        assertThrows(BusinessException.class,()->service.verifyCode(address,code,"RESET"));
    }
    @Test void fiveWrongAttemptsPreventSubsequentUseOfTheCorrectCode() {
        String code=send();String wrong=code.equals("000000")?"999999":"000000";
        for(int i=0;i<5;i++) assertThrows(BusinessException.class,()->service.verifyCode(address,wrong,"RESET"));
        assertThrows(BusinessException.class,()->service.verifyCode(address,code,"RESET"));
    }
}
