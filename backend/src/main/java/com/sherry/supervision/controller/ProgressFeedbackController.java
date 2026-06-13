package com.sherry.supervision.controller;

import com.sherry.supervision.common.ApiResponse;
import com.sherry.supervision.dto.ProgressFeedbackRequest;
import com.sherry.supervision.entity.ProgressFeedback;
import com.sherry.supervision.service.ProgressFeedbackService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/progress-feedbacks")
public class ProgressFeedbackController {

    private final ProgressFeedbackService progressFeedbackService;

    public ProgressFeedbackController(ProgressFeedbackService progressFeedbackService) {
        this.progressFeedbackService = progressFeedbackService;
    }

    @GetMapping
    public ApiResponse<Map<String, List<ProgressFeedback>>> list(
            @RequestParam(name = "item_id", required = false) UUID itemId) {
        return ApiResponse.ok(Map.of("items", progressFeedbackService.list(itemId)));
    }

    @PostMapping
    public ApiResponse<ProgressFeedback> create(@Valid @RequestBody ProgressFeedbackRequest request) {
        return ApiResponse.ok(progressFeedbackService.create(request));
    }
}
