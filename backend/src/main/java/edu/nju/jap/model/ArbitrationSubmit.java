package edu.nju.jap.model;

import java.util.List;

public record ArbitrationSubmit(long taskId, long dataId, List<Proposition> propositions, List<Relation> relations) {
}
