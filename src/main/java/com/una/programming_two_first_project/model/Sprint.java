package com.una.programming_two_first_project.model;

import com.una.programming_two_first_project.annotation.ForeignKey;
import com.una.programming_two_first_project.annotation.InverseProperty;
import com.una.programming_two_first_project.annotation.PrimaryKey;
import org.jetbrains.annotations.NotNull;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Sprint implements Model
{
    @InverseProperty(relationModelClass = Task.class, relationModelRelationFieldName = "sprintId",
                     relationModelRelationIdFieldName = "sprint")
    public final List<Task> tasks;
    public final OffsetDateTime endDateTime, startDateTime;
    public final Project project;
    @PrimaryKey public final String internalId;
    public final String number;
    public final String id;
    @ForeignKey(relationModelClass = Project.class, relationFieldName = "project")
    public final String projectId;

    public Sprint() {
        endDateTime = startDateTime = null;
        project = null;
        id = internalId = number = projectId = "";
        tasks = new ArrayList<>(0);
    }

    public Sprint(@NotNull String id, Project project, @NotNull String number, @NotNull OffsetDateTime startDateTime,
                  @NotNull OffsetDateTime endDateTime) {
        this.internalId = id;
        this.project = project;
        this.number = number;
        this.endDateTime = endDateTime;
        this.startDateTime = startDateTime;
        tasks = new ArrayList<>(0);

        if (project != null) {
            projectId = project.code;
        } else {
            projectId = "";
        }

        this.id = formatId(projectId, number);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Sprint sprint = (Sprint) o;
        return Objects.equals(id, sprint.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String getId() {
        return id;
    }

    public static String formatId(String projectId, @NotNull String number) {
        final String defaultPlaceholder = "??";
        return String.format("P%s-%s", (!projectId.isEmpty() ? projectId : defaultPlaceholder), !number.isEmpty() ? number : defaultPlaceholder);
    }
}
