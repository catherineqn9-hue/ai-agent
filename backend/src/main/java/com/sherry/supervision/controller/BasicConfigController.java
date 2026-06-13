package com.sherry.supervision.controller;

import com.sherry.supervision.common.ApiResponse;
import com.sherry.supervision.service.BasicConfigService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/basic-configs")
public class BasicConfigController {

    private final BasicConfigService basicConfigService;

    public BasicConfigController(BasicConfigService basicConfigService) {
        this.basicConfigService = basicConfigService;
    }

    @GetMapping("/resources")
    public ApiResponse<Map<String, List<Map<String, Object>>>> resources() {
        return ApiResponse.ok(Map.of("resources", basicConfigService.describeResources()));
    }

    @GetMapping("/{resourceName}")
    public ApiResponse<Map<String, List<Map<String, Object>>>> list(@PathVariable String resourceName) {
        return ApiResponse.ok(Map.of("items", basicConfigService.list(resourceName)));
    }

    @PostMapping("/{resourceName}")
    public ApiResponse<Map<String, Object>> create(
            @PathVariable String resourceName, @RequestBody Map<String, Object> payload) {
        return ApiResponse.ok(basicConfigService.create(resourceName, payload));
    }

    @GetMapping("/{resourceName}/{id}")
    public ApiResponse<Map<String, Object>> get(@PathVariable String resourceName, @PathVariable UUID id) {
        return ApiResponse.ok(basicConfigService.get(resourceName, id));
    }

    @PutMapping("/{resourceName}/{id}")
    public ApiResponse<Map<String, Object>> update(
            @PathVariable String resourceName,
            @PathVariable UUID id,
            @RequestBody Map<String, Object> payload) {
        return ApiResponse.ok(basicConfigService.update(resourceName, id, payload));
    }

    @DeleteMapping("/{resourceName}/{id}")
    public ApiResponse<Map<String, String>> delete(@PathVariable String resourceName, @PathVariable UUID id) {
        basicConfigService.delete(resourceName, id);
        return ApiResponse.ok(Map.of("deleted", id.toString()));
    }
}
