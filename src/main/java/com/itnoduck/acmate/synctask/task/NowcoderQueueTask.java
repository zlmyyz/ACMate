package com.itnoduck.acmate.synctask.task;
import com.itnoduck.acmate.oj.service.NowcoderProfileSyncService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix="acmate.nowcoder.queue",name="enabled",havingValue="true",matchIfMissing=true)
public class NowcoderQueueTask {
    private final NowcoderProfileSyncService service;
    public NowcoderQueueTask(NowcoderProfileSyncService service){this.service=service;}
    @Scheduled(fixedDelay=2000,initialDelay=10000) public void syncPage(){service.processSync();}
    @Scheduled(fixedDelay=2000,initialDelay=12000) public void importProblem(){service.processImport();}
    @Scheduled(fixedDelay=60000,initialDelay=15000) public void enqueueAccounts(){service.enqueueEligible();}
}
