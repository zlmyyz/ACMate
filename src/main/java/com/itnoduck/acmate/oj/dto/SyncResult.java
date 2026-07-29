package com.itnoduck.acmate.oj.dto;

public class SyncResult {

    private Long accountId;
    private String handle;
    private int fetchedCount;
    private int insertedCount;
    private int acceptedCount;
    private int newAcceptedProblemCount;
    private String lastSyncTime;
    private String syncStatus;
    private long remainingCooldownSeconds;
    private String nextAllowedSyncTime;

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long v) { accountId = v; }
    public String getHandle() { return handle; }
    public void setHandle(String v) { handle = v; }
    public int getFetchedCount() { return fetchedCount; }
    public void setFetchedCount(int v) { fetchedCount = v; }
    public int getInsertedCount() { return insertedCount; }
    public void setInsertedCount(int v) { insertedCount = v; }
    public int getAcceptedCount() { return acceptedCount; }
    public void setAcceptedCount(int v) { acceptedCount = v; }
    public int getNewAcceptedProblemCount() { return newAcceptedProblemCount; }
    public void setNewAcceptedProblemCount(int v) { newAcceptedProblemCount = v; }
    public String getLastSyncTime() { return lastSyncTime; }
    public void setLastSyncTime(String v) { lastSyncTime = v; }
    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String v) { syncStatus = v; }
    public long getRemainingCooldownSeconds() { return remainingCooldownSeconds; }
    public void setRemainingCooldownSeconds(long v) { remainingCooldownSeconds = v; }
    public String getNextAllowedSyncTime() { return nextAllowedSyncTime; }
    public void setNextAllowedSyncTime(String v) { nextAllowedSyncTime = v; }
}
