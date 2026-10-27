package com.itnoduck.acmate.problem.service;
import com.itnoduck.acmate.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import tools.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class NowcoderProblemImportServiceTest {
    final NowcoderProblemImportService service=new NowcoderProblemImportService(new ObjectMapper());
    @Test void acceptsOnlyCanonicalPublicNowcoderProblemUrls() {
        assertEquals("https://ac.nowcoder.com/acm/contest/19304/A",service.canonicalUrl("https://ac.nowcoder.com/acm/contest/19304/a?from=share#content").toString());
        assertEquals("https://www.nowcoder.com/practice/aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",service.canonicalUrl("https://nowcoder.com/practice/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA/").toString());
        assertEquals("https://ac.nowcoder.com/acm/problem/123",service.canonicalUrl("https://ac.nowcoder.com/acm/problem/123").toString());
    }
    @Test void rejectsSsrfRedirectPathsCredentialsAndInvalidSchemesBeforeFetching() {
        for(String url:List.of("http://ac.nowcoder.com/acm/problem/1","https://127.0.0.1/acm/problem/1","https://ac.nowcoder.com.evil.test/acm/problem/1","https://evil@ac.nowcoder.com/acm/problem/1","https://ac.nowcoder.com:8443/acm/problem/1","https://ac.nowcoder.com/redirect?url=http://localhost","https://ac.nowcoder.com/acm/problem/%31","https:opaque","file:///etc/passwd","https://www.nowcoder.com/discuss/123"))
            assertEquals(400,assertThrows(BusinessException.class,()->service.preview(url),url).getCode());
    }
    @Test void extractsPracticeBodySamplesAndTagsWithoutExecutingScripts() {
        String body="""
        {"code":0,"data":{"question":{"title":"测试题","problemId":123,"difficulty":2,
        "content":"<p>求 <img src='https://www.nowcoder.com/equation?tex=a' alt='a'> 的值</p><script>alert(1)</script>",
        "inputDesc":"<p>一个整数</p>","outputDesc":"<p>结果</p>","samples":[{"input":"1\\n2","output":"3","note":"<b>示例说明</b>"}]},"questionTags":[{"name":"数学"}]}}
        """;
        var result=service.parsePractice(body,URI.create("https://www.nowcoder.com/practice/aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"));
        assertEquals("NC123",result.externalProblemKey());assertEquals("NOWCODER",result.platform());
        assertEquals("测试题",result.title());assertEquals("2",result.difficulty());assertEquals("数学",result.tags());
        assertTrue(result.contentMd().contains("## 输入描述"));assertTrue(result.contentMd().contains("1\n2"));
        assertTrue(result.contentMd().contains("https://www.nowcoder.com/equation?tex=a"));
        assertFalse(result.contentMd().contains("alert"));assertTrue(result.warnings().isEmpty());
    }
    @Test void extractsContestStatementAndPreservesSampleLineBreaks() {
        var result=service.parseContest("""
        <title>A-测试题_比赛</title><div class="question-title">测试题</div>
        <div class="subject-item-wrap">时间限制：1秒</div>
        <div class="subject-describe"><div class="subject-question">计算答案。</div><h2>输入描述:</h2><pre>两个数</pre>
        <div class="question-oi-hd">示例1</div><h2>输入</h2><pre>1
        2</pre><h2>输出</h2><pre>3</pre><button>复制</button></div><script>window.pageInfo={problemId:'123'};</script>
        """,URI.create("https://ac.nowcoder.com/acm/contest/10/A"));
        assertEquals("NC123",result.externalProblemKey());assertTrue(result.contentMd().contains("1\n2"));
        assertTrue(result.contentMd().contains("计算答案"));assertFalse(result.contentMd().contains("复制"));
        assertEquals(2,result.warnings().size());
    }
    @Test void rejectsLoginPagesMissingBodiesAndPaidQuestions() {
        assertEquals(422,assertThrows(BusinessException.class,()->service.parseContest("<title>请登录</title>",URI.create("https://ac.nowcoder.com/acm/problem/1"))).getCode());
        assertEquals(422,assertThrows(BusinessException.class,()->service.parsePractice("{\"code\":0,\"data\":{\"question\":{\"paidQuestion\":true}}}",URI.create("https://www.nowcoder.com/practice/aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"))).getCode());
    }
    @Test @EnabledIfSystemProperty(named="nowcoder.live-check",matches="true")
    void readsActualPublicPracticeAndContestWithUtf8Content() {
        var practice=service.preview("https://www.nowcoder.com/practice/9d381551b6ab40c4b5c3c8d60fe4066e");
        assertEquals("复读机",practice.title());assertEquals("NC294009",practice.externalProblemKey());
        assertTrue(practice.contentMd().contains("输入"));assertFalse(practice.contentMd().contains("�"));
        var contest=service.preview("https://ac.nowcoder.com/acm/contest/19304/A");
        assertEquals("比大小",contest.title());assertEquals("NC212955",contest.externalProblemKey());
        var standalone=service.preview("https://ac.nowcoder.com/acm/problem/212955");
        assertEquals("NC212955",standalone.externalProblemKey());assertTrue(standalone.title().contains("比大小"));
    }
}
