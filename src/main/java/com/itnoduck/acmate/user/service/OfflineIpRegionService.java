package com.itnoduck.acmate.user.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import java.net.InetAddress;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Read-only XDB lookup. All address searches stay inside this process. */
@Service
public class OfflineIpRegionService {
    private final byte[] ipv4=load("ip2region_v4.xdb");
    private final byte[] ipv6=load("ip2region_v6.xdb");
    private byte[] load(String file) {
        try(var input=new ClassPathResource("geolocation/"+file).getInputStream()) {return input.readAllBytes();}
        catch(java.io.IOException exception) {return new byte[0];}
    }
    public String resolve(String literal) {
        if(literal==null||!literal.matches("[0-9a-fA-F:.]+")||!literal.contains(".")&&!literal.contains(":")) return "未知地区";
        try {
            var address=InetAddress.getByName(literal);byte[] ip=address.getAddress();
            if(address.isLoopbackAddress()||address.isSiteLocalAddress()||address.isLinkLocalAddress()||address.isAnyLocalAddress()||address.isMulticastAddress()
                    ||ip.length==16&&(ip[0]&254)==252||ip.length==4&&(ip[0]&255)==100&&(ip[1]&255)>=64&&(ip[1]&255)<=127) return "本地网络";
            String raw=lookup(ip,ip.length==4?ipv4:ipv6);
            String[] fields=raw.split("\\|",-1);
            if(fields.length<2) return "未知地区";
            String country=fields[0],province=fields[1];
            return !province.isBlank()&&!province.equals("0")?country.equals("中国")?province:country+" · "+province:country.isBlank()||country.equals("0")?"未知地区":country;
        } catch(Exception ignored) {return "未知地区";}
    }
    private String lookup(byte[] ip,byte[] data) {
        if(data.length<524544) return "";
        var buffer=ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        int vector=256+(((ip[0]&255)*256+(ip[1]&255))*8);
        long start=Integer.toUnsignedLong(buffer.getInt(vector)),end=Integer.toUnsignedLong(buffer.getInt(vector+4));
        int width=ip.length,step=width*2+6;
        if(start<524544||end<start||end+step>data.length) return "";
        long lower=0,upper=(end-start)/step;
        while(lower<=upper) {
            long middle=(lower+upper)/2;int offset=Math.toIntExact(start+middle*step);
            int before=compare(ip,data,offset),after=compare(ip,data,offset+width);
            if(before<0) upper=middle-1;
            else if(after>0) lower=middle+1;
            else {
                int length=Short.toUnsignedInt(buffer.getShort(offset+width*2));
                long pointer=Integer.toUnsignedLong(buffer.getInt(offset+width*2+2));
                if(pointer+length>data.length) return "";
                return new String(data,Math.toIntExact(pointer),length,StandardCharsets.UTF_8);
            }
        }
        return "";
    }
    private int compare(byte[] ip,byte[] data,int offset) {
        for(int index=0;index<ip.length;index++) {
            int stored=offset+(ip.length==4?3-index:index);
            int difference=Integer.compare(ip[index]&255,data[stored]&255);
            if(difference!=0) return difference;
        }
        return 0;
    }
}
