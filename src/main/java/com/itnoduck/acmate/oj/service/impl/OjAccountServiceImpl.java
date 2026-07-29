package com.itnoduck.acmate.oj.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itnoduck.acmate.auditlog.service.AuditLogService;
import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.oj.client.CodeforcesApiClient;
import com.itnoduck.acmate.oj.client.CodeforcesProblemDto;
import com.itnoduck.acmate.oj.client.CodeforcesSubmissionDto;
import com.itnoduck.acmate.oj.dto.SyncResult;
import com.itnoduck.acmate.oj.entity.FirstAc;
import com.itnoduck.acmate.oj.entity.OjAccount;
import com.itnoduck.acmate.oj.entity.OjSubmission;
import com.itnoduck.acmate.oj.mapper.FirstAcMapper;
import com.itnoduck.acmate.oj.mapper.OjAccountMapper;
import com.itnoduck.acmate.oj.mapper.OjSubmissionMapper;
import com.itnoduck.acmate.oj.service.OjAccountService;
import com.itnoduck.acmate.security.AuthenticatedUser;
import com.itnoduck.acmate.synctask.entity.SyncTaskLog;
import com.itnoduck.acmate.synctask.mapper.SyncTaskLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class OjAccountServiceImpl implements OjAccountService {

    private static final Logger log = LoggerFactory.getLogger(OjAccountServiceImpl.class);
    private static final int CF_FETCH_COUNT = 500;
    private static final long SYNC_COOLDOWN_SECONDS = 3600;

    final Set<Long> syncingAccounts = ConcurrentHashMap.newKeySet();

    private final OjAccountMapper accountMapper;
    private final OjSubmissionMapper submissionMapper;
    private final FirstAcMapper firstAcMapper;
    private final SyncTaskLogMapper taskLogMapper;
    private final AuditLogService auditLogService;
    private final CodeforcesApiClient cfClient;

    public OjAccountServiceImpl(OjAccountMapper accountMapper,
                                OjSubmissionMapper submissionMapper,
                                FirstAcMapper firstAcMapper,
                                SyncTaskLogMapper taskLogMapper,
                                AuditLogService auditLogService,
                                CodeforcesApiClient cfClient) {
        this.accountMapper = accountMapper;
        this.submissionMapper = submissionMapper;
        this.firstAcMapper = firstAcMapper;
        this.taskLogMapper = taskLogMapper;
        this.auditLogService = auditLogService;
        this.cfClient = cfClient;
    }

    @Override
    public Map<String, Object> getMyAccount(AuthenticatedUser user) {
        var acc = accountMapper.selectOne(new LambdaQueryWrapper<OjAccount>()
                .eq(OjAccount::getUserId, user.getId()));
        if (acc == null) return Map.of("hasAccount", false);
        var result = new java.util.LinkedHashMap<String, Object>();
        result.put("hasAccount", true);
        result.put("id", acc.getId());
        result.put("platform", acc.getPlatform());
        result.put("externalUserId", acc.getExternalUserId());
        result.put("displayName", acc.getDisplayName());
        result.put("verifyStatus", acc.getVerifyStatus());
        result.put("syncEnabled", acc.getSyncEnabled());
        result.put("lastSyncTime", acc.getLastSyncTime() != null ? acc.getLastSyncTime().toString() : null);
        result.put("lastSyncSuccess", acc.getLastSyncSuccess());
        return result;
    }

    @Override
    @Transactional
    public void bind(String handle, AuthenticatedUser user) {
        if (handle == null || handle.isBlank()) throw new BusinessException(400, "Codeforces handle 不能为空");

        var existing = accountMapper.selectOne(new LambdaQueryWrapper<OjAccount>()
                .eq(OjAccount::getUserId, user.getId()));
        if (existing != null) throw new BusinessException(409, "已绑定过 Codeforces 账号，请先解绑");

        var conflict = accountMapper.selectOne(new LambdaQueryWrapper<OjAccount>()
                .eq(OjAccount::getExternalUserId, handle.trim()));
        if (conflict != null) throw new BusinessException(409, "该 Codeforces 账号已被其他用户绑定");

        OjAccount acc = new OjAccount();
        acc.setUserId(user.getId());
        acc.setPlatform("CODEFORCES");
        acc.setExternalUserId(handle.trim());
        acc.setDisplayName(handle.trim());
        acc.setVerifyStatus(0);
        acc.setSyncEnabled(1);
        accountMapper.insert(acc);
    }

    @Override
    @Transactional
    public void unbind(AuthenticatedUser user) {
        accountMapper.delete(new LambdaQueryWrapper<OjAccount>().eq(OjAccount::getUserId, user.getId()));
    }

    @Override
    public List<Map<String, Object>> getPendingAccounts(AuthenticatedUser user) {
        if (!user.isAdmin()) throw new BusinessException(403, "无权访问");
        var accounts = accountMapper.selectList(new LambdaQueryWrapper<OjAccount>()
                .eq(OjAccount::getVerifyStatus, 0)
                .orderByAsc(OjAccount::getCreateTime));
        List<Map<String, Object>> result = new ArrayList<>();
        for (var a : accounts) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("userId", a.getUserId());
            m.put("platform", a.getPlatform());
            m.put("externalUserId", a.getExternalUserId());
            m.put("displayName", a.getDisplayName());
            m.put("verifyStatus", a.getVerifyStatus());
            m.put("syncEnabled", a.getSyncEnabled());
            m.put("lastSyncTime", a.getLastSyncTime() != null ? a.getLastSyncTime().toString() : null);
            result.add(m);
        }
        return result;
    }

    @Override
    @Transactional
    public void verify(Long id, int status, AuthenticatedUser user) {
        if (!user.isAdmin()) throw new BusinessException(403, "无权操作");
        var acc = accountMapper.selectById(id);
        if (acc == null) throw new BusinessException(404, "账号不存在");
        String before = String.valueOf(acc.getVerifyStatus());
        acc.setVerifyStatus(status);
        accountMapper.updateById(acc);
        String actionType = status == 1 ? "OJ_ACCOUNT_VERIFIED" : "OJ_ACCOUNT_REJECTED";
        auditLogService.log(user.getId(), actionType, "OJ_ACCOUNT", id, status == 1 ? "approved" : "rejected", before, String.valueOf(status));
    }

    @Override
    public SyncResult syncMyAccount(AuthenticatedUser user) {
        // Step 1: get verified CF account
        var acc = accountMapper.selectOne(new LambdaQueryWrapper<OjAccount>()
                .eq(OjAccount::getUserId, user.getId()));
        if (acc == null) throw new BusinessException(404, "未绑定 Codeforces 账号");
        if (!"CODEFORCES".equals(acc.getPlatform())) throw new BusinessException(400, "仅支持 Codeforces 平台同步");
        if (acc.getVerifyStatus() == null || acc.getVerifyStatus() != 1)
            throw new BusinessException(409, "Codeforces 账号未通过审核，审核通过后才能同步");

        // Cooldown check (only for manual sync)
        if (isCooldownActive(acc)) {
            long elapsed = Duration.between(acc.getLastSyncTime(), LocalDateTime.now()).getSeconds();
            long remaining = SYNC_COOLDOWN_SECONDS - elapsed;
            SyncResult cr = new SyncResult();
            cr.setSyncStatus("COOLDOWN");
            cr.setRemainingCooldownSeconds(Math.max(0, remaining));
            cr.setNextAllowedSyncTime(acc.getLastSyncTime().plusSeconds(SYNC_COOLDOWN_SECONDS).toString());
            return cr;
        }

        // Concurrent request protection
        if (!syncingAccounts.add(acc.getId())) {
            throw new BusinessException(429, "该账号正在同步中，请稍后再试");
        }
        try {
            return doSync(acc, "MANUAL");
        } finally {
            syncingAccounts.remove(acc.getId());
        }
    }

    @Override
    public SyncResult syncAccountById(Long accountId, String triggerType) {
        var acc = accountMapper.selectById(accountId);
        if (acc == null) throw new BusinessException(404, "账号不存在");
        if (!"CODEFORCES".equals(acc.getPlatform())) throw new BusinessException(400, "仅支持 Codeforces 平台同步");
        if (acc.getVerifyStatus() == null || acc.getVerifyStatus() != 1)
            throw new BusinessException(409, "账号未审核通过");
        if (acc.getSyncEnabled() == null || acc.getSyncEnabled() != 1)
            throw new BusinessException(409, "账号同步未启用");

        if (!syncingAccounts.add(acc.getId())) {
            throw new BusinessException(429, "该账号正在同步中，请稍后再试");
        }
        try {
            return doSync(acc, triggerType);
        } finally {
            syncingAccounts.remove(acc.getId());
        }
    }

    private boolean isCooldownActive(OjAccount acc) {
        if (acc.getLastSyncSuccess() == null || acc.getLastSyncSuccess() != 1) return false;
        if (acc.getLastSyncTime() == null) return false;
        return Duration.between(acc.getLastSyncTime(), LocalDateTime.now()).getSeconds() < SYNC_COOLDOWN_SECONDS;
    }

    private SyncResult doSync(OjAccount acc, String triggerType) {
        String handle = acc.getExternalUserId();

        // Step 1: get sync cursor
        Long maxExistingSubId = getMaxRemoteSubmissionId(acc.getId());

        // Step 2: fetch from CF API
        List<CodeforcesSubmissionDto> fetched;
        try {
            fetched = cfClient.fetchSubmissions(handle, 1, CF_FETCH_COUNT);
        } catch (BusinessException e) {
            recordFailedSync(acc, e.getMessage(), triggerType);
            throw e;
        } catch (Exception e) {
            log.warn("Unexpected sync error for accountId={} handle={}: {}", acc.getId(), handle, e.getMessage());
            recordFailedSync(acc, "同步异常，请稍后重试", triggerType);
            throw new BusinessException(500, "同步异常，请稍后重试");
        }

        // Step 3: normalize, filter new, and save
        List<CodeforcesSubmissionDto> newSubs = fetched.stream()
                .filter(s -> s.getId() != null && s.getId() > 0)
                .filter(s -> maxExistingSubId == null || s.getId() > maxExistingSubId)
                .collect(Collectors.toList());

        int acceptedCount = 0, insertedCount = 0, newAcProblemCount = 0;
        Long maxNewSubId = maxExistingSubId;

        if (!newSubs.isEmpty()) {
            var result = saveSubmissions(newSubs, acc);
            insertedCount = result.insertedCount;
            acceptedCount = result.acceptedCount;
            newAcProblemCount = result.newAcProblemCount;
            maxNewSubId = result.maxSubId;
        }

        // Step 4: update account sync state
        updateAccountSyncState(acc, maxNewSubId != null ? String.valueOf(maxNewSubId) : null);

        // Step 5: write sync task log
        SyncTaskLog taskLog = new SyncTaskLog();
        taskLog.setOjAccountId(acc.getId());
        taskLog.setPlatform("CODEFORCES");
        taskLog.setTriggerType(triggerType);
        taskLog.setTaskStatus("SUCCESS");
        taskLog.setCursorBefore(maxExistingSubId != null ? String.valueOf(maxExistingSubId) : null);
        taskLog.setCursorAfter(maxNewSubId != null ? String.valueOf(maxNewSubId) : null);
        taskLog.setFetchedCount(fetched.size());
        taskLog.setInsertedCount(insertedCount);
        taskLog.setFirstAcCount(newAcProblemCount);
        taskLog.setStartTime(LocalDateTime.now().minusSeconds(30));
        taskLog.setEndTime(LocalDateTime.now());
        taskLogMapper.insert(taskLog);

        SyncResult result = new SyncResult();
        result.setAccountId(acc.getId());
        result.setHandle(handle);
        result.setFetchedCount(fetched.size());
        result.setInsertedCount(insertedCount);
        result.setAcceptedCount(acceptedCount);
        result.setNewAcceptedProblemCount(newAcProblemCount);
        result.setLastSyncTime(acc.getLastSyncTime() != null ? acc.getLastSyncTime().toString() : null);
        result.setSyncStatus("SUCCESS");
        return result;
    }

    // ---------- helpers ----------

    private Long getMaxRemoteSubmissionId(Long accountId) {
        var wrapper = new LambdaQueryWrapper<OjSubmission>()
                .eq(OjSubmission::getOjAccountId, accountId)
                .orderByDesc(OjSubmission::getRemoteSubmissionId)
                .last("LIMIT 1");
        var latest = submissionMapper.selectOne(wrapper);
        if (latest == null) return null;
        try {
            return Long.parseLong(latest.getRemoteSubmissionId());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Transactional
    public SaveResult saveSubmissions(List<CodeforcesSubmissionDto> raw, OjAccount account) {
        int inserted = 0;
        int accepted = 0;
        int newAc = 0;
        Long maxSubId = null;

        for (CodeforcesSubmissionDto s : raw) {
            if (s.getId() == null) continue;

            Long subId = s.getId();
            if (maxSubId == null || subId > maxSubId) maxSubId = subId;

            String problemKey = buildProblemKey(s.getProblem());
            if (problemKey == null) continue;

            LocalDateTime submittedTime = s.getCreationTimeSeconds() != null
                    ? LocalDateTime.ofInstant(Instant.ofEpochSecond(s.getCreationTimeSeconds()), ZoneId.systemDefault())
                    : LocalDateTime.now();

            String verdict = s.getVerdict() != null ? s.getVerdict() : "UNKNOWN";
            boolean isOk = "OK".equals(verdict);
            boolean firstAc = false;

            OjSubmission sub = new OjSubmission();
            sub.setOjAccountId(account.getId());
            sub.setUserId(account.getUserId());
            sub.setPlatform("CODEFORCES");
            sub.setRemoteSubmissionId(String.valueOf(subId));
            sub.setProblemId(null);
            sub.setExternalProblemKey(problemKey);
            sub.setVerdict(verdict);
            sub.setLanguage(s.getProgrammingLanguage());
            sub.setSubmittedTime(submittedTime);
            sub.setIsFirstAc(0);

            try {
                submissionMapper.insert(sub);
                inserted++;
                if (isOk) accepted++;
            } catch (DuplicateKeyException e) {
                continue; // already synced, skip first-AC check
            }

            if (isOk) {
                FirstAc ac = new FirstAc();
                ac.setUserId(account.getUserId());
                ac.setPlatform("CODEFORCES");
                ac.setExternalProblemKey(problemKey);
                ac.setSubmissionId(sub.getId());
                try {
                    firstAcMapper.insert(ac);
                    firstAc = true;
                    newAc++;
                } catch (DuplicateKeyException e) {
                    // not first AC — DB unique constraint guarantees atomicity
                }
                if (firstAc) {
                    sub.setIsFirstAc(1);
                    submissionMapper.updateById(sub);
                }
            }
        }

        SaveResult r = new SaveResult();
        r.insertedCount = inserted;
        r.acceptedCount = accepted;
        r.newAcProblemCount = newAc;
        r.maxSubId = maxSubId;
        return r;
    }

    private String buildProblemKey(CodeforcesProblemDto problem) {
        if (problem == null) return null;
        if (problem.getContestId() != null && problem.getIndex() != null) {
            return problem.getContestId() + problem.getIndex();
        }
        if (problem.getProblemsetName() != null && problem.getIndex() != null) {
            return problem.getProblemsetName().replaceAll("[^a-zA-Z0-9]", "") + problem.getIndex();
        }
        return null;
    }

    private void updateAccountSyncState(OjAccount acc, String cursor) {
        acc.setLastSyncCursor(cursor);
        acc.setLastSyncTime(LocalDateTime.now());
        acc.setLastSyncSuccess(1);
        accountMapper.updateById(acc);
    }

    private void recordFailedSync(OjAccount acc, String errorMsg, String triggerType) {
        try {
            acc.setLastSyncTime(LocalDateTime.now());
            acc.setLastSyncSuccess(0);
            accountMapper.updateById(acc);

            SyncTaskLog taskLog = new SyncTaskLog();
            taskLog.setOjAccountId(acc.getId());
            taskLog.setPlatform("CODEFORCES");
            taskLog.setTriggerType(triggerType);
            taskLog.setTaskStatus("FAILED");
            taskLog.setCursorBefore(acc.getLastSyncCursor());
            taskLog.setErrorMessage(errorMsg != null && errorMsg.length() > 1000
                    ? errorMsg.substring(0, 997) + "..." : errorMsg);
            taskLog.setFetchedCount(0);
            taskLog.setInsertedCount(0);
            taskLog.setFirstAcCount(0);
            taskLog.setStartTime(LocalDateTime.now().minusSeconds(5));
            taskLog.setEndTime(LocalDateTime.now());
            taskLogMapper.insert(taskLog);
        } catch (Exception e) {
            log.error("Failed to record sync failure", e);
        }
    }

    static class SaveResult {
        int insertedCount;
        int acceptedCount;
        int newAcProblemCount;
        Long maxSubId;
    }
}
