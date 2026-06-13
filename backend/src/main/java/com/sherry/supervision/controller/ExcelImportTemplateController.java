package com.sherry.supervision.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sherry.supervision.common.ApiResponse;
import com.sherry.supervision.dto.ExcelImportResult;
import com.sherry.supervision.dto.SupervisionBatchDetail;
import com.sherry.supervision.entity.ExcelImportTemplate;
import com.sherry.supervision.entity.SupervisionBatch;
import com.sherry.supervision.entity.SupervisionImportError;
import com.sherry.supervision.mapper.ExcelImportTemplateMapper;
import com.sherry.supervision.service.ExcelImportService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/supervision-imports")
public class ExcelImportTemplateController {

    private final ExcelImportTemplateMapper excelImportTemplateMapper;
    private final ExcelImportService excelImportService;

    public ExcelImportTemplateController(
            ExcelImportTemplateMapper excelImportTemplateMapper,
            ExcelImportService excelImportService) {
        this.excelImportTemplateMapper = excelImportTemplateMapper;
        this.excelImportService = excelImportService;
    }

    @GetMapping("/templates")
    public ApiResponse<Map<String, List<ExcelImportTemplate>>> listTemplates() {
        LambdaQueryWrapper<ExcelImportTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExcelImportTemplate::getEnabled, true);
        wrapper.orderByAsc(ExcelImportTemplate::getTemplateName, ExcelImportTemplate::getCreatedAt);
        return ApiResponse.ok(Map.of("templates", excelImportTemplateMapper.selectList(wrapper)));
    }

    @GetMapping("/batches")
    public ApiResponse<Map<String, List<SupervisionBatch>>> listBatches() {
        return ApiResponse.ok(Map.of("batches", excelImportService.listBatches()));
    }

    @GetMapping("/batches/{batchId}")
    public ApiResponse<SupervisionBatchDetail> getBatchDetail(@PathVariable UUID batchId) {
        return ApiResponse.ok(excelImportService.getBatchDetail(batchId));
    }

    @GetMapping("/batches/{batchId}/errors")
    public ApiResponse<Map<String, List<SupervisionImportError>>> listBatchErrors(@PathVariable UUID batchId) {
        return ApiResponse.ok(Map.of("errors", excelImportService.listBatchErrors(batchId)));
    }

    @PostMapping("/excel")
    public ApiResponse<ExcelImportResult> importExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "template_code", defaultValue = "standard_supervision") String templateCode,
            @RequestParam(name = "created_by", defaultValue = "admin") String createdBy) throws Exception {
        return ApiResponse.ok(excelImportService.importExcel(
                file.getInputStream(), file.getOriginalFilename(), templateCode, createdBy));
    }
}
