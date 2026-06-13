package com.sherry.supervision.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sherry.supervision.dto.ProgressFeedbackRequest;
import com.sherry.supervision.entity.ProgressFeedback;
import com.sherry.supervision.mapper.ProgressFeedbackMapper;
import com.sherry.supervision.service.ProgressFeedbackService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ProgressFeedbackServiceImpl implements ProgressFeedbackService {

    private final ProgressFeedbackMapper progressFeedbackMapper;
    private final ObjectMapper objectMapper;

    public ProgressFeedbackServiceImpl(
            ProgressFeedbackMapper progressFeedbackMapper, ObjectMapper objectMapper) {
        this.progressFeedbackMapper = progressFeedbackMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<ProgressFeedback> list(UUID itemId) {
        LambdaQueryWrapper<ProgressFeedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(itemId != null, ProgressFeedback::getItemId, itemId);
        wrapper.orderByDesc(ProgressFeedback::getFeedbackAt, ProgressFeedback::getCreatedAt);
        return progressFeedbackMapper.selectList(wrapper);
    }

    @Override
    public ProgressFeedback create(ProgressFeedbackRequest request) {
        ProgressFeedback feedback = new ProgressFeedback();
        feedback.setId(UUID.randomUUID());
        feedback.setItemId(request.itemId());
        feedback.setFeedbackUserId(defaultValue(request.feedbackUserId(), "ai_assistant"));
        feedback.setFeedbackUserName(defaultValue(request.feedbackUserName(), "AI 助手"));
        feedback.setProgressPercent(request.progressPercent() == null ? 0 : request.progressPercent());
        feedback.setContent(request.content());
        feedback.setRiskNote(request.riskNote());
        feedback.setAttachmentIds(objectMapper.createArrayNode());
        feedback.setFeedbackAt(OffsetDateTime.now());
        feedback.setCreatedAt(OffsetDateTime.now());
        feedback.setUpdatedAt(OffsetDateTime.now());
        progressFeedbackMapper.insert(feedback);
        return feedback;
    }

    private String defaultValue(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }
}
