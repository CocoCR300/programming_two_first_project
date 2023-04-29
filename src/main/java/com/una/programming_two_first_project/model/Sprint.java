package com.una.programming_two_first_project.model;

import org.jetbrains.annotations.NotNull;

import java.time.OffsetDateTime;

public class Sprint implements Model {
    public final OffsetDateTime endDateTime, startDateTime;
    public final Project project;
    public final String number, identifier;

    public Sprint(Project project, @NotNull String number, @NotNull OffsetDateTime startDateTime,
                  @NotNull OffsetDateTime endDateTime) {
        this.project = project;
        this.number = number;
        this.endDateTime = endDateTime;
        this.startDateTime = startDateTime;

        identifier = project.code + number;
    }
}
