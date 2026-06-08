package edu.nju.jap.model.dto.request;

import edu.nju.jap.model.entity.Proposition;
import edu.nju.jap.model.entity.Relation;

import java.util.List;

public record ArbitrationSubmit(long taskId, long dataId, List<Proposition> propositions, List<Relation> relations,
                                Object graphLayout) {
    public ArbitrationSubmit(long taskId, long dataId, List<Proposition> propositions, List<Relation> relations) {
        this(taskId, dataId, propositions, relations, null);
    }
}
