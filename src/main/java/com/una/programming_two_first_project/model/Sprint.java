package com.una.programming_two_first_project.model;

import com.una.programming_two_first_project.data_store.Model;
import com.una.programming_two_first_project.data_store.annotation.ForeignKey;
import com.una.programming_two_first_project.data_store.annotation.InverseProperty;
import com.una.programming_two_first_project.data_store.annotation.PrimaryKey;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Sprint implements Model
{
    @InverseProperty(relationModelClass = Task.class, relationModelRelationFieldName = "sprintId",
                     relationModelRelationIdFieldName = "sprint")
    public final transient List<Task> tasks;
    public final LocalDate endDate, startDate;
    public final transient Project project;
    @PrimaryKey public final String id;
    public final short number;
    @ForeignKey(relationModelClass = Project.class, relationFieldName = "project")
    public final String projectId;

    public Sprint() {
        number = 0;
        endDate = startDate = null;
        project = null;
        id = projectId = "";
        tasks = new ArrayList<>(0);
    }

    public Sprint(@NotNull String id, Project project, short number, @NotNull LocalDate startDate,
                  @NotNull LocalDate endDate) {
        this.id = id;
        this.project = project;
        this.number = number;
        this.endDate = endDate;
        this.startDate = startDate;
        tasks = new ArrayList<>(0);

        if (project != null) {
            projectId = project.getId();
        } else {
            projectId = "";
        }
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

    public String getCode() {
        return String.format("P%s-%s", project != null ? project.getCode() : "??", String.format("%02d", number));
    }

    @Override
    public String getId() {
        return id;
    }
}
