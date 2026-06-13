package com.sherry.supervision.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sherry.supervision.common.ApiResponse;
import com.sherry.supervision.dto.SupervisionItemDetail;
import com.sherry.supervision.entity.ProgressFeedback;
import com.sherry.supervision.entity.SupervisionItem;
import com.sherry.supervision.service.ProgressFeedbackService;
import com.sherry.supervision.service.SupervisionItemService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SupervisionItemControllerTest {

    private SupervisionItemService supervisionItemService;
    private ProgressFeedbackService progressFeedbackService;
    private SupervisionItemController controller;

    @BeforeEach
    void setUp() {
        supervisionItemService = mock(SupervisionItemService.class);
        progressFeedbackService = mock(ProgressFeedbackService.class);
        controller = new SupervisionItemController(supervisionItemService, progressFeedbackService);
    }

    @Test
    void shouldReturnItemDetailWithProgressFeedbacks() {
        UUID itemId = UUID.randomUUID();
        SupervisionItem item = new SupervisionItem();
        item.setId(itemId);
        item.setTitle("合同确认");

        ProgressFeedback feedback = new ProgressFeedback();
        feedback.setItemId(itemId);
        feedback.setContent("负责人已接收");
        feedback.setProgressPercent(60);

        when(supervisionItemService.get(itemId)).thenReturn(item);
        when(progressFeedbackService.list(itemId)).thenReturn(List.of(feedback));

        ApiResponse<SupervisionItemDetail> response = controller.detail(itemId);

        assertThat(response.data().item()).isSameAs(item);
        assertThat(response.data().feedbacks()).containsExactly(feedback);
        verify(supervisionItemService).get(itemId);
        verify(progressFeedbackService).list(itemId);
    }
}
