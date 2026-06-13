package com.sherry.supervision.controller;

import com.sherry.supervision.common.ApiResponse;
import com.sherry.supervision.auth.CurrentUser;
import com.sherry.supervision.dto.AssignmentCandidateResponse;
import com.sherry.supervision.dto.AssignItemRequest;
import com.sherry.supervision.dto.RejectAssignmentRequest;
import com.sherry.supervision.dto.StatusUpdateRequest;
import com.sherry.supervision.dto.SupervisionItemDetail;
import com.sherry.supervision.dto.SupervisionItemRequest;
import com.sherry.supervision.entity.ItemAssignee;
import com.sherry.supervision.entity.SupervisionItem;
import com.sherry.supervision.service.ItemAssignmentService;
import com.sherry.supervision.service.AssignmentRecommendationService;
import com.sherry.supervision.service.ProgressFeedbackService;
import com.sherry.supervision.service.SupervisionItemService;
import jakarta.servlet.http.HttpServletRequest;
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
    private final ItemAssignmentService itemAssignmentService;
    private final AssignmentRecommendationService assignmentRecommendationService;

    public SupervisionItemController(
            SupervisionItemService supervisionItemService,
            ProgressFeedbackService progressFeedbackService,
            ItemAssignmentService itemAssignmentService,
            AssignmentRecommendationService assignmentRecommendationService) {
        this.supervisionItemService = supervisionItemService;
        this.progressFeedbackService = progressFeedbackService;
        this.itemAssignmentService = itemAssignmentService;
        this.assignmentRecommendationService = assignmentRecommendationService;
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
                itemAssignmentService.listByItem(id),
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

    @GetMapping("/{id}/assignees")
    public ApiResponse<Map<String, List<ItemAssignee>>> listAssignees(@PathVariable UUID id) {
        return ApiResponse.ok(Map.of("assignees", itemAssignmentService.listByItem(id)));
    }

    @GetMapping("/{id}/assignment-recommendations")
    public ApiResponse<Map<String, List<AssignmentCandidateResponse>>> assignmentRecommendations(
            @PathVariable UUID id,
            @RequestParam(name = "role_type", required = false) String roleType,
            @RequestParam(name = "department_id", required = false) String departmentId) {
        return ApiResponse.ok(Map.of("candidates",
                assignmentRecommendationService.recommend(id, roleType, departmentId)));
    }

    @PostMapping("/{id}/assignees")
    public ApiResponse<ItemAssignee> assign(
            @PathVariable UUID id,
            @Valid @RequestBody AssignItemRequest request,
            HttpServletRequest servletRequest) {
        return ApiResponse.ok(itemAssignmentService.assign(id, request, CurrentUser.from(servletRequest)));
    }

    @PostMapping("/{id}/confirm-receive")
    public ApiResponse<ItemAssignee> confirmReceive(@PathVariable UUID id, HttpServletRequest servletRequest) {
        return ApiResponse.ok(itemAssignmentService.confirmReceive(id, CurrentUser.from(servletRequest)));
    }

    @PostMapping("/{id}/reject-assignment")
    public ApiResponse<ItemAssignee> rejectAssignment(
            @PathVariable UUID id,
            @Valid @RequestBody RejectAssignmentRequest request,
            HttpServletRequest servletRequest) {
        return ApiResponse.ok(itemAssignmentService.reject(id, request, CurrentUser.from(servletRequest)));
    }
}
