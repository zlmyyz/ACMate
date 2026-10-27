package com.itnoduck.acmate.practice;
import com.itnoduck.acmate.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.InetAddress;
import java.util.Locale;

/** In development only direct loopback requests may run code; production requires a separately isolated judge. */
@Component
public class PracticeAccess {
    private final String mode;
    public PracticeAccess(@Value("${acmate.practice.mode:${ACMATE_PRACTICE_MODE:disabled}}") String mode){this.mode=mode == null ? "disabled" : mode.strip().toLowerCase(Locale.ROOT);}
    public boolean allowed(HttpServletRequest request){
        if("isolated".equals(mode))return true;
        if(!"local".equals(mode))return false;
        if(request.getHeader("Forwarded")!=null||request.getHeader("X-Forwarded-For")!=null||request.getHeader("X-Real-IP")!=null)return false;
        String host=request.getServerName().toLowerCase(Locale.ROOT);
        if(!host.equals("localhost")&&!host.equals("127.0.0.1")&&!host.equals("::1")&&!host.equals("[::1]"))return false;
        String remote=request.getRemoteAddr();
        if(!"127.0.0.1".equals(remote)&&!"::1".equals(remote)&&!"0:0:0:0:0:0:0:1".equals(remote))return false;
        try{return InetAddress.getByName(remote).isLoopbackAddress();}catch(Exception e){return false;}
    }
    public void require(HttpServletRequest request){if(!allowed(request))throw new BusinessException(503,"判题环境尚未隔离，暂不对外开放");}
}
