package com.itnoduck.acmate.practice;

import com.itnoduck.acmate.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import tools.jackson.databind.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Semaphore;

/** Optional sample runner. It never executes processes on the application host or writes OJ statistics. */
@Service
public class PracticeRunner {
    public record Result(String status,String stdout,String stderr,long timeMs,long memoryKb){}
    private final boolean enabled;
    private final String token;
    private final RestClient client;
    private final ObjectMapper json;
    private final Semaphore slots=new Semaphore(1);
    public PracticeRunner(@Value("${acmate.practice.enabled:false}") boolean enabled,
            @Value("${acmate.practice.url:http://127.0.0.1:5050}") String url,
            @Value("${acmate.practice.token-file:./deploy/judge/.judge-token}") String tokenFile,ObjectMapper json){
        this.enabled=enabled;this.json=json;
        String value="";
        if(enabled)try{value=Files.readString(Path.of(tokenFile)).strip();}catch(java.io.IOException ignored){}
        token=value;
        var factory=new SimpleClientHttpRequestFactory();factory.setConnectTimeout(2000);factory.setReadTimeout(50000);
        client=RestClient.builder().baseUrl(url).requestFactory(factory).defaultHeaders(h->h.setBearerAuth(token)).build();
    }
    public boolean available(){
        if(!enabled||token.isBlank())return false;
        try{return client.get().uri("/version").retrieve().toBodilessEntity().getStatusCode().is2xxSuccessful();}
        catch(Exception ignored){return false;}
    }
    public Result run(String language,String source,String input){
        if(!enabled||token.isBlank())throw new BusinessException(503,"本站样例运行环境尚未部署，可复制代码前往牛客运行或提交");
        PracticeLanguage spec;
        try{spec=PracticeLanguage.valueOf(language);}catch(IllegalArgumentException|NullPointerException ex){throw new BusinessException(400,"不支持的语言");}
        if(!slots.tryAcquire())throw new BusinessException(429,"样例运行繁忙，请稍后再试");
        String cached=null;
        try{
            Map<String,Object> copyIn=new LinkedHashMap<>();
            if(spec.artifact==null){
                copyIn.put(spec.filename,Map.of("content",source));
            }else{
                String output=spec.artifact;
                var compile=command(spec.compile,Map.of(spec.filename,Map.of("content",source)),"",true);
                compile.put("copyOutCached",List.of(output));
                var result=execute(compile);
                cached=result.path("fileIds").path(output).asText("");
                if(!"Accepted".equals(result.path("status").asText(""))||cached.isBlank()) {
                    String diagnostic=result.path("files").path("stdout").asText("")+result.path("files").path("stderr").asText("");
                    if(diagnostic.isBlank())diagnostic="编译未完成："+result.path("status").asText("System Error");
                    return new Result("COMPILE_ERROR","",diagnostic,0,0);
                }
                copyIn.put(output,Map.of("fileId",cached));
            }
            if(spec==PracticeLanguage.CSHARP)copyIn.put("Main.runtimeconfig.json",Map.of("content","{\"runtimeOptions\":{\"tfm\":\"net10.0\",\"framework\":{\"name\":\"Microsoft.NETCore.App\",\"version\":\"10.0.0\"}}}"));
            var result=execute(command(spec.run,copyIn,input,false));
            String status=result.path("status").asText("System Error");
            for(var error:result.path("fileError")) {
                String type=error.path("type").asText("");
                if(type.equals("CollectSizeExceeded")||type.equals("CopyOutSizeExceeded"))status="Output Limit Exceeded";
            }
            return new Result(status.equals("Accepted")?"FINISHED":status,result.path("files").path("stdout").asText(""),result.path("files").path("stderr").asText(""),result.path("time").asLong(0)/1_000_000,result.path("memory").asLong(0)/1024);
        }catch(BusinessException ex){throw ex;}
        catch(Exception ex){throw new BusinessException(503,"样例运行服务不可用，请稍后重试或前往牛客运行");}
        finally{
            if(cached!=null&&!cached.isBlank())try{client.delete().uri("/file/{id}",cached).retrieve().toBodilessEntity();}catch(Exception ignored){}
            slots.release();
        }
    }
    private Map<String,Object> command(List<String> args,Map<String,Object> files,String input,boolean compile){
        var cmd=new LinkedHashMap<String,Object>();cmd.put("args",args);cmd.put("env",List.of("PATH=/usr/local/bin:/usr/bin:/bin","LANG=C.UTF-8","HOME=/w","GOCACHE=/usr/local/go/acmate-cache","GOPATH=/w/go","GOENV=off","GOTOOLCHAIN=local","GOPROXY=off","GOSUMDB=off","GOMAXPROCS=2","CGO_ENABLED=0","DOTNET_ROOT=/usr/share/dotnet","DOTNET_CLI_HOME=/w","DOTNET_SYSTEM_GLOBALIZATION_INVARIANT=1","DOTNET_EnableDiagnostics=0","DOTNET_CLI_TELEMETRY_OPTOUT=1","DOTNET_GCHeapHardLimit=0x08000000"));
        cmd.put("files",List.of(Map.of("content",input),Map.of("name","stdout","max",65536),Map.of("name","stderr","max",16384)));
        cmd.put("cpuLimit",compile?20_000_000_000L:2_000_000_000L);cmd.put("clockLimit",compile?40_000_000_000L:5_000_000_000L);
        cmd.put("memoryLimit",(compile?768L:256L)*1024*1024);cmd.put("procLimit",64);cmd.put("copyIn",files);return cmd;
    }
    private JsonNode execute(Map<String,Object> cmd){
        String body=client.post().uri("/run").body(Map.of("cmd",List.of(cmd))).retrieve().body(String.class);
        var root=json.readTree(body==null?"[]":body);
        if(!root.isArray()||root.size()!=1)throw new BusinessException(503,"样例运行服务返回异常");
        return root.get(0);
    }
}

