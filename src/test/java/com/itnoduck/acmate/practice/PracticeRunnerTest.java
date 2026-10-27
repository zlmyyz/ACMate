package com.itnoduck.acmate.practice;
import com.itnoduck.acmate.common.exception.BusinessException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
class PracticeRunnerTest {
 @TempDir Path folder;
 @Test void disabledRunnerNeverExecutesOnApplicationHost(){
  var runner=new PracticeRunner(false,"http://127.0.0.1:1",folder.resolve("missing").toString(),new ObjectMapper());
  assertFalse(runner.available());assertEquals(503,assertThrows(BusinessException.class,()->runner.run("PYTHON3","print(1)","")).getCode());
 }
 @Test void sendsOnlyFixedSandboxCommandsAndDeletesCompiledCache()throws Exception{
  Path token=folder.resolve("token");Files.writeString(token,"test-token");var count=new AtomicInteger();var deletes=new AtomicInteger();
  var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);var json=new ObjectMapper();
  server.createContext("/run",exchange->{
   assertEquals("Bearer test-token",exchange.getRequestHeaders().getFirst("Authorization"));
   var body=json.readTree(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));var cmd=body.path("cmd").get(0);
   assertTrue(cmd.path("memoryLimit").asLong()>0);assertTrue(cmd.path("clockLimit").asLong()>0);assertEquals(64,cmd.path("procLimit").asInt());
   String response=count.getAndIncrement()==0?"[{\"status\":\"Accepted\",\"fileIds\":{\"main\":\"cached-binary\"}}]":"[{\"status\":\"Accepted\",\"files\":{\"stdout\":\"3\\n\",\"stderr\":\"\"},\"time\":1000000,\"memory\":102400}]";
   byte[] bytes=response.getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();
  });
  server.createContext("/file/cached-binary",exchange->{if(exchange.getRequestMethod().equals("DELETE"))deletes.incrementAndGet();exchange.sendResponseHeaders(200,-1);exchange.close();});server.start();
  try{
   var runner=new PracticeRunner(true,"http://127.0.0.1:"+server.getAddress().getPort(),token.toString(),json);
   var result=runner.run("CPP17","int main(){}","1 2");assertEquals("FINISHED",result.status());assertEquals("3\n",result.stdout());assertEquals(2,count.get());assertEquals(1,deletes.get());
  }finally{server.stop(0);}
 }
}
