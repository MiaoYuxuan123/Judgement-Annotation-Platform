package edu.nju.jap.model.dto.request;

import edu.nju.jap.model.entity.Proposition;
import edu.nju.jap.model.entity.Relation;

import java.util.List;

public record AnnotationSubmit(long taskId, long dataId, List<Proposition> propositions, List<Relation> relations,
                               boolean isDraft) {
}
