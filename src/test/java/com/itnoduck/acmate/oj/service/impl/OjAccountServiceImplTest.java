package com.itnoduck.acmate.oj.service.impl;

import com.itnoduck.acmate.auditlog.service.AuditLogService;
import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.oj.client.CodeforcesApiClient;
import com.itnoduck.acmate.oj.client.CodeforcesProblemDto;
import com.itnoduck.acmate.oj.client.CodeforcesSubmissionDto;
import com.itnoduck.acmate.oj.entity.FirstAc;
import com.itnoduck.acmate.oj.entity.OjAccount;
import com.itnoduck.acmate.oj.entity.OjSubmission;
import com.itnoduck.acmate.oj.mapper.FirstAcMapper;
import com.itnoduck.acmate.oj.mapper.OjAccountMapper;
import com.itnoduck.acmate.oj.mapper.OjSubmissionMapper;
import com.itnoduck.acmate.security.AuthenticatedUser;
import com.itnoduck.acmate.synctask.entity.SyncTaskLog;
import com.itnoduck.acmate.synctask.mapper.SyncTaskLogMapper;
import com.itnoduck.acmate.testutil.MybatisPlusTestHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OjAccountServiceImplTest {

    @Mock private OjAccountMapper accountMapper;
    @Mock private OjSubmissionMapper submissionMapper;
    @Mock private FirstAcMapper firstAcMapper;
    @Mock private SyncTaskLogMapper taskLogMapper;
    @Mock private AuditLogService auditLogService;
    @Mock private CodeforcesApiClient cfClient;
    @InjectMocks private OjAccountServiceImpl service;

    private static final Long USER_ID = 1L;
    private static final Long ADMIN_ID = 100L;
    private static final Long ACCOUNT_ID = 10L;

    private AuthenticatedUser normalUser;
    private AuthenticatedUser adminUser;

    @BeforeAll
    static void initMybatisPlus() {
        MybatisPlusTestHelper.initEntityTables();
    }

    @BeforeEach
    void setup() {
        normalUser = new AuthenticatedUser(USER_ID, "normal", "hash", "Normal",
                null, null, null, false, true, List.of());
        adminUser = new AuthenticatedUser(ADMIN_ID, "admin", "hash", "Admin",
                null, null, null, true, true, List.of());
    }

    private OjAccount verifiedAccount() {
        OjAccount acc = new OjAccount();
        acc.setId(ACCOUNT_ID);
        acc.setUserId(USER_ID);
        acc.setPlatform("CODEFORCES");
        acc.setExternalUserId("test_handle");
        acc.setDisplayName("test_handle");
        acc.setVerifyStatus(1);
        acc.setSyncEnabled(1);
        acc.setLastSyncSuccess(1);
        return acc;
    }

    // ---- getMyAccount ----

    @Test
    void getMyAccount_noAccount_returnsHasAccountFalse() {
        when(accountMapper.selectOne(any())).thenReturn(null);
        var result = service.getMyAccount(normalUser);
        assertEquals(false, result.get("hasAccount"));
    }

    @Test
    void getMyAccount_withAccount_returnsDetails() {
        OjAccount acc = verifiedAccount();
        acc.setLastSyncTime(LocalDateTime.of(2025, 1, 1, 12, 0));
        when(accountMapper.selectOne(any())).thenReturn(acc);
        var result = service.getMyAccount(normalUser);
        assertEquals(true, result.get("hasAccount"));
        assertEquals("CODEFORCES", result.get("platform"));
        assertEquals("test_handle", result.get("externalUserId"));
        assertEquals(1, result.get("verifyStatus"));
    }

    // ---- bind ----

    @Test
    void bind_nullHandle_throws400() {
        var e = assertThrows(BusinessException.class, () -> service.bind(null, normalUser));
        assertEquals(400, e.getCode());
    }

    @Test
    void bind_blankHandle_throws400() {
        var e = assertThrows(BusinessException.class, () -> service.bind("  ", normalUser));
        assertEquals(400, e.getCode());
    }

    @Test
    void bind_existingAccount_throws409() {
        when(accountMapper.selectOne(any())).thenReturn(verifiedAccount());
        var e = assertThrows(BusinessException.class, () -> service.bind("new_handle", normalUser));
        assertEquals(409, e.getCode());
        assertTrue(e.getMessage().contains("已绑定"));
    }

    @Test
    void bind_conflictingHandle_throws409() {
        when(accountMapper.selectOne(any()))
                .thenReturn(null)
                .thenReturn(verifiedAccount());
        var e = assertThrows(BusinessException.class, () -> service.bind("test_handle", normalUser));
        assertEquals(409, e.getCode());
        assertTrue(e.getMessage().contains("已被其他用户绑定"));
    }

    @Test
    void bind_success_insertsAccount() {
        when(accountMapper.selectOne(any())).thenReturn(null, null);
        service.bind(" new_handle ", normalUser);
        ArgumentCaptor<OjAccount> captor = ArgumentCaptor.forClass(OjAccount.class);
        verify(accountMapper).insert(captor.capture());
        OjAccount saved = captor.getValue();
        assertEquals(USER_ID, saved.getUserId());
        assertEquals("CODEFORCES", saved.getPlatform());
        assertEquals("new_handle", saved.getExternalUserId());
        assertEquals(0, saved.getVerifyStatus());
        assertEquals(1, saved.getSyncEnabled());
    }

    // ---- unbind ----

    @Test
    void unbind_deletesAccount() {
        service.unbind(normalUser);
        verify(accountMapper).delete(any());
    }

    // ---- getPendingAccounts ----

    @Test
    void getPendingAccounts_nonAdmin_throws403() {
        var e = assertThrows(BusinessException.class, () -> service.getPendingAccounts(normalUser));
        assertEquals(403, e.getCode());
    }

    @Test
    void getPendingAccounts_admin_returnsPendingOnly() {
        OjAccount pending = verifiedAccount();
        pending.setVerifyStatus(0);
        when(accountMapper.selectList(any())).thenReturn(List.of(pending));
        var result = service.getPendingAccounts(adminUser);
        assertEquals(1, result.size());
        assertEquals(0, result.get(0).get("verifyStatus"));
    }

    // ---- verify ----

    @Test
    void verify_nonAdmin_throws403() {
        var e = assertThrows(BusinessException.class, () -> service.verify(1L, 1, normalUser));
        assertEquals(403, e.getCode());
    }

    @Test
    void verify_notFound_throws404() {
        when(accountMapper.selectById(ACCOUNT_ID)).thenReturn(null);
        var e = assertThrows(BusinessException.class, () -> service.verify(ACCOUNT_ID, 1, adminUser));
        assertEquals(404, e.getCode());
    }

    @Test
    void verify_approve_setsStatusAndAudits() {
        OjAccount acc = verifiedAccount();
        acc.setVerifyStatus(0);
        when(accountMapper.selectById(ACCOUNT_ID)).thenReturn(acc);
        service.verify(ACCOUNT_ID, 1, adminUser);
        assertEquals(1, acc.getVerifyStatus());
        verify(accountMapper).updateById(acc);
        verify(auditLogService).log(eq(ADMIN_ID), eq("OJ_ACCOUNT_VERIFIED"), eq("OJ_ACCOUNT"), eq(ACCOUNT_ID),
                eq("approved"), eq("0"), eq("1"));
    }

    @Test
    void verify_reject_setsStatusAndAudits() {
        OjAccount acc = verifiedAccount();
        acc.setVerifyStatus(0);
        when(accountMapper.selectById(ACCOUNT_ID)).thenReturn(acc);
        service.verify(ACCOUNT_ID, 2, adminUser);
        assertEquals(2, acc.getVerifyStatus());
        verify(auditLogService).log(eq(ADMIN_ID), eq("OJ_ACCOUNT_REJECTED"), eq("OJ_ACCOUNT"), eq(ACCOUNT_ID),
                eq("rejected"), eq("0"), eq("2"));
    }

    // ---- syncMyAccount ----

    @Test
    void sync_noAccount_throws404() {
        when(accountMapper.selectOne(any())).thenReturn(null);
        var e = assertThrows(BusinessException.class, () -> service.syncMyAccount(normalUser));
        assertEquals(404, e.getCode());
        assertTrue(e.getMessage().contains("未绑定"));
    }

    @Test
    void sync_wrongPlatform_throws400() {
        OjAccount acc = verifiedAccount();
        acc.setPlatform("ATCODER");
        when(accountMapper.selectOne(any())).thenReturn(acc);
        var e = assertThrows(BusinessException.class, () -> service.syncMyAccount(normalUser));
        assertEquals(400, e.getCode());
    }

    @Test
    void sync_notVerified_throws409() {
        OjAccount acc = verifiedAccount();
        acc.setVerifyStatus(0);
        when(accountMapper.selectOne(any())).thenReturn(acc);
        var e = assertThrows(BusinessException.class, () -> service.syncMyAccount(normalUser));
        assertEquals(409, e.getCode());
        assertTrue(e.getMessage().contains("未通过审核"));
    }

    @Test
    void sync_cfApiUnreachable_throws503() {
        OjAccount acc = verifiedAccount();
        when(accountMapper.selectOne(any())).thenReturn(acc);
        when(submissionMapper.selectOne(any())).thenReturn(null);
        when(cfClient.fetchSubmissions("test_handle", 1, 500))
                .thenThrow(new BusinessException(503, "Codeforces 服务暂时不可达"));
        var e = assertThrows(BusinessException.class, () -> service.syncMyAccount(normalUser));
        assertEquals(503, e.getCode());
    }

    @Test
    void sync_cfApiNotFound_throws404() {
        OjAccount acc = verifiedAccount();
        when(accountMapper.selectOne(any())).thenReturn(acc);
        when(submissionMapper.selectOne(any())).thenReturn(null);
        when(cfClient.fetchSubmissions("test_handle", 1, 500))
                .thenThrow(new BusinessException(404, "Codeforces 账号 test_handle 不存在"));
        assertThrows(BusinessException.class, () -> service.syncMyAccount(normalUser));
    }

    @Test
    void sync_cfApiRateLimit_throws429() {
        OjAccount acc = verifiedAccount();
        when(accountMapper.selectOne(any())).thenReturn(acc);
        when(submissionMapper.selectOne(any())).thenReturn(null);
        when(cfClient.fetchSubmissions("test_handle", 1, 500))
                .thenThrow(new BusinessException(429, "Codeforces API 请求频率过高"));
        var e = assertThrows(BusinessException.class, () -> service.syncMyAccount(normalUser));
        assertEquals(429, e.getCode());
    }

    @Test
    void sync_emptyResult_returnsZeroCounts() {
        OjAccount acc = verifiedAccount();
        when(accountMapper.selectOne(any())).thenReturn(acc);
        when(submissionMapper.selectOne(any())).thenReturn(null);
        when(cfClient.fetchSubmissions("test_handle", 1, 500)).thenReturn(Collections.emptyList());

        var result = service.syncMyAccount(normalUser);
        assertEquals("SUCCESS", result.getSyncStatus());
        assertEquals(0, result.getFetchedCount());
        assertEquals(0, result.getInsertedCount());
        assertEquals(0, result.getNewAcceptedProblemCount());
        verify(taskLogMapper).insert(any(SyncTaskLog.class));
    }

    @Test
    void sync_newSubmissions_insertsAndReturnsCounts() {
        OjAccount acc = verifiedAccount();
        when(accountMapper.selectOne(any())).thenReturn(acc);
        when(submissionMapper.selectOne(any())).thenReturn(null);

        var prob = new CodeforcesProblemDto();
        prob.setContestId(123L);
        prob.setIndex("A");

        var sub = new CodeforcesSubmissionDto();
        sub.setId(100L);
        sub.setCreationTimeSeconds(1700000000L);
        sub.setVerdict("OK");
        sub.setProgrammingLanguage("C++");
        sub.setProblem(prob);

        when(cfClient.fetchSubmissions("test_handle", 1, 500)).thenReturn(List.of(sub));

        var result = service.syncMyAccount(normalUser);
        assertEquals("SUCCESS", result.getSyncStatus());
        assertEquals(1, result.getFetchedCount());
        assertEquals(1, result.getInsertedCount());
        assertEquals(1, result.getAcceptedCount());
        assertEquals(1, result.getNewAcceptedProblemCount());

        verify(submissionMapper).insert(ArgumentMatchers.<OjSubmission>any());
        verify(firstAcMapper).insert(ArgumentMatchers.<FirstAc>any());
        verify(submissionMapper).updateById(ArgumentMatchers.<OjSubmission>any());
        verify(accountMapper).updateById(acc);
        assertEquals(1, acc.getLastSyncSuccess());
        assertEquals("100", acc.getLastSyncCursor());
        verify(taskLogMapper).insert(any(SyncTaskLog.class));
    }

    @Test
    void sync_idempotent_filterAlreadySynced() {
        OjAccount acc = verifiedAccount();
        when(accountMapper.selectOne(any())).thenReturn(acc);

        OjSubmission existing = new OjSubmission();
        existing.setRemoteSubmissionId("200");
        when(submissionMapper.selectOne(any())).thenReturn(existing);

        var prob = new CodeforcesProblemDto();
        prob.setContestId(123L);
        prob.setIndex("B");

        var sub = new CodeforcesSubmissionDto();
        sub.setId(150L);
        sub.setVerdict("OK");
        sub.setProblem(prob);

        when(cfClient.fetchSubmissions("test_handle", 1, 500)).thenReturn(List.of(sub));

        var result = service.syncMyAccount(normalUser);
        assertEquals(0, result.getInsertedCount());
        verify(submissionMapper, never()).insert(ArgumentMatchers.<OjSubmission>any());
    }

    @Test
    void sync_nonAcSubmission_doesNotCountAsNewAc() {
        OjAccount acc = verifiedAccount();
        when(accountMapper.selectOne(any())).thenReturn(acc);
        when(submissionMapper.selectOne(any())).thenReturn(null);

        var prob = new CodeforcesProblemDto();
        prob.setContestId(123L);
        prob.setIndex("A");

        var sub = new CodeforcesSubmissionDto();
        sub.setId(101L);
        sub.setVerdict("WRONG_ANSWER");
        sub.setProblem(prob);

        when(cfClient.fetchSubmissions("test_handle", 1, 500)).thenReturn(List.of(sub));

        var result = service.syncMyAccount(normalUser);
        assertEquals(1, result.getInsertedCount());
        assertEquals(0, result.getAcceptedCount());
        assertEquals(0, result.getNewAcceptedProblemCount());
        verify(firstAcMapper, never()).insert(ArgumentMatchers.<FirstAc>any());
    }

    @Test
    void sync_duplicateKeyException_isSkipped() {
        OjAccount acc = verifiedAccount();
        when(accountMapper.selectOne(any())).thenReturn(acc);
        when(submissionMapper.selectOne(any())).thenReturn(null);

        var prob = new CodeforcesProblemDto();
        prob.setContestId(123L);
        prob.setIndex("A");

        var sub = new CodeforcesSubmissionDto();
        sub.setId(200L);
        sub.setVerdict("OK");
        sub.setProblem(prob);

        when(cfClient.fetchSubmissions("test_handle", 1, 500)).thenReturn(List.of(sub));
        doThrow(new DuplicateKeyException("dup")).when(submissionMapper).insert(ArgumentMatchers.<OjSubmission>any());

        var result = service.syncMyAccount(normalUser);
        assertEquals(0, result.getInsertedCount());
        assertEquals(0, result.getAcceptedCount());
        assertEquals(0, result.getNewAcceptedProblemCount());
        verify(firstAcMapper, never()).insert(ArgumentMatchers.<FirstAc>any());
    }

    @Test
    void sync_alreadyFirstAc_doesNotIncrementNewAcCount() {
        OjAccount acc = verifiedAccount();
        when(accountMapper.selectOne(any())).thenReturn(acc);
        when(submissionMapper.selectOne(any())).thenReturn(null);

        var prob = new CodeforcesProblemDto();
        prob.setContestId(123L);
        prob.setIndex("A");

        var sub = new CodeforcesSubmissionDto();
        sub.setId(300L);
        sub.setVerdict("OK");
        sub.setProblem(prob);

        when(cfClient.fetchSubmissions("test_handle", 1, 500)).thenReturn(List.of(sub));
        doThrow(new DuplicateKeyException("dup")).when(firstAcMapper).insert(ArgumentMatchers.<FirstAc>any());

        var result = service.syncMyAccount(normalUser);
        assertEquals(1, result.getInsertedCount());
        assertEquals(1, result.getAcceptedCount());
        assertEquals(0, result.getNewAcceptedProblemCount());
        verify(submissionMapper, never()).updateById(ArgumentMatchers.<OjSubmission>any());
    }

    @Test
    void sync_problemsetFallback_usesProblemsetName() {
        OjAccount acc = verifiedAccount();
        when(accountMapper.selectOne(any())).thenReturn(acc);
        when(submissionMapper.selectOne(any())).thenReturn(null);

        var prob = new CodeforcesProblemDto();
        prob.setContestId(null);
        prob.setProblemsetName("acmsguru");
        prob.setIndex("123");

        var sub = new CodeforcesSubmissionDto();
        sub.setId(400L);
        sub.setVerdict("OK");
        sub.setProblem(prob);

        when(cfClient.fetchSubmissions("test_handle", 1, 500)).thenReturn(List.of(sub));

        var result = service.syncMyAccount(normalUser);
        assertEquals(1, result.getInsertedCount());

        ArgumentCaptor<OjSubmission> captor = ArgumentCaptor.forClass(OjSubmission.class);
        verify(submissionMapper).insert(captor.capture());
        assertEquals("acmsguru123", captor.getValue().getExternalProblemKey());
    }

    @Test
    void sync_nullProblem_skipped() {
        OjAccount acc = verifiedAccount();
        when(accountMapper.selectOne(any())).thenReturn(acc);
        when(submissionMapper.selectOne(any())).thenReturn(null);

        var sub = new CodeforcesSubmissionDto();
        sub.setId(500L);
        sub.setVerdict("OK");
        sub.setProblem(null);

        when(cfClient.fetchSubmissions("test_handle", 1, 500)).thenReturn(List.of(sub));

        var result = service.syncMyAccount(normalUser);
        assertEquals(0, result.getInsertedCount());
    }

    @Test
    void sync_nullSubId_skipped() {
        OjAccount acc = verifiedAccount();
        when(accountMapper.selectOne(any())).thenReturn(acc);
        when(submissionMapper.selectOne(any())).thenReturn(null);

        var prob = new CodeforcesProblemDto();
        prob.setContestId(123L);
        prob.setIndex("A");

        var sub = new CodeforcesSubmissionDto();
        sub.setId(null);
        sub.setVerdict("OK");
        sub.setProblem(prob);

        when(cfClient.fetchSubmissions("test_handle", 1, 500)).thenReturn(List.of(sub));

        var result = service.syncMyAccount(normalUser);
        assertEquals(0, result.getInsertedCount());
    }

    @Test
    void sync_subIdZeroOrNegative_skipped() {
        OjAccount acc = verifiedAccount();
        when(accountMapper.selectOne(any())).thenReturn(acc);
        when(submissionMapper.selectOne(any())).thenReturn(null);

        var prob = new CodeforcesProblemDto();
        prob.setContestId(123L);
        prob.setIndex("A");

        var sub = new CodeforcesSubmissionDto();
        sub.setId(0L);
        sub.setVerdict("OK");
        sub.setProblem(prob);

        when(cfClient.fetchSubmissions("test_handle", 1, 500)).thenReturn(List.of(sub));

        var result = service.syncMyAccount(normalUser);
        assertEquals(0, result.getInsertedCount());
    }

    @Test
    void sync_unexpectedException_throws500() {
        OjAccount acc = verifiedAccount();
        when(accountMapper.selectOne(any())).thenReturn(acc);
        when(submissionMapper.selectOne(any())).thenReturn(null);
        when(cfClient.fetchSubmissions("test_handle", 1, 500)).thenThrow(new RuntimeException("boom"));

        var e = assertThrows(BusinessException.class, () -> service.syncMyAccount(normalUser));
        assertEquals(500, e.getCode());
    }

    @Test
    void sync_multipleNewSubmissions_correctCounts() {
        OjAccount acc = verifiedAccount();
        when(accountMapper.selectOne(any())).thenReturn(acc);
        when(submissionMapper.selectOne(any())).thenReturn(null);

        var probA = new CodeforcesProblemDto();
        probA.setContestId(1L);
        probA.setIndex("A");
        var probB = new CodeforcesProblemDto();
        probB.setContestId(1L);
        probB.setIndex("B");

        var s1 = new CodeforcesSubmissionDto();
        s1.setId(1L);
        s1.setVerdict("OK");
        s1.setProblem(probA);
        var s2 = new CodeforcesSubmissionDto();
        s2.setId(2L);
        s2.setVerdict("OK");
        s2.setProblem(probB);
        var s3 = new CodeforcesSubmissionDto();
        s3.setId(3L);
        s3.setVerdict("WRONG_ANSWER");
        s3.setProblem(probA);

        when(cfClient.fetchSubmissions("test_handle", 1, 500)).thenReturn(List.of(s1, s2, s3));

        var result = service.syncMyAccount(normalUser);
        assertEquals(3, result.getFetchedCount());
        assertEquals(3, result.getInsertedCount());
        assertEquals(2, result.getAcceptedCount());
        assertEquals(2, result.getNewAcceptedProblemCount());
        verify(firstAcMapper, times(2)).insert(ArgumentMatchers.<FirstAc>any());
    }

    @Test
    void bind_handleWithWhitespace_trimmed() {
        when(accountMapper.selectOne(any())).thenReturn(null, null);
        service.bind("  myHandle  ", normalUser);
        ArgumentCaptor<OjAccount> captor = ArgumentCaptor.forClass(OjAccount.class);
        verify(accountMapper).insert(captor.capture());
        assertEquals("myHandle", captor.getValue().getExternalUserId());
        assertEquals("myHandle", captor.getValue().getDisplayName());
    }

    @Test
    void sync_concurrentFirstAc_secondInsertFails_atomic() {
        OjAccount acc = verifiedAccount();
        when(accountMapper.selectOne(any())).thenReturn(acc);
        when(submissionMapper.selectOne(any())).thenReturn(null);

        var prob = new CodeforcesProblemDto();
        prob.setContestId(123L);
        prob.setIndex("A");

        var sub = new CodeforcesSubmissionDto();
        sub.setId(600L);
        sub.setVerdict("OK");
        sub.setProblem(prob);

        when(cfClient.fetchSubmissions("test_handle", 1, 500)).thenReturn(List.of(sub));
        // firstAcMapper insert throws DKE → concurrent already claimed this AC
        doThrow(new DuplicateKeyException("dup")).when(firstAcMapper).insert(ArgumentMatchers.<FirstAc>any());

        var result = service.syncMyAccount(normalUser);
        assertEquals(1, result.getInsertedCount());
        assertEquals(1, result.getAcceptedCount());
        assertEquals(0, result.getNewAcceptedProblemCount());
        // is_first_ac should NOT be set
        verify(submissionMapper, never()).updateById(ArgumentMatchers.<OjSubmission>any());
    }

    // ---- cooldown tests ----

    @Test
    void sync_cooldownActive_returnsCooldownStatus() {
        OjAccount acc = verifiedAccount();
        acc.setLastSyncTime(LocalDateTime.now().minusMinutes(30));
        when(accountMapper.selectOne(any())).thenReturn(acc);

        var result = service.syncMyAccount(normalUser);
        assertEquals("COOLDOWN", result.getSyncStatus());
        assertTrue(result.getRemainingCooldownSeconds() > 0);
        assertNotNull(result.getNextAllowedSyncTime());
        verify(cfClient, never()).fetchSubmissions(anyString(), anyInt(), anyInt());
    }

    @Test
    void sync_cooldownExpired_proceedsNormally() {
        OjAccount acc = verifiedAccount();
        acc.setLastSyncTime(LocalDateTime.now().minusMinutes(90));
        when(accountMapper.selectOne(any())).thenReturn(acc);
        when(submissionMapper.selectOne(any())).thenReturn(null);
        when(cfClient.fetchSubmissions("test_handle", 1, 500)).thenReturn(Collections.emptyList());

        var result = service.syncMyAccount(normalUser);
        assertEquals("SUCCESS", result.getSyncStatus());
        verify(cfClient).fetchSubmissions("test_handle", 1, 500);
    }

    @Test
    void sync_failedSync_noCooldown() {
        OjAccount acc = verifiedAccount();
        acc.setLastSyncTime(LocalDateTime.now().minusMinutes(30));
        acc.setLastSyncSuccess(0);
        when(accountMapper.selectOne(any())).thenReturn(acc);
        when(submissionMapper.selectOne(any())).thenReturn(null);
        when(cfClient.fetchSubmissions("test_handle", 1, 500)).thenReturn(Collections.emptyList());

        var result = service.syncMyAccount(normalUser);
        assertEquals("SUCCESS", result.getSyncStatus());
    }

    @Test
    void sync_noPreviousSync_noCooldown() {
        OjAccount acc = verifiedAccount();
        acc.setLastSyncTime(null);
        acc.setLastSyncSuccess(null);
        when(accountMapper.selectOne(any())).thenReturn(acc);
        when(submissionMapper.selectOne(any())).thenReturn(null);
        when(cfClient.fetchSubmissions("test_handle", 1, 500)).thenReturn(Collections.emptyList());

        var result = service.syncMyAccount(normalUser);
        assertEquals("SUCCESS", result.getSyncStatus());
    }

    @Test
    void sync_concurrentRequest_throws429() {
        OjAccount acc = verifiedAccount();
        acc.setLastSyncTime(null);
        when(accountMapper.selectOne(any())).thenReturn(acc);
        // Pre-mark account as syncing
        service.syncingAccounts.add(ACCOUNT_ID);

        var e = assertThrows(BusinessException.class, () -> service.syncMyAccount(normalUser));
        assertEquals(429, e.getCode());
        service.syncingAccounts.remove(ACCOUNT_ID);
    }

    // ---- syncAccountById tests ----

    @Test
    void syncById_accountNotFound_throws404() {
        when(accountMapper.selectById(999L)).thenReturn(null);
        var e = assertThrows(BusinessException.class, () -> service.syncAccountById(999L, "MANUAL"));
        assertEquals(404, e.getCode());
    }

    @Test
    void syncById_notVerified_throws409() {
        OjAccount acc = verifiedAccount();
        acc.setVerifyStatus(0);
        when(accountMapper.selectById(ACCOUNT_ID)).thenReturn(acc);
        var e = assertThrows(BusinessException.class, () -> service.syncAccountById(ACCOUNT_ID, "SCHEDULED"));
        assertEquals(409, e.getCode());
    }

    @Test
    void syncById_notEnabled_throws409() {
        OjAccount acc = verifiedAccount();
        acc.setSyncEnabled(0);
        when(accountMapper.selectById(ACCOUNT_ID)).thenReturn(acc);
        var e = assertThrows(BusinessException.class, () -> service.syncAccountById(ACCOUNT_ID, "SCHEDULED"));
        assertEquals(409, e.getCode());
    }

    @Test
    void syncById_scheduledTrigger_succeeds() {
        OjAccount acc = verifiedAccount();
        acc.setLastSyncTime(null);
        when(accountMapper.selectById(ACCOUNT_ID)).thenReturn(acc);
        when(submissionMapper.selectOne(any())).thenReturn(null);
        when(cfClient.fetchSubmissions("test_handle", 1, 500)).thenReturn(Collections.emptyList());

        var result = service.syncAccountById(ACCOUNT_ID, "SCHEDULED");
        assertEquals("SUCCESS", result.getSyncStatus());
        // Verify SyncTaskLog was written with SCHEDULED trigger
        var captor = ArgumentCaptor.forClass(SyncTaskLog.class);
        verify(taskLogMapper).insert(captor.capture());
        assertEquals("SCHEDULED", captor.getValue().getTriggerType());
    }
}
