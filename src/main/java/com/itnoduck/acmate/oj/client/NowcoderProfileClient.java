package com.itnoduck.acmate.oj.client;

import com.itnoduck.acmate.common.exception.BusinessException;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.*;
import java.util.regex.Pattern;

/** Reads only the public, paginated programming submission table. No login cookies. */
@Component
public class NowcoderProfileClient {
    public record Submission(long id,long problemId,String title,String verdict,String language,LocalDateTime time) {}
    public record Page(int page,int totalPages,List<Submission> submissions) {}
    private static final DateTimeFormatter TIME=DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss").withResolverStyle(ResolverStyle.STRICT);
    public Page page(long uid,int page) {
        if(uid<=0||page<=0||page>100000)throw new BusinessException(400,"牛客 UID 或页码无效");
        return parse(fetch(URI.create("https://ac.nowcoder.com/acm/contest/profile/"+uid+"/practice-coding?page="+page+"&pageSize=50&statusTypeFilter=-1&languageCategoryFilter=-1&orderType=DESC")),uid,page);
    }
    public Page parse(String html,long uid,int page) {
        var doc=Jsoup.parse(html,"https://ac.nowcoder.com");
        if(doc.select("a[href='/acm/contest/profile/"+uid+"/practice-coding']").isEmpty()||doc.selectFirst("table.table-hover")==null)
            throw unavailable();
        int pages=1;
        for(var link:doc.select(".pagination a[data-page]")) {
            try { pages=Math.max(pages,Integer.parseInt(link.attr("data-page"))); } catch(NumberFormatException ex){throw unavailable();}
        }
        if(pages>100000)throw unavailable();
        var rows=new ArrayList<Submission>();
        for(var row:doc.select("table.table-hover tbody tr")) {
            var cells=row.select("td");
            if(cells.size()<9) { if(!row.select("a[href*='submissionId=']").isEmpty())throw unavailable(); continue; }
            var record=cells.get(0).selectFirst("a[href]");var problem=cells.get(1).selectFirst("a[href]");
            if(record==null||problem==null)throw unavailable();
            var id=Pattern.compile("(?:\\?|&)submissionId=([1-9][0-9]*)(?:&|$)").matcher(record.attr("href"));
            var owner=Pattern.compile("(?:\\?|&)uid=([1-9][0-9]*)(?:&|$)").matcher(record.attr("href"));
            var pid=Pattern.compile("/acm/problem/([1-9][0-9]*)").matcher(problem.attr("href"));
            if(!id.find()||!owner.find()||!pid.matches()||!owner.group(1).equals(Long.toString(uid)))throw unavailable();
            String label=cells.get(2).text();
            String verdict="部分正确".equals(label)?"PARTIAL_ACCEPTED":NowcoderPublicClient.verdict(label);
            if(verdict==null)continue; // Pending/unknown states never become accepted records.
            try {
                String title=problem.text(),language=cells.get(7).text();
                if(title.length()>255||language.length()>64)throw unavailable();
                rows.add(new Submission(Long.parseLong(id.group(1)),Long.parseLong(pid.group(1)),title,verdict,language,LocalDateTime.parse(cells.get(8).text(),TIME)));
            }catch(NumberFormatException|java.time.DateTimeException ex){throw unavailable();}
        }
        if(rows.isEmpty()&&page<pages&&doc.select("table.table-hover tbody tr td a[href*='submissionId=']").isEmpty())throw unavailable();
        return new Page(page,pages,List.copyOf(rows));
    }
    protected String fetch(URI uri) {
        HttpURLConnection connection=null;
        try {
            connection=(HttpURLConnection)uri.toURL().openConnection(Proxy.NO_PROXY);
            connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(5000);connection.setReadTimeout(8000);
            connection.setRequestProperty("User-Agent","ACMate/1.0 (public profile sync)");
            if(connection.getResponseCode()!=200)throw unavailable();
            try(var stream=connection.getInputStream()) {
                byte[] bytes=stream.readNBytes(2*1024*1024+1);
                if(bytes.length>2*1024*1024)throw unavailable();
                return new String(bytes,StandardCharsets.UTF_8);
            }
        }catch(java.io.IOException ex){throw unavailable();}finally{if(connection!=null)connection.disconnect();}
    }
    private BusinessException unavailable(){return new BusinessException(502,"牛客公开个人记录暂不可读或格式已变化，请稍后重试");}
}
