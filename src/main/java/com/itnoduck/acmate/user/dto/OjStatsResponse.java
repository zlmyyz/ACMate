package com.itnoduck.acmate.user.dto;

import java.time.LocalDateTime;

public class OjStatsResponse {
    private int solvedCount;
    private int solvedCount30d;
    private int solvedCount7d;
    private LocalDateTime lastAcceptedTime;

    public int getSolvedCount() { return solvedCount; }
    public void setSolvedCount(int v) { solvedCount = v; }
    public int getSolvedCount30d() { return solvedCount30d; }
    public void setSolvedCount30d(int v) { solvedCount30d = v; }
    public int getSolvedCount7d() { return solvedCount7d; }
    public void setSolvedCount7d(int v) { solvedCount7d = v; }
    public LocalDateTime getLastAcceptedTime() { return lastAcceptedTime; }
    public void setLastAcceptedTime(LocalDateTime v) { lastAcceptedTime = v; }
}
