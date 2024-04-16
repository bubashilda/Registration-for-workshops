package io.project.RegistrationForWorkshops.model;

import lombok.Getter;

public enum Subject {
    LA("ЛА", "LA"),
    MA("МА", "MA"),
    AnG("АиГ", "AnG");
    private final String implementation;
    @Getter
    private final String callback;

    Subject(String s, String call) {
        implementation = s;
        callback = call;
    }

    @Override
    public String toString() {
        return implementation;
    }
}
