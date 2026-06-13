package com.sherry.supervision.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sherry.supervision.dto.ExcelImportResult;
import com.sherry.supervision.dto.SupervisionBatchDetail;
import com.sherry.supervision.entity.ExcelImportTemplate;
import com.sherry.supervision.entity.SupervisionBatch;
import com.sherry.supervision.entity.SupervisionImportError;
import com.sherry.supervision.entity.SupervisionItem;
import com.sherry.supervision.excel.ExcelImportStrategy;
import com.sherry.supervision.excel.ExcelImportStrategyFactory;
import com.sherry.supervision.exception.ResourceNotFoundException;
import com.sherry.supervision.mapper.ExcelImportTemplateMapper;
import com.sherry.supervision.mapper.SupervisionBatchMapper;
import com.sherry.supervision.mapper.SupervisionImportErrorMapper;
import com.sherry.supervision.mapper.SupervisionItemMapper;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ExcelImportService {

    private final ExcelImportTemplateMapper templateMapper;
    private final SupervisionBatchMapper batchMapper;
    private final SupervisionItemMapper itemMapper;
    private final SupervisionImportErrorMapper errorMapper;
    private final ExcelImportStrategyFactory strategyFactory;
    private final ObjectMapper objectMapper;

    public ExcelImportService(
            ExcelImportTemplateMapper templateMapper,
            SupervisionBatchMapper batchMapper,
            SupervisionItemMapper itemMapper,
            SupervisionImportErrorMapper errorMapper,
            ExcelImportStrategyFactory strategyFactory,
            ObjectMapper objectMapper) {
        this.templateMapper = templateMapper;
        this.batchMapper = batchMapper;
        this.itemMapper = itemMapper;
        this.errorMapper = errorMapper;
        this.strategyFactory = strategyFactory;
        this.objectMapper = objectMapper;
    }

    public List<SupervisionBatch> listBatches() {
        return batchMapper.selectList(new LambdaQueryWrapper<SupervisionBatch>()
                .orderByDesc(SupervisionBatch::getUpdatedAt, SupervisionBatch::getCreatedAt));
    }

    public SupervisionBatchDetail getBatchDetail(UUID batchId) {
        SupervisionBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new ResourceNotFoundException("导入批次不存在：" + batchId);
        }
        List<SupervisionItem> items = itemMapper.selectList(new LambdaQueryWrapper<SupervisionItem>()
                .eq(SupervisionItem::getBatchId, batchId)
                .orderByAsc(SupervisionItem::getSourceRowNo, SupervisionItem::getCreatedAt));
        return new SupervisionBatchDetail(batch, items, listBatchErrors(batchId));
    }

    public List<SupervisionImportError> listBatchErrors(UUID batchId) {
        return errorMapper.selectList(new LambdaQueryWrapper<SupervisionImportError>()
                .eq(SupervisionImportError::getBatchId, batchId)
                .orderByAsc(SupervisionImportError::getRowNo, SupervisionImportError::getCreatedAt));
    }

    public ExcelImportResult importExcel(
            InputStream inputStream, String filename, String templateCode, String createdBy) {
        ExcelImportTemplate template = templateMapper.selectOne(new LambdaQueryWrapper<ExcelImportTemplate>()
                .eq(ExcelImportTemplate::getTemplateCode, templateCode)
                .eq(ExcelImportTemplate::getEnabled, true));
        if (template == null) {
            throw new ResourceNotFoundException("Excel 导入模板不存在：" + templateCode);
        }

        SupervisionBatch batch = createBatch(filename, template, createdBy);
        ExcelImportStrategy strategy = strategyFactory.get(template.getHandlerCode());
        List<SupervisionItem> parsedItems = strategy.parse(inputStream, template);
        java.util.ArrayList<String> errors = new java.util.ArrayList<>();
        int successCount = 0;

        for (SupervisionItem item : parsedItems) {
            try {
                prepareItem(item, batch, createdBy);
                itemMapper.insert(item);
                successCount++;
            } catch (Exception ex) {
                String message = "第 " + item.getSourceRowNo() + " 行导入失败：" + friendlyImportError(item, ex);
                errors.add(message);
                saveImportError(batch, item, message);
            }
        }

        int totalCount = parsedItems.size();
        int failedCount = totalCount - successCount;
        batch.setTotalCount(totalCount);
        batch.setSuccessCount(successCount);
        batch.setFailedCount(failedCount);
        batch.setImportStatus(failedCount == 0 ? "completed" : "failed");
        batch.setUpdatedAt(OffsetDateTime.now());
        batchMapper.updateById(batch);

        return new ExcelImportResult(
                batch.getId().toString(),
                batch.getBatchNo(),
                batch.getBatchName(),
                batch.getImportStatus(),
                batch.getTotalCount(),
                batch.getSuccessCount(),
                batch.getFailedCount(),
                errors);
    }

    private SupervisionBatch createBatch(String filename, ExcelImportTemplate template, String createdBy) {
        String batchNo = "BATCH-" + OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        SupervisionBatch batch = new SupervisionBatch();
        batch.setId(UUID.randomUUID());
        batch.setBatchNo(batchNo);
        batch.setBatchName(template.getTemplateName() + " - " + filename);
        batch.setSourceType("excel");
        batch.setSourceFileId(filename);
        batch.setImportStatus("parsing");
        batch.setTotalCount(0);
        batch.setSuccessCount(0);
        batch.setFailedCount(0);
        batch.setCreatedBy(createdBy);
        batch.setCreatedAt(OffsetDateTime.now());
        batch.setUpdatedAt(OffsetDateTime.now());
        batchMapper.insert(batch);
        return batch;
    }

    private void prepareItem(SupervisionItem item, SupervisionBatch batch, String createdBy) {
        item.setBatchId(batch.getId());
        item.setId(UUID.randomUUID());
        if (!StringUtils.hasText(item.getItemNo())) {
            item.setItemNo(generateItemNo());
        }
        if (!StringUtils.hasText(item.getPriority())) {
            item.setPriority("normal");
        }
        if (!StringUtils.hasText(item.getStatus())) {
            item.setStatus("pending_assign");
        }
        if (!StringUtils.hasText(item.getCreatedBy())) {
            item.setCreatedBy(createdBy);
        }
        item.setCreatedAt(OffsetDateTime.now());
        item.setUpdatedAt(OffsetDateTime.now());
    }

    private void saveImportError(SupervisionBatch batch, SupervisionItem item, String message) {
        SupervisionImportError error = new SupervisionImportError();
        error.setId(UUID.randomUUID());
        error.setBatchId(batch.getId());
        error.setRowNo(item.getSourceRowNo());
        error.setItemNo(item.getItemNo());
        error.setTitle(item.getTitle());
        error.setErrorMessage(message);
        error.setRawData(objectMapper.valueToTree(Map.of(
                "item_no", valueOrEmpty(item.getItemNo()),
                "title", valueOrEmpty(item.getTitle()),
                "priority", valueOrEmpty(item.getPriority()),
                "status", valueOrEmpty(item.getStatus()),
                "created_by", valueOrEmpty(item.getCreatedBy()))));
        error.setCreatedAt(OffsetDateTime.now());
        error.setUpdatedAt(OffsetDateTime.now());
        errorMapper.insert(error);
    }

    private String generateItemNo() {
        return "ITEM-" + OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private String friendlyImportError(SupervisionItem item, Exception exception) {
        if (exception instanceof DataIntegrityViolationException) {
            if (StringUtils.hasText(item.getItemNo())) {
                return "事项编号重复：" + item.getItemNo();
            }
            return "数据违反唯一约束";
        }
        String message = exception.getMessage();
        return StringUtils.hasText(message) ? message.split("\\R")[0] : "未知错误";
    }
}
