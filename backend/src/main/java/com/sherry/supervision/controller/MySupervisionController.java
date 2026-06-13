package com.sherry.supervision.controller;

import com.sherry.supervision.auth.CurrentUser;
import com.sherry.supervision.common.ApiResponse;
import com.sherry.supervision.dto.MySupervisionItem;
import com.sherry.supervision.dto.MySupervisionItemDetail;
import com.sherry.supervision.entity.AppUser;
import com.sherry.supervision.entity.ItemAssignee;
import com.sherry.supervision.entity.SupervisionItem;
import com.sherry.supervision.service.ItemAssignmentService;
import com.sherry.supervision.service.ProgressFeedbackService;
import com.sherry.supervision.service.SupervisionItemService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/my/supervision-items")
public class MySupervisionController {

    private final ItemAssignmentService itemAssignmentService;
    private final SupervisionItemService supervisionItemService;
    private final ProgressFeedbackService progressFeedbackService;

    public MySupervisionController(
            ItemAssignmentService itemAssignmentService,
            SupervisionItemService supervisionItemService,
            ProgressFeedbackService progressFeedbackService) {
        this.itemAssignmentService = itemAssignmentService;
        this.supervisionItemService = supervisionItemService;
        this.progressFeedbackService = progressFeedbackService;
    }

    @GetMapping
    public ApiResponse<Map<String, List<MySupervisionItem>>> list(HttpServletRequest request) {
        AppUser currentUser = CurrentUser.from(request);
        List<MySupervisionItem> items = itemAssignmentService.listByAssignee(currentUser.getUsername()).stream()
                .map(this::toMyItem)
                .toList();
        return ApiResponse.ok(Map.of("items", items));
    }

    @GetMapping("/{id}")
    public ApiResponse<MySupervisionItemDetail> detail(@PathVariable UUID id, HttpServletRequest request) {
        AppUser currentUser = CurrentUser.from(request);
        ItemAssignee assignment = itemAssignmentService.getCurrentAssignment(id, currentUser.getUsername());
        return ApiResponse.ok(new MySupervisionItemDetail(
                supervisionItemService.get(id),
                assignment,
                itemAssignmentService.listByItem(id),
                progressFeedbackService.list(id)));
    }

    private MySupervisionItem toMyItem(ItemAssignee assignment) {
        SupervisionItem item = supervisionItemService.get(assignment.getItemId());
        return new MySupervisionItem(item, assignment, riskLevel(item, assignment), requiresHumanReview(item));
    }

    private String riskLevel(SupervisionItem item, ItemAssignee assignment) {
        if ("blocked".equals(item.getStatus()) || "urgent".equals(item.getPriority())) {
            return "high";
        }
        if (item.getDeadlineAt() != null && item.getDeadlineAt().isBefore(OffsetDateTime.now().plusDays(2))) {
            return "medium";
        }
        if ("rejected".equals(assignment.getConfirmStatus())) {
            return "medium";
        }
        return "low";
    }

    private boolean requiresHumanReview(SupervisionItem item) {
        return "completed".equals(item.getStatus()) || "cancelled".equals(item.getStatus());
    }
}
