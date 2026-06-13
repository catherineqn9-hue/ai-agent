package com.sherry.supervision.controller;

import com.sherry.supervision.common.ApiResponse;
import com.sherry.supervision.configresource.ConfigResourceRegistry;
import com.sherry.supervision.util.JsonbUtils;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin-menus")
public class AdminMenuController {

    private final JdbcTemplate jdbcTemplate;
    private final ConfigResourceRegistry registry;

    public AdminMenuController(JdbcTemplate jdbcTemplate, ConfigResourceRegistry registry) {
        this.jdbcTemplate = jdbcTemplate;
        this.registry = registry;
    }

    @GetMapping
    public ApiResponse<Map<String, List<Map<String, Object>>>> menus() {
        String sql = """
                SELECT menu_id, title, icon, menu_type, resource, hint, group_name, table_fields, form_fields
                FROM admin_menu_config
                WHERE enabled = true
                ORDER BY sort_order ASC, created_at ASC
                """;
        return ApiResponse.ok(Map.of("menus", jdbcTemplate.query(sql, (rs, rowNum) -> toMenu(rs))));
    }

    private Map<String, Object> toMenu(ResultSet rs) throws SQLException {
        Map<String, Object> menu = new LinkedHashMap<>();
        menu.put("id", rs.getString("menu_id"));
        menu.put("title", rs.getString("title"));
        menu.put("icon", rs.getString("icon"));
        menu.put("type", rs.getString("menu_type"));
        menu.put("resource", rs.getString("resource"));
        menu.put("readOnly", readOnly(rs.getString("menu_type"), rs.getString("resource")));
        menu.put("hint", rs.getString("hint"));
        menu.put("groupName", rs.getString("group_name"));
        menu.put("tableFields", JsonbUtils.fromJsonText(rs.getString("table_fields")));
        menu.put("fields", JsonbUtils.fromJsonText(rs.getString("form_fields")));
        return menu;
    }

    private boolean readOnly(String menuType, String resource) {
        if (!"crud".equals(menuType) || resource == null || resource.isBlank()) {
            return false;
        }
        try {
            return registry.get(resource).readOnly();
        } catch (RuntimeException ex) {
            return false;
        }
    }
}
