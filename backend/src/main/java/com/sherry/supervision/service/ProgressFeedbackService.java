package com.sherry.supervision.service;

import com.sherry.supervision.dto.ProgressFeedbackRequest;
import com.sherry.supervision.entity.ProgressFeedback;
import java.util.List;
import java.util.UUID;

public interface ProgressFeedbackService {

    List<ProgressFeedback> list(UUID itemId);

    ProgressFeedback create(ProgressFeedbackRequest request);
}
