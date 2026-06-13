package com.sherry.supervision.configresource;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record ConfigResource(
        String name,
        String tableName,
        String keyField,
        String nameField,
        List<String> fields,
        Set<String> jsonFields,
        Map<String, Object> defaultValues,
        boolean readOnly) {

    public ConfigResource(
            String name,
            String tableName,
            String keyField,
            String nameField,
            List<String> fields,
            Set<String> jsonFields,
            Map<String, Object> defaultValues) {
        this(name, tableName, keyField, nameField, fields, jsonFields, defaultValues, false);
    }
}
