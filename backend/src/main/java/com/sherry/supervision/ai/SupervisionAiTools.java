package com.sherry.supervision.ai;

import com.sherry.supervision.dto.ProgressFeedbackRequest;
import com.sherry.supervision.dto.SupervisionBatchDetail;
import com.sherry.supervision.dto.SupervisionItemRequest;
import com.sherry.supervision.entity.ProgressFeedback;
import com.sherry.supervision.entity.SupervisionBatch;
import com.sherry.supervision.entity.SupervisionImportError;
import com.sherry.supervision.entity.SupervisionItem;
import com.sherry.supervision.service.ExcelImportService;
import com.sherry.supervision.service.ProgressFeedbackService;
import com.sherry.supervision.service.SupervisionItemService;
import dev.langchain4j.agent.tool.Tool;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SupervisionAiTools {

    private final SupervisionItemService supervisionItemService;
    private final ProgressFeedbackService progressFeedbackService;
    private final ExcelImportService excelImportService;

    public SupervisionAiTools(
            SupervisionItemService supervisionItemService,
            ProgressFeedbackService progressFeedbackService,
            ExcelImportService excelImportService) {
        this.supervisionItemService = supervisionItemService;
        this.progressFeedbackService = progressFeedbackService;
        this.excelImportService = excelImportService;
    }

    @Tool("查询督办事项，可按状态或关键字过滤")
    public List<SupervisionItem> querySupervisionItems(String status, String keyword) {
        return supervisionItemService.list(status, keyword);
    }

    @Tool("创建一条督办事项")
    public SupervisionItem createSupervisionItem(String title, String description, String priority) {
        return supervisionItemService.create(
                new SupervisionItemRequest(
                        null, title, description, priority, "pending_assign", null, "ai_assistant"));
    }

    @Tool("更新督办事项状态")
    public SupervisionItem updateSupervisionStatus(String itemId, String status) {
        return supervisionItemService.updateStatus(UUID.fromString(itemId), status);
    }

    @Tool("给督办事项添加进度反馈")
    public ProgressFeedback addProgressFeedback(
            String itemId, String content, Integer progressPercent, String riskNote) {
        return progressFeedbackService.create(
                new ProgressFeedbackRequest(
                        UUID.fromString(itemId),
                        "ai_assistant",
                        "AI 助手",
                        progressPercent == null ? 0 : progressPercent,
                        content,
                        riskNote));
    }

    @Tool("查询指定督办事项的进度反馈记录")
    public List<ProgressFeedback> queryProgressFeedbacks(String itemId) {
        return progressFeedbackService.list(UUID.fromString(itemId));
    }

    @Tool("查询最近的 Excel 导入批次")
    public List<SupervisionBatch> queryImportBatches() {
        return excelImportService.listBatches();
    }

    @Tool("查询指定 Excel 导入批次的事项和失败明细")
    public SupervisionBatchDetail queryImportBatchDetail(String batchId) {
        return excelImportService.getBatchDetail(UUID.fromString(batchId));
    }

    @Tool("查询指定 Excel 导入批次的失败行")
    public List<SupervisionImportError> queryImportBatchErrors(String batchId) {
        return excelImportService.listBatchErrors(UUID.fromString(batchId));
    }
}
