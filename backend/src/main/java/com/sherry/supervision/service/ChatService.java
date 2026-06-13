package com.sherry.supervision.service;

import com.sherry.supervision.dto.ChatRequest;
import com.sherry.supervision.dto.ChatResponse;
import com.sherry.supervision.dto.ProgressFeedbackRequest;
import com.sherry.supervision.dto.SupervisionBatchDetail;
import com.sherry.supervision.dto.SupervisionItemRequest;
import com.sherry.supervision.entity.ProgressFeedback;
import com.sherry.supervision.entity.SupervisionBatch;
import com.sherry.supervision.entity.SupervisionImportError;
import com.sherry.supervision.entity.SupervisionItem;
import com.sherry.supervision.llm.AiChatClient;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private static final String DEFAULT_PRIORITY = "normal";
    private static final String DEFAULT_STATUS = "pending_assign";
    private static final String AI_ASSISTANT_NAME = "AI 助手";
    private static final String AGENT_KEY = "supervision_assistant";
    private static final String SYSTEM_PROMPT = """
            你是 Sherry 督办后台的 AI 助手。
            你需要用简洁、可靠的中文回答用户。
            当前系统能力包括：查询督办事项、创建督办事项、更新事项状态、添加进度反馈、查询进度反馈、查询 Excel 导入批次和导入失败明细。
            如果用户要执行具体业务动作，应提醒他提供必要字段，避免编造不存在的数据。
            """;

    private final SupervisionItemService supervisionItemService;
    private final ProgressFeedbackService progressFeedbackService;
    private final ExcelImportService excelImportService;
    private final AiToolCallLogService toolCallLogService;
    private final AiChatClient aiChatClient;

    public ChatService(
            SupervisionItemService supervisionItemService,
            ProgressFeedbackService progressFeedbackService,
            ExcelImportService excelImportService,
            AiToolCallLogService toolCallLogService,
            AiChatClient aiChatClient) {
        this.supervisionItemService = supervisionItemService;
        this.progressFeedbackService = progressFeedbackService;
        this.excelImportService = excelImportService;
        this.toolCallLogService = toolCallLogService;
        this.aiChatClient = aiChatClient;
    }

    public ChatResponse run(ChatRequest request) {
        String threadId = request.threadId() == null ? UUID.randomUUID().toString() : request.threadId();
        String requestId = UUID.randomUUID().toString();
        String traceId = UUID.randomUUID().toString();
        Map<String, Object> context = request.context() == null ? Map.of() : request.context();
        String intent = detectIntent(request.message());
        List<Map<String, String>> trace = new ArrayList<>();
        trace.add(Map.of("node", "understand_intent", "detail", intent));

        if ("query_import_batch_detail".equals(intent)) {
            String batchId = stringValue(context.get("batch_id"));
            if (isBlank(batchId)) {
                return response(
                        requestId,
                        threadId,
                        "我需要先知道要查询哪个导入批次。",
                        true,
                        List.of("请提供导入批次 ID，或先让我查询最近导入批次。"),
                        intent,
                        "waiting_for_input",
                        traceId,
                        trace);
            }
            SupervisionBatchDetail detail = executeTool(
                    requestId,
                    threadId,
                    intent,
                    Map.of("batch_id", batchId),
                    () -> excelImportService.getBatchDetail(UUID.fromString(batchId)));
            return response(
                    requestId,
                    threadId,
                    answerImportBatchDetail(detail),
                    false,
                    List.of(),
                    intent,
                    "completed",
                    traceId,
                    trace);
        }
        if ("query_import_batches".equals(intent)) {
            List<SupervisionBatch> batches = executeTool(
                    requestId,
                    threadId,
                    intent,
                    Map.of(),
                    excelImportService::listBatches);
            return response(
                    requestId,
                    threadId,
                    answerImportBatches(batches),
                    false,
                    List.of(),
                    intent,
                    "completed",
                    traceId,
                    trace);
        }
        if ("query_supervision_items".equals(intent)) {
            String status = stringValue(context.get("status"));
            String keyword = stringValue(context.get("keyword"));
            List<SupervisionItem> items = executeTool(
                    requestId,
                    threadId,
                    intent,
                    Map.of("status", valueOrDefault(status, ""), "keyword", valueOrDefault(keyword, "")),
                    () -> supervisionItemService.list(status, keyword));
            return response(requestId, threadId, answerItems(items), false, List.of(), intent, "completed", traceId, trace);
        }
        if ("create_supervision_item".equals(intent)) {
            String title = firstNotBlank(stringValue(context.get("title")), cleanupTitle(request.message()));
            SupervisionItemRequest itemRequest = new SupervisionItemRequest(
                    null,
                    title,
                    stringValue(context.get("description")),
                    firstNotBlank(stringValue(context.get("priority")), DEFAULT_PRIORITY),
                    DEFAULT_STATUS,
                    null,
                    firstNotBlank(request.userId(), "ai_assistant"));
            SupervisionItem item = executeTool(
                    requestId,
                    threadId,
                    intent,
                    itemRequest,
                    () -> supervisionItemService.create(itemRequest));
            return response(
                    requestId,
                    threadId,
                    "已创建督办事项：" + item.getTitle() + "，编号：" + item.getItemNo() + "。",
                    false,
                    List.of(),
                    intent,
                    "completed",
                    traceId,
                    trace);
        }
        if ("update_supervision_status".equals(intent) && context.get("item_id") != null) {
            String status = firstNotBlank(stringValue(context.get("status")), "completed");
            UUID itemId = UUID.fromString(context.get("item_id").toString());
            SupervisionItem item = executeTool(
                    requestId,
                    threadId,
                    intent,
                    Map.of("item_id", itemId.toString(), "status", status),
                    () -> supervisionItemService.updateStatus(itemId, status));
            return response(
                    requestId,
                    threadId,
                    "已更新督办事项状态：" + item.getTitle() + " -> " + item.getStatus() + "。",
                    false,
                    List.of(),
                    intent,
                    "completed",
                    traceId,
                    trace);
        }
        if ("query_progress_feedbacks".equals(intent) && context.get("item_id") != null) {
            UUID itemId = UUID.fromString(context.get("item_id").toString());
            List<ProgressFeedback> feedbacks = executeTool(
                    requestId,
                    threadId,
                    intent,
                    Map.of("item_id", itemId.toString()),
                    () -> progressFeedbackService.list(itemId));
            return response(
                    requestId,
                    threadId,
                    answerFeedbacks(feedbacks),
                    false,
                    List.of(),
                    intent,
                    "completed",
                    traceId,
                    trace);
        }
        if ("progress_feedback".equals(intent) && context.get("item_id") != null) {
            ProgressFeedbackRequest feedbackRequest = new ProgressFeedbackRequest(
                    UUID.fromString(context.get("item_id").toString()),
                    firstNotBlank(request.userId(), "ai_assistant"),
                    AI_ASSISTANT_NAME,
                    intValue(context.get("progress_percent")),
                    firstNotBlank(stringValue(context.get("content")), request.message()),
                    stringValue(context.get("risk_note")));
            executeTool(
                    requestId,
                    threadId,
                    intent,
                    feedbackRequest,
                    () -> progressFeedbackService.create(feedbackRequest));
            return response(
                    requestId, threadId, "已记录进度反馈。", false, List.of(), intent, "completed", traceId, trace);
        }

        if (aiChatClient.isAvailable()) {
            try {
                String answer = aiChatClient.chat(SYSTEM_PROMPT, request.message());
                return response(
                        requestId,
                        threadId,
                        answer,
                        false,
                        List.of(),
                        intent,
                        "completed",
                        traceId,
                        trace);
            } catch (RuntimeException ex) {
                return response(
                        requestId,
                        threadId,
                        "Kimi 模型暂时不可用，业务接口能力仍可正常使用。错误信息：" + safeError(ex),
                        false,
                        List.of(),
                        intent,
                        "model_unavailable",
                        traceId,
                        trace);
            }
        }

        return response(
                requestId,
                threadId,
                "我已收到。当前助手支持查询督办事项、创建事项、更新状态、添加进度反馈、查询进度反馈、查询 Excel 导入批次和查看导入失败明细。",
                false,
                List.of(),
                intent,
                "completed",
                traceId,
                trace);
    }

    private <T> T executeTool(
            String requestId,
            String threadId,
            String toolName,
            Object inputPayload,
            Supplier<T> supplier) {
        long startedAt = System.nanoTime();
        try {
            T result = supplier.get();
            toolCallLogService.recordSuccess(
                    requestId,
                    threadId,
                    AGENT_KEY,
                    toolName,
                    inputPayload,
                    result,
                    elapsedMillis(startedAt));
            return result;
        } catch (RuntimeException ex) {
            toolCallLogService.recordFailure(
                    requestId,
                    threadId,
                    AGENT_KEY,
                    toolName,
                    inputPayload,
                    ex.getMessage(),
                    elapsedMillis(startedAt));
            throw ex;
        }
    }

    private long elapsedMillis(long startedAt) {
        return Math.max(0, (System.nanoTime() - startedAt) / 1_000_000);
    }

    private String detectIntent(String message) {
        String text = message == null ? "" : message;
        if (containsAny(text, "失败明细", "失败行", "导入错误", "批次详情", "batch detail", "import error", "failed rows")) {
            return "query_import_batch_detail";
        }
        if (containsAny(text, "导入批次", "Excel 导入", "Excel导入", "最近导入", "导入结果", "import batches", "excel import")) {
            return "query_import_batches";
        }
        if (containsAny(text, "查询进度反馈", "查看进度反馈", "反馈记录")) {
            return "query_progress_feedbacks";
        }
        if (containsAny(text, "查询督办", "督办事项列表", "有哪些督办", "事项列表")) {
            return "query_supervision_items";
        }
        if (containsAny(text, "新增", "创建", "新建")) {
            return "create_supervision_item";
        }
        if (containsAny(text, "状态", "完成", "更新状态", "改成")) {
            return "update_supervision_status";
        }
        if (containsAny(text, "进度", "反馈", "回执")) {
            return "progress_feedback";
        }
        return "chat";
    }

    private ChatResponse response(
            String requestId,
            String threadId,
            String answer,
            boolean needClarification,
            List<String> questions,
            String intent,
            String status,
            String traceId,
            List<Map<String, String>> trace) {
        return new ChatResponse(
                requestId, threadId, answer, needClarification, questions, intent, new HashMap<>(), status, traceId, trace);
    }

    private String answerItems(List<SupervisionItem> items) {
        if (items.isEmpty()) {
            return "当前没有查到匹配的督办事项。";
        }
        StringBuilder builder = new StringBuilder("查到 ").append(items.size()).append(" 条督办事项，先列前 ")
                .append(Math.min(items.size(), 5)).append(" 条：");
        items.stream().limit(5).forEach(item -> builder.append("\n- ")
                .append(item.getTitle()).append("（").append(item.getStatus()).append("，编号：")
                .append(item.getItemNo()).append("）"));
        return builder.toString();
    }

    private String answerFeedbacks(List<ProgressFeedback> feedbacks) {
        if (feedbacks.isEmpty()) {
            return "当前没有查到进度反馈。";
        }
        StringBuilder builder = new StringBuilder("查到 ").append(feedbacks.size()).append(" 条进度反馈，先列前 ")
                .append(Math.min(feedbacks.size(), 5)).append(" 条：");
        feedbacks.stream().limit(5).forEach(feedback -> builder.append("\n- ")
                .append(feedback.getContent()).append("（")
                .append(feedback.getProgressPercent() == null ? 0 : feedback.getProgressPercent()).append("%）"));
        return builder.toString();
    }

    private String answerImportBatches(List<SupervisionBatch> batches) {
        if (batches.isEmpty()) {
            return "当前没有查到 Excel 导入批次。";
        }
        StringBuilder builder = new StringBuilder("查到 ").append(batches.size()).append(" 个导入批次，先列前 ")
                .append(Math.min(batches.size(), 5)).append(" 个：");
        batches.stream().limit(5).forEach(batch -> builder.append("\n- ")
                .append(batch.getBatchNo()).append("（").append(valueOrDefault(batch.getImportStatus(), "unknown"))
                .append("，总数 ").append(count(batch.getTotalCount()))
                .append("，成功 ").append(count(batch.getSuccessCount()))
                .append("，失败 ").append(count(batch.getFailedCount())).append("）"));
        return builder.toString();
    }

    private String answerImportBatchDetail(SupervisionBatchDetail detail) {
        SupervisionBatch batch = detail.batch();
        StringBuilder builder = new StringBuilder("批次 ").append(batch.getBatchNo()).append(" 导入结果：")
                .append("总数 ").append(count(batch.getTotalCount()))
                .append("，成功 ").append(count(batch.getSuccessCount()))
                .append("，失败 ").append(count(batch.getFailedCount())).append("。");
        if (detail.errors().isEmpty()) {
            return builder.append("\n没有失败行。").toString();
        }
        builder.append("\n失败行：");
        detail.errors().stream().limit(10).forEach(error -> builder.append("\n- 第 ")
                .append(count(error.getRowNo())).append(" 行：")
                .append(valueOrDefault(error.getErrorMessage(), "未知错误")));
        return builder.toString();
    }

    private String cleanupTitle(String message) {
        return firstNotBlank(message, "")
                .replace("新增督办", "")
                .replace("创建督办", "")
                .replace("新建督办", "")
                .replace("新增事项", "")
                .replace("创建事项", "")
                .replace("新建事项", "")
                .trim();
    }

    private boolean containsAny(String text, String... candidates) {
        for (String candidate : candidates) {
            if (text.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private String firstNotBlank(String first, String fallback) {
        return isBlank(first) ? fallback : first;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private Integer intValue(Object value) {
        if (value == null) {
            return 0;
        }
        return Integer.parseInt(value.toString());
    }

    private int count(Integer value) {
        return value == null ? 0 : value;
    }

    private String valueOrDefault(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    private String safeError(RuntimeException exception) {
        String message = exception.getMessage();
        return isBlank(message) ? exception.getClass().getSimpleName() : message;
    }
}
