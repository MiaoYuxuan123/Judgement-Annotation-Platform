package edu.nju.jap.service.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class GraphLayoutJsonCodec {
    private final ObjectMapper objectMapper;

    public GraphLayoutJsonCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String toJson(Object graphLayout) {
        if (graphLayout == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(graphLayout);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("布局数据格式无效", ex);
        }
    }

    public Object fromJson(String layoutJson) {
        if (layoutJson == null || layoutJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(layoutJson, Object.class);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }
}
