package com.sherry.supervision.dto;

import java.util.List;

public record ExcelImportResult(
        String batchId,
        String batchNo,
        String batchName,
        String importStatus,
        Integer totalCount,
        Integer successCount,
        Integer failedCount,
        List<String> errors) {
}
