package com.sherry.supervision.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ChatServiceTest {

    private SupervisionItemService supervisionItemService;
    private ProgressFeedbackService progressFeedbackService;
    private ExcelImportService excelImportService;
    private AiToolCallLogService toolCallLogService;
    private AiChatClient aiChatClient;
    private ChatService chatService;

    @BeforeEach
    void setUp() {
        supervisionItemService = mock(SupervisionItemService.class);
        progressFeedbackService = mock(ProgressFeedbackService.class);
        excelImportService = mock(ExcelImportService.class);
        toolCallLogService = mock(AiToolCallLogService.class);
        aiChatClient = mock(AiChatClient.class);
        when(aiChatClient.isAvailable()).thenReturn(false);
        chatService = new ChatService(
                supervisionItemService,
                progressFeedbackService,
                excelImportService,
                toolCallLogService,
                aiChatClient);
    }

    @Test
    void shouldCreateSupervisionItemThroughBusinessService() {
        SupervisionItem created = new SupervisionItem();
        created.setTitle("整理周报");
        created.setItemNo("ITEM-001");
        when(supervisionItemService.create(any(SupervisionItemRequest.class))).thenReturn(created);

        ChatResponse response = chatService.run(new ChatRequest(
                "admin_user",
                "新增督办 整理周报",
                null,
                Map.of("title", "整理周报", "description", "本周经营数据", "priority", "high")));

        ArgumentCaptor<SupervisionItemRequest> captor = ArgumentCaptor.forClass(SupervisionItemRequest.class);
        verify(supervisionItemService).create(captor.capture());
        verify(toolCallLogService).recordSuccess(
                eq(response.requestId()),
                eq(response.threadId()),
                eq("supervision_assistant"),
                eq("create_supervision_item"),
                any(),
                any(),
                any());
        assertThat(captor.getValue().title()).isEqualTo("整理周报");
        assertThat(captor.getValue().description()).isEqualTo("本周经营数据");
        assertThat(captor.getValue().priority()).isEqualTo("high");
        assertThat(captor.getValue().status()).isEqualTo("pending_assign");
        assertThat(captor.getValue().createdBy()).isEqualTo("admin_user");
        assertThat(response.intent()).isEqualTo("create_supervision_item");
        assertThat(response.answer()).contains("已创建督办事项", "整理周报", "ITEM-001");
    }

    @Test
    void shouldUpdateStatusThroughBusinessService() {
        UUID itemId = UUID.randomUUID();
        SupervisionItem updated = new SupervisionItem();
        updated.setTitle("合同确认");
        updated.setStatus("completed");
        when(supervisionItemService.updateStatus(eq(itemId), eq("completed"))).thenReturn(updated);

        ChatResponse response = chatService.run(new ChatRequest(
                "admin_user",
                "把事项状态改成完成",
                null,
                Map.of("item_id", itemId.toString(), "status", "completed")));

        verify(supervisionItemService).updateStatus(itemId, "completed");
        assertThat(response.intent()).isEqualTo("update_supervision_status");
        assertThat(response.answer()).contains("已更新督办事项状态", "合同确认", "已完成");
    }

    @Test
    void shouldAddProgressFeedbackThroughBusinessService() {
        UUID itemId = UUID.randomUUID();
        when(progressFeedbackService.create(any(ProgressFeedbackRequest.class))).thenReturn(new ProgressFeedback());

        ChatResponse response = chatService.run(new ChatRequest(
                "admin_user",
                "添加进度反馈",
                null,
                Map.of(
                        "item_id", itemId.toString(),
                        "content", "已联系负责人",
                        "progress_percent", 60,
                        "risk_note", "等待回执")));

        ArgumentCaptor<ProgressFeedbackRequest> captor = ArgumentCaptor.forClass(ProgressFeedbackRequest.class);
        verify(progressFeedbackService).create(captor.capture());
        assertThat(captor.getValue().itemId()).isEqualTo(itemId);
        assertThat(captor.getValue().feedbackUserId()).isEqualTo("admin_user");
        assertThat(captor.getValue().feedbackUserName()).isEqualTo("AI 助手");
        assertThat(captor.getValue().progressPercent()).isEqualTo(60);
        assertThat(captor.getValue().content()).isEqualTo("已联系负责人");
        assertThat(captor.getValue().riskNote()).isEqualTo("等待回执");
        assertThat(response.intent()).isEqualTo("progress_feedback");
        assertThat(response.answer()).contains("已记录进度反馈");
    }

    @Test
    void shouldQueryProgressFeedbacksThroughBusinessService() {
        UUID itemId = UUID.randomUUID();
        ProgressFeedback feedback = new ProgressFeedback();
        feedback.setContent("负责人已接收");
        feedback.setProgressPercent(80);
        when(progressFeedbackService.list(itemId)).thenReturn(List.of(feedback));

        ChatResponse response = chatService.run(new ChatRequest(
                "admin_user",
                "查询进度反馈",
                null,
                Map.of("item_id", itemId.toString())));

        verify(progressFeedbackService).list(itemId);
        assertThat(response.intent()).isEqualTo("query_progress_feedbacks");
        assertThat(response.answer()).contains("查到 1 条进度反馈", "负责人已接收", "80%");
    }

    @Test
    void shouldQueryImportBatchesThroughBusinessService() {
        SupervisionBatch batch = new SupervisionBatch();
        batch.setId(UUID.randomUUID());
        batch.setBatchNo("BATCH-202606120001");
        batch.setBatchName("默认督办模板 - test.xlsx");
        batch.setImportStatus("failed");
        batch.setTotalCount(2);
        batch.setSuccessCount(1);
        batch.setFailedCount(1);
        when(excelImportService.listBatches()).thenReturn(List.of(batch));

        ChatResponse response = chatService.run(new ChatRequest(
                "admin_user",
                "查询最近 Excel 导入批次",
                null,
                Map.of()));

        verify(excelImportService).listBatches();
        verify(toolCallLogService).recordSuccess(
                eq(response.requestId()),
                eq(response.threadId()),
                eq("supervision_assistant"),
                eq("query_import_batches"),
                any(),
                any(),
                any());
        assertThat(response.intent()).isEqualTo("query_import_batches");
        assertThat(response.answer()).contains("查到 1 个导入批次", "BATCH-202606120001", "总数 2", "成功 1", "失败 1");
    }

    @Test
    void shouldQueryImportBatchesWithAsciiHealthCheckMessage() {
        when(excelImportService.listBatches()).thenReturn(List.of());

        ChatResponse response = chatService.run(new ChatRequest(
                "admin_user",
                "query import batches",
                null,
                Map.of()));

        verify(excelImportService).listBatches();
        assertThat(response.intent()).isEqualTo("query_import_batches");
        assertThat(response.answer()).contains("当前没有查到 Excel 导入批次");
    }

    @Test
    void shouldQueryImportBatchDetailThroughBusinessService() {
        UUID batchId = UUID.randomUUID();
        SupervisionBatch batch = new SupervisionBatch();
        batch.setId(batchId);
        batch.setBatchNo("BATCH-202606120002");
        batch.setImportStatus("failed");
        batch.setTotalCount(2);
        batch.setSuccessCount(1);
        batch.setFailedCount(1);

        SupervisionImportError error = new SupervisionImportError();
        error.setRowNo(3);
        error.setItemNo("DUP-IMPORT-CODE");
        error.setTitle("重复事项");
        error.setErrorMessage("第 3 行导入失败：事项编号重复：DUP-IMPORT-CODE");

        SupervisionBatchDetail detail = new SupervisionBatchDetail(batch, List.of(), List.of(error));
        when(excelImportService.getBatchDetail(batchId)).thenReturn(detail);

        ChatResponse response = chatService.run(new ChatRequest(
                "admin_user",
                "查询导入批次失败明细",
                null,
                Map.of("batch_id", batchId.toString())));

        verify(excelImportService).getBatchDetail(batchId);
        assertThat(response.intent()).isEqualTo("query_import_batch_detail");
        assertThat(response.answer()).contains(
                "BATCH-202606120002", "总数 2", "成功 1", "失败 1", "第 3 行", "DUP-IMPORT-CODE");
    }

    @Test
    void shouldAskForBatchIdWhenQueryingImportBatchDetailWithoutContext() {
        ChatResponse response = chatService.run(new ChatRequest(
                "admin_user",
                "查询导入批次失败明细",
                null,
                Map.of()));

        verifyNoInteractions(excelImportService);
        assertThat(response.intent()).isEqualTo("query_import_batch_detail");
        assertThat(response.needClarification()).isTrue();
        assertThat(response.questions()).contains("请提供导入批次 ID，或先让我查询最近导入批次。");
    }

    @Test
    void shouldUseKimiClientForPlainChatWhenAvailable() {
        when(aiChatClient.isAvailable()).thenReturn(true);
        when(aiChatClient.chat(any(), eq("帮我总结一下督办系统能做什么"))).thenReturn("可以帮你查询、创建和跟进督办事项。");

        ChatResponse response = chatService.run(new ChatRequest(
                "admin_user",
                "帮我总结一下督办系统能做什么",
                null,
                Map.of()));

        verify(aiChatClient).chat(any(), eq("帮我总结一下督办系统能做什么"));
        assertThat(response.intent()).isEqualTo("chat");
        assertThat(response.answer()).isEqualTo("可以帮你查询、创建和跟进督办事项。");
    }

    @Test
    void shouldFallbackWhenKimiClientUnavailable() {
        ChatResponse response = chatService.run(new ChatRequest(
                "admin_user",
                "随便聊聊",
                null,
                Map.of()));

        verify(aiChatClient).isAvailable();
        assertThat(response.intent()).isEqualTo("chat");
        assertThat(response.answer()).contains("当前助手支持查询督办事项");
    }

    @Test
    void shouldReturnFriendlyMessageWhenKimiClientFails() {
        when(aiChatClient.isAvailable()).thenReturn(true);
        when(aiChatClient.chat(any(), eq("随便聊聊"))).thenThrow(new IllegalStateException("Invalid Authentication"));

        ChatResponse response = chatService.run(new ChatRequest(
                "admin_user",
                "随便聊聊",
                null,
                Map.of()));

        assertThat(response.intent()).isEqualTo("chat");
        assertThat(response.status()).isEqualTo("model_unavailable");
        assertThat(response.answer()).contains("Kimi 模型暂时不可用", "Invalid Authentication");
    }
}
