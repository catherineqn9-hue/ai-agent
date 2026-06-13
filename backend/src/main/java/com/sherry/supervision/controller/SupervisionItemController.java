package com.sherry.supervision.controller;

import com.sherry.supervision.common.ApiResponse;
import com.sherry.supervision.dto.StatusUpdateRequest;
import com.sherry.supervision.dto.SupervisionItemDetail;
import com.sherry.supervision.dto.SupervisionItemRequest;
import com.sherry.supervision.entity.SupervisionItem;
import com.sherry.supervision.service.ProgressFeedbackService;
import com.sherry.supervision.service.SupervisionItemService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/supervision-items")
public class SupervisionItemController {

    private final SupervisionItemService supervisionItemService;
    private final ProgressFeedbackService progressFeedbackService;

    public SupervisionItemController(
            SupervisionItemService supervisionItemService,
            ProgressFeedbackService progressFeedbackService) {
        this.supervisionItemService = supervisionItemService;
        this.progressFeedbackService = progressFeedbackService;
    }

    @GetMapping
    public ApiResponse<Map<String, List<SupervisionItem>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(Map.of("items", supervisionItemService.list(status, keyword)));
    }

    @GetMapping("/{id}")
    public ApiResponse<SupervisionItemDetail> detail(@PathVariable UUID id) {
        return ApiResponse.ok(new SupervisionItemDetail(
                supervisionItemService.get(id),
                progressFeedbackService.list(id)));
    }

    @PostMapping
    public ApiResponse<SupervisionItem> create(@Valid @RequestBody SupervisionItemRequest request) {
        return ApiResponse.ok(supervisionItemService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<SupervisionItem> update(
            @PathVariable UUID id, @Valid @RequestBody SupervisionItemRequest request) {
        return ApiResponse.ok(supervisionItemService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<SupervisionItem> updateStatus(
            @PathVariable UUID id, @Valid @RequestBody StatusUpdateRequest request) {
        return ApiResponse.ok(supervisionItemService.updateStatus(id, request.status()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Map<String, String>> delete(@PathVariable UUID id) {
        supervisionItemService.delete(id);
        return ApiResponse.ok(Map.of("deleted", id.toString()));
    }
}
