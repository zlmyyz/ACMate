package com.itnoduck.acmate.export.service;

import com.itnoduck.acmate.security.AuthenticatedUser;

public interface DataExportService {
    String exportProblems(AuthenticatedUser user);
    String exportLeaderboard(String period, AuthenticatedUser user);
}
