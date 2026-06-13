package com.sherry.supervision.dto;

import com.sherry.supervision.entity.ProgressFeedback;
import com.sherry.supervision.entity.SupervisionItem;
import java.util.List;

public record SupervisionItemDetail(
        SupervisionItem item,
        List<ProgressFeedback> feedbacks) {
}
