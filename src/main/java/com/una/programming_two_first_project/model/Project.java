package com.una.programming_two_first_project.model;

import com.una.programming_two_first_project.annotation.InverseProperty;
import com.una.programming_two_first_project.annotation.PrimaryKey;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Project implements Model
{
    @InverseProperty(relationModelClass = Sprint.class,
            relationModelRelationFieldName = "project",
            relationModelRelationIdFieldName = "projectId")
    public final transient List<Sprint> sprints;
    public final OffsetDateTime endDateTime, startDateTime;
    @PrimaryKey(autogenerate = false)
    public final String code;
    public final String name;

    public Project() {
        endDateTime = startDateTime = null;
        code = name = "";
        sprints = new ArrayList<>(0);
    }

    public Project(String code, String name, OffsetDateTime startDateTime, OffsetDateTime endDateTime) {
        this.code = code;
        this.name = name;
        this.endDateTime = endDateTime;
        this.startDateTime = startDateTime;
        this.sprints = new ArrayList<>(0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Project project = (Project) o;
        return Objects.equals(code, project.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }

    @Override
    public String getId() {
        return code;
    }
}
