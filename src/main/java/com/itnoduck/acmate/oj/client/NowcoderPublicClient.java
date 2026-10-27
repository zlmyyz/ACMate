package com.itnoduck.acmate.oj.client;

import com.itnoduck.acmate.common.exception.BusinessException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

@Component
public class NowcoderPublicClient {
    public record OfficialSubmission(long id,long userId,long problemId,String verdict,String language,LocalDateTime submittedTime) {}
    private final ObjectMapper json;
    public NowcoderPublicClient(ObjectMapper json){this.json=json;}
    public List<OfficialSubmission> submissions(long contest,long uid,long problem){
        var result=new ArrayList<OfficialSubmission>();
        for(int page=1;page<=3;page++){
            String body=fetch(URI.create("https://ac.nowcoder.com/acm-heavy/acm/contest/status-list?id="+contest+"&page="+page+"&pageSize=20&searchUserName="+uid+"&problemIdFilter="+problem));
            var root=readResponse(body);
            if(root.path("code").asInt(-1)!=0)throw new BusinessException(502,"牛客未开放此题的公开提交记录，请在官方查看结果");
            var data=root.path("data");
            if(!data.path("data").isArray())throw new BusinessException(502,"牛客提交记录结构变化，暂时无法同步");
            result.addAll(parse(body,uid,problem));
            if(page>=data.path("basicInfo").path("pageCount").asInt(1))break;
        }
        return result;
    }
    public List<OfficialSubmission> parse(String body,long uid,long problem){
        var rows=new ArrayList<OfficialSubmission>();
        for(var row:readResponse(body).path("data").path("data")){
            long id=row.path("submissionId").asLong(0),user=row.path("userId").asLong(0),pid=row.path("problemId").asLong(0),millis=row.path("submitTime").asLong(0);
            if(id<=0||user!=uid||pid!=problem||row.path("isTeam").asBoolean(false)||millis<=0)continue;
            String verdict=verdict(row.path("statusMessage").asText(""));
            if(verdict==null)continue;
            rows.add(new OfficialSubmission(id,user,pid,verdict,row.path("languageName").asText(row.path("language").asText("")),LocalDateTime.ofInstant(Instant.ofEpochMilli(millis),ZoneId.of("Asia/Shanghai"))));
        }
        return rows;
    }
    public static String verdict(String value){
        return switch(value.strip()){
            case "答案正确","Accepted","AC"->"OK";
            case "答案错误","Wrong Answer","WA"->"WRONG_ANSWER";
            case "运行超时","时间超限","Time Limit Exceeded","TLE"->"TIME_LIMIT_EXCEEDED";
            case "内存超限","Memory Limit Exceeded","MLE"->"MEMORY_LIMIT_EXCEEDED";
            case "编译错误","Compile Error","Compilation Error","CE"->"COMPILATION_ERROR";
            case "运行错误","段错误","Runtime Error","RE"->"RUNTIME_ERROR";
            case "输出超限","Output Limit Exceeded","OLE"->"OUTPUT_LIMIT_EXCEEDED";
            case "格式错误","Presentation Error","PE"->"PRESENTATION_ERROR";
            default->null;
        };
    }
    private tools.jackson.databind.JsonNode readResponse(String body){
        try{return json.readTree(body);}
        catch(tools.jackson.core.JacksonException ex){throw new BusinessException(502,"牛客返回的公开记录格式异常，请稍后重试");}
    }
    protected String fetch(URI uri){
        HttpURLConnection connection=null;
        try{
            connection=(HttpURLConnection)uri.toURL().openConnection(Proxy.NO_PROXY);
            connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(5000);connection.setReadTimeout(8000);
            connection.setRequestProperty("User-Agent","ACMate/1.0 (public submission sync)");
            if(connection.getResponseCode()!=200)throw new BusinessException(502,"牛客公开记录暂不可用，请稍后重试");
            try(var input=connection.getInputStream()){
                byte[] bytes=input.readNBytes(2*1024*1024+1);
                if(bytes.length>2*1024*1024)throw new BusinessException(502,"牛客响应过大，无法同步");
                return new String(bytes,StandardCharsets.UTF_8);
            }
        }catch(java.io.IOException|tools.jackson.core.JacksonException ex){throw new BusinessException(502,"读取牛客公开记录失败，请稍后重试");}
        finally{if(connection!=null)connection.disconnect();}
    }
}
