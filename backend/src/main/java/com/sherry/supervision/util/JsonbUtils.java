package com.sherry.supervision.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sherry.supervision.exception.InvalidRequestException;
import java.sql.SQLException;
import org.postgresql.util.PGobject;

public final class JsonbUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonbUtils() {
    }

    public static PGobject toJsonb(Object value) {
        try {
            PGobject object = new PGobject();
            object.setType("jsonb");
            object.setValue(OBJECT_MAPPER.writeValueAsString(value));
            return object;
        } catch (JsonProcessingException | SQLException e) {
            throw new InvalidRequestException("JSON 字段格式不正确", e);
        }
    }

    public static Object fromJsonText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            JsonNode node = OBJECT_MAPPER.readTree(value);
            return OBJECT_MAPPER.convertValue(node, Object.class);
        } catch (JsonProcessingException e) {
            return value;
        }
    }
}
