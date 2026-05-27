package edu.nju.jap.model;

import java.util.List;

public record AnnotationSubmit(long taskId, long dataId, List<Proposition> propositions, List<Relation> relations,
                             boolean isDraft) {
}
