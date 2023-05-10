package com.una.programming_two_first_project.model;

import org.jetbrains.annotations.NotNull;

import java.time.OffsetDateTime;

public class Sprint implements Model
{
    public final OffsetDateTime endDateTime, startDateTime;
    public final Project project;
    public final String id, number;

    public Sprint() {
        endDateTime = startDateTime = null;
        project = null;
        id = number = "";
    }

    public Sprint(Project project, @NotNull String number, @NotNull OffsetDateTime startDateTime,
                  @NotNull OffsetDateTime endDateTime) {
        this.project = project;
        this.number = number;
        this.endDateTime = endDateTime;
        this.startDateTime = startDateTime;

        id = formatId(project, number);
    }

    @Override
    public String getId() {
        return id;
    }

    public static String formatId(Project project, @NotNull String number) {
        return String.format("P%s-%s", (project != null ? project.code : "??"), !number.isEmpty() ? number : "??");
    }
}
