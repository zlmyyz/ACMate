package com.itnoduck.acmate.training.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum ProblemStatus {
    NOT_STARTED(0),
    CHALLENGING(1),
    ACCEPTED(2);

    private final int code;

    ProblemStatus(int code) { this.code = code; }

    public int getCode() { return code; }

    private static final Map<Integer, ProblemStatus> MAP = Arrays.stream(values())
            .collect(Collectors.toMap(ProblemStatus::getCode, Function.identity()));

    public static ProblemStatus fromCode(int code) {
        ProblemStatus s = MAP.get(code);
        if (s == null) throw new IllegalArgumentException("Unknown status code: " + code);
        return s;
    }

    public static ProblemStatus fromString(String s) {
        if (s == null) return null;
        return switch (s.toUpperCase()) {
            case "NOT_STARTED" -> NOT_STARTED;
            case "CHALLENGING" -> CHALLENGING;
            case "ACCEPTED" -> ACCEPTED;
            default -> throw new IllegalArgumentException("Unknown status: " + s);
        };
    }
}
