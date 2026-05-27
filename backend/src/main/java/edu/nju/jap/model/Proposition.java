package edu.nju.jap.model;

public record Proposition(String propId, int sequenceNo, int startPos, int endPos, String text, String tag) {
}
