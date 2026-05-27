package edu.nju.jap.model.dto.response;

import edu.nju.jap.model.entity.DocumentItem;
import edu.nju.jap.model.entity.GuideConfig;

import java.util.List;

public record TaskDetail(TaskSummary summary, List<DocumentItem> documents, List<UserVO> annotators, UserVO reviewer,
                         GuideConfig configSnapshot) {
}
