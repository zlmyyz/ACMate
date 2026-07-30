package com.itnoduck.acmate.training.service;

import com.itnoduck.acmate.training.dto.MemberProgressResponse;

public interface TrainingPlanProgressService {

    void updateStatus(Long planId, Long problemId, String status, Long userId);

    void updateNote(Long planId, Long problemId, String note, Long userId);

    MemberProgressResponse getMemberProgress(Long planId, Long targetUserId, Long currentUserId);
}
