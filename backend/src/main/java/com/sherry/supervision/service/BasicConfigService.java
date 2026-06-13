package com.sherry.supervision.service;

import com.sherry.supervision.configresource.ConfigResource;
import com.sherry.supervision.configresource.ConfigResourceRegistry;
import com.sherry.supervision.exception.InvalidRequestException;
import com.sherry.supervision.util.JsonbUtils;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class BasicConfigService {

    private final JdbcTemplate jdbcTemplate;
    private final ConfigResourceRegistry registry;

    public BasicConfigService(JdbcTemplate jdbcTemplate, ConfigResourceRegistry registry) {
        this.jdbcTemplate = jdbcTemplate;
        this.registry = registry;
    }

    public List<Map<String, Object>> describeResources() {
        return registry.list().stream()
                .map(resource -> Map.<String, Object>of(
                        "name", resource.name(),
                        "table", resource.tableName(),
                        "key_field", resource.keyField(),
                        "name_field", resource.nameField(),
                        "fields", resource.fields(),
                        "json_fields", resource.jsonFields(),
                        "read_only", resource.readOnly()))
                .toList();
    }

    public List<Map<String, Object>> list(String resourceName) {
        ConfigResource resource = registry.get(resourceName);
        String sql = "SELECT " + String.join(", ", resource.fields()) + " FROM " + resource.tableName()
                + orderBy(resource);
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapRow(rs, resource.fields(), resource));
    }

    public Map<String, Object> get(String resourceName, UUID id) {
        ConfigResource resource = registry.get(resourceName);
        String sql = "SELECT " + String.join(", ", resource.fields()) + " FROM " + resource.tableName()
                + " WHERE id = ?";
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> mapRow(rs, resource.fields(), resource), id);
    }

    public Map<String, Object> create(String resourceName, Map<String, Object> payload) {
        ConfigResource resource = registry.get(resourceName);
        rejectReadonly(resource);
        Map<String, Object> values = normalizedValues(resource, payload, true);
        UUID id = UUID.randomUUID();
        values.put("id", id);

        String columns = String.join(", ", values.keySet());
        String placeholders = String.join(", ", values.keySet().stream().map(key -> "?").toList());
        jdbcTemplate.update(
                "INSERT INTO " + resource.tableName() + " (" + columns + ") VALUES (" + placeholders + ")",
                values.values().toArray());
        return get(resourceName, id);
    }

    public Map<String, Object> update(String resourceName, UUID id, Map<String, Object> payload) {
        ConfigResource resource = registry.get(resourceName);
        rejectReadonly(resource);
        Map<String, Object> values = normalizedValues(resource, payload, false);
        if (!values.isEmpty()) {
            List<String> assignments = new ArrayList<>();
            List<Object> params = new ArrayList<>();
            values.forEach((field, value) -> {
                assignments.add(field + " = ?");
                params.add(value);
            });
            assignments.add("updated_at = now()");
            params.add(id);
            jdbcTemplate.update(
                    "UPDATE " + resource.tableName() + " SET " + String.join(", ", assignments) + " WHERE id = ?",
                    params.toArray());
        }
        return get(resourceName, id);
    }

    public void delete(String resourceName, UUID id) {
        ConfigResource resource = registry.get(resourceName);
        rejectReadonly(resource);
        jdbcTemplate.update("DELETE FROM " + resource.tableName() + " WHERE id = ?", id);
    }

    private void rejectReadonly(ConfigResource resource) {
        if (resource.readOnly()) {
            throw new InvalidRequestException("配置资源为只读，不能新增、编辑或删除：" + resource.name());
        }
    }

    private String orderBy(ConfigResource resource) {
        if (resource.fields().contains("updated_at")) {
            return " ORDER BY updated_at DESC, created_at DESC";
        }
        if (resource.fields().contains("created_at")) {
            return " ORDER BY created_at DESC";
        }
        return "";
    }

    private Map<String, Object> normalizedValues(
            ConfigResource resource, Map<String, Object> payload, boolean includeDefaults) {
        Map<String, Object> values = new LinkedHashMap<>();
        if (includeDefaults) {
            values.putAll(resource.defaultValues());
        }
        payload.forEach((field, value) -> {
            if (!resource.fields().contains(field) || List.of("id", "created_at", "updated_at").contains(field)) {
                return;
            }
            values.put(field, resource.jsonFields().contains(field) ? JsonbUtils.toJsonb(value) : value);
        });
        resource.jsonFields().forEach(field -> {
            if (values.containsKey(field) && !(values.get(field) instanceof org.postgresql.util.PGobject)) {
                values.put(field, JsonbUtils.toJsonb(values.get(field)));
            }
        });
        return values;
    }

    private Map<String, Object> mapRow(ResultSet rs, List<String> fields, ConfigResource resource)
            throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        for (String field : fields) {
            Object value = rs.getObject(field);
            if (resource.jsonFields().contains(field)) {
                value = JsonbUtils.fromJsonText(rs.getString(field));
            } else if (value instanceof OffsetDateTime dateTime) {
                value = dateTime.toString();
            } else if (value instanceof UUID uuid) {
                value = uuid.toString();
            }
            row.put(field, value);
        }
        return row;
    }
}
