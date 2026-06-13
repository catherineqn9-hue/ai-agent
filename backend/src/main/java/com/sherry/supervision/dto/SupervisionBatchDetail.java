package com.sherry.supervision.dto;

import com.sherry.supervision.entity.SupervisionBatch;
import com.sherry.supervision.entity.SupervisionImportError;
import com.sherry.supervision.entity.SupervisionItem;
import java.util.List;

public record SupervisionBatchDetail(
        SupervisionBatch batch,
        List<SupervisionItem> items,
        List<SupervisionImportError> errors) {
}
