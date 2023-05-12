package com.una.programming_two_first_project.model;

import com.una.programming_two_first_project.annotation.ForeignKey;
import com.una.programming_two_first_project.annotation.PrimaryKey;
import org.jetbrains.annotations.NotNull;

import java.time.OffsetDateTime;

public class Sprint implements Model
{
    public final OffsetDateTime endDateTime, startDateTime;
    public final Project project;
    public final String number;
    @PrimaryKey(autogenerate = false, composerMethodName = "formatId", composerAttributeNames = {"projectId", "id"})
    public final String id;
    @ForeignKey(relationModelType = Project.class, relationFieldName = "project")
    public final String projectId;

    public Sprint() {
        endDateTime = startDateTime = null;
        project = null;
        id = number = projectId = "";
    }

    public Sprint(Project project, @NotNull String number, @NotNull OffsetDateTime startDateTime,
                  @NotNull OffsetDateTime endDateTime) {
        this.project = project;
        this.number = number;
        this.endDateTime = endDateTime;
        this.startDateTime = startDateTime;

        if (project != null) {
            projectId = project.code;
        } else {
            projectId = "";
        }

        id = formatId(projectId, number);
    }

    @Override
    public String getId() {
        return id;
    }

    public static String formatId(String projectId, @NotNull String number) {
        return String.format("P%s-%s", (!projectId.isEmpty() ? projectId : "??"), !number.isEmpty() ? number : "??");
    }
}
