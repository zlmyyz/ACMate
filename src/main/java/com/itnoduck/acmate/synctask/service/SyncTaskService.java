package com.itnoduck.acmate.synctask.service;

import com.itnoduck.acmate.security.AuthenticatedUser;
import java.util.Map;

public interface SyncTaskService {
    Map<String, Object> listTasks(int page, int size, String taskStatus, AuthenticatedUser user);
    Map<String, Object> triggerSync(AuthenticatedUser user);
}
