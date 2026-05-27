package edu.nju.jap.model.entity;

public record Proposition(String propId, int sequenceNo, int startPos, int endPos, String text, String tag) {
}
