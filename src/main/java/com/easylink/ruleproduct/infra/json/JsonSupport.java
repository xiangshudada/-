package com.easylink.ruleproduct.infra.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class JsonSupport {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<String>> LIST_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public JsonSupport(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> readMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid json map: " + json, ex);
        }
    }

    public List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, LIST_TYPE);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid json list: " + json, ex);
        }
    }

    public BigDecimal decimalValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        if (value instanceof String text && !text.isBlank()) {
            return new BigDecimal(text);
        }
        return null;
    }

    public String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to serialize value to json", ex);
        }
    }
}
