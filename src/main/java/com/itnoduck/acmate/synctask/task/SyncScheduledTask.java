package com.itnoduck.acmate.synctask.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itnoduck.acmate.oj.entity.OjAccount;
import com.itnoduck.acmate.oj.mapper.OjAccountMapper;
import com.itnoduck.acmate.oj.service.OjAccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
@ConditionalOnProperty(prefix = "acmate.codeforces.scheduling", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SyncScheduledTask {

    private static final Logger log = LoggerFactory.getLogger(SyncScheduledTask.class);

    private final OjAccountMapper accountMapper;
    private final OjAccountService ojAccountService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public SyncScheduledTask(OjAccountMapper accountMapper, OjAccountService ojAccountService) {
        this.accountMapper = accountMapper;
        this.ojAccountService = ojAccountService;
    }

    @Scheduled(cron = "${acmate.codeforces.scheduling.cron:0 0 * * * *}", zone = "${acmate.codeforces.scheduling.zone:Asia/Shanghai}")
    public void syncAllVerifiedAccounts() {
        if (!running.compareAndSet(false, true)) {
            log.warn("Scheduled sync already running, skipping this execution");
            return;
        }
        try {
            var accounts = accountMapper.selectList(new LambdaQueryWrapper<OjAccount>()
                    .eq(OjAccount::getVerifyStatus, 1)
                    .eq(OjAccount::getSyncEnabled, 1));
            log.info("Scheduled sync started: {} verified accounts to sync", accounts.size());
            for (var acc : accounts) {
                try {
                    ojAccountService.syncAccountById(acc.getId(), "SCHEDULED");
                    log.debug("Scheduled sync OK accountId={} handle={}", acc.getId(), acc.getExternalUserId());
                } catch (Exception e) {
                    log.error("Scheduled sync FAILED accountId={} handle={}: {}", acc.getId(), acc.getExternalUserId(), e.getMessage());
                }
            }
            log.info("Scheduled sync completed: {} accounts processed", accounts.size());
        } finally {
            running.set(false);
        }
    }
}
