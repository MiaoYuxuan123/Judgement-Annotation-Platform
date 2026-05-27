package edu.nju.jap.model;

import java.time.LocalDateTime;

public record TaskSummary(long taskId, String taskName, String description, String status, int documentCount,
                          int annotatorCount, String reviewerName, String creatorName, LocalDateTime createdAt) {
}
