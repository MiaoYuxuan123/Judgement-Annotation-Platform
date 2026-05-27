package edu.nju.jap.model;

import java.util.List;
import java.util.Map;

public record TaskDetail(TaskSummary summary, List<DocumentItem> documents, List<Map<String, Object>> annotators,
                         Map<String, Object> reviewer, GuideConfig configSnapshot) {
}
