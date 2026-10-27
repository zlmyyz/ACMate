package com.itnoduck.acmate.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** Trust forwarded addresses only from explicitly configured proxy IPs, walking the chain from right to left. */
@Component
public class ClientIpResolver {
    private final Set<String> proxies;
    public ClientIpResolver(@Value("${acmate.trusted-proxies:}") String configured){
        proxies=Arrays.stream(configured.split(",")).map(String::strip).filter(s->!s.isEmpty()).map(ClientIpResolver::literal).filter(java.util.Objects::nonNull).collect(Collectors.toUnmodifiableSet());
    }
    private static String literal(String value){
        if(value==null||value.isBlank()||!value.matches("[0-9a-fA-F:.]+")||(!value.contains(":")&&!value.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}")))return null;
        try{return InetAddress.getByName(value).getHostAddress();}catch(Exception e){return null;}
    }
    public String resolve(HttpServletRequest request){
        String remote=literal(request.getRemoteAddr());
        if(remote==null)return "unknown";
        if(!proxies.contains(remote))return remote;
        String forwarded=request.getHeader("X-Forwarded-For");
        if(forwarded==null || forwarded.length()>2048)return remote;
        String[] chain=forwarded.split(",",-1); if(chain.length>32)return remote;
        String current=remote;
        for(int i=chain.length-1;i>=0 && proxies.contains(current);i--){
            String previous=literal(chain[i].strip());if(previous==null)return remote;current=previous;
        }
        return current;
    }
}
