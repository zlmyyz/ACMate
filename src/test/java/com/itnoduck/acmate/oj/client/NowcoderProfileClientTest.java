package com.itnoduck.acmate.oj.client;
import com.itnoduck.acmate.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.*;
class NowcoderProfileClientTest {
    final NowcoderProfileClient client=new NowcoderProfileClient();
    String html(String uid,String verdict,String time){return """
        <a href='/acm/contest/profile/123/practice-coding'>练习</a>
        <table class='table-hover'><tbody><tr>
        <td><a href='/acm/contest/view-submission?submissionId=99&amp;uid=%s'>99</a></td>
        <td><a href='/acm/problem/321947'>找不同</a></td><td>%s</td>
        <td>100</td><td>2</td><td>100</td><td>33</td><td>C++</td><td>%s</td>
        </tr></tbody></table><div class='pagination'><a data-page='8'>末页</a></div>
        """.formatted(uid,verdict,time);}
    @Test void parsesCanonicalNcNumberOwnerPaginationAndOfficialTime(){
        var page=client.parse(html("123","答案正确","2026-08-24 13:22:53"),123,1);
        assertEquals(8,page.totalPages());assertEquals(1,page.submissions().size());var row=page.submissions().get(0);
        assertEquals(321947,row.problemId());assertEquals("找不同",row.title());assertEquals("OK",row.verdict());assertEquals(8,row.time().getMonthValue());
    }
    @Test void refusesDifferentOwnerMissingTableOrMalformedTime(){
        assertThrows(BusinessException.class,()->client.parse(html("999","答案正确","2026-08-24 13:22:53"),123,1));
        assertThrows(BusinessException.class,()->client.parse("<h1>请登录</h1>",123,1));
        assertThrows(BusinessException.class,()->client.parse(html("123","答案正确","2026-02-30 13:22:53"),123,1));
    }
    @Test void pendingAndUnknownStatesNeverBecomeAccepted(){
        assertTrue(client.parse(html("123","评测中","2026-08-24 13:22:53"),123,1).submissions().isEmpty());
    }
    @Test @EnabledIfSystemProperty(named="nowcoder.live-check",matches="true") void readsDifferentPublicHistoryPages(){
        var first=client.page(789817332,1);var second=client.page(789817332,2);
        assertEquals(50,first.submissions().size());assertTrue(first.totalPages()>1);assertFalse(second.submissions().isEmpty());
        assertNotEquals(first.submissions().get(0).id(),second.submissions().get(0).id());
    }
}
