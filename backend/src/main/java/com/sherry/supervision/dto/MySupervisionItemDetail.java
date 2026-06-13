package com.sherry.supervision.dto;

import com.sherry.supervision.entity.ItemAssignee;
import com.sherry.supervision.entity.ProgressFeedback;
import com.sherry.supervision.entity.SupervisionItem;
import java.util.List;

public record MySupervisionItemDetail(
        SupervisionItem item,
        ItemAssignee assignment,
        List<ItemAssignee> assignees,
        List<ProgressFeedback> feedbacks) {
}
