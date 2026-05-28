package edu.nju.jap.model.entity;

import java.util.ArrayList;
import java.util.List;

public record Relation(String relId, String type, String source, String target, List<String> members) {
    public Relation(String relId, String type, String source, String target) {
        this(relId, type, source, target, List.of(source, target));
    }

    public Relation {
        if (members == null || members.isEmpty()) {
            List<String> fallback = new ArrayList<>();
            if (source != null && !source.isBlank()) {
                fallback.add(source);
            }
            if (target != null && !target.isBlank()) {
                fallback.add(target);
            }
            members = fallback;
        }
    }
}
