package com.una.programming_two_first_project.model;

import com.una.programming_two_first_project.data_store.Model;
import com.una.programming_two_first_project.data_store.annotation.InverseProperty;
import com.una.programming_two_first_project.data_store.annotation.PrimaryKey;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Project implements Model
{
    @InverseProperty(relationModelClass = Sprint.class,
            relationModelRelationFieldName = "project",
            relationModelRelationIdFieldName = "projectId")
    public final transient List<Sprint> sprints;
    public final LocalDate endDate, startDate;
    @PrimaryKey public final short number;
    public final String name;

    public Project() {
        number = 0;
        endDate = startDate = null;
        name = "";
        sprints = new ArrayList<>(0);
    }

    public Project(short number, String name, LocalDate startDate, LocalDate endDate) {
        this.number = number;
        this.name = name;
        this.endDate = endDate;
        this.startDate = startDate;
        this.sprints = new ArrayList<>(0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Project project = (Project) o;
        return number == project.number;
    }

    @Override
    public int hashCode() {
        return Objects.hash(number);
    }

    public String getCode() {
        return String.format("%02d", number);
    }

    @Override
    public String getId() {
        return getCode();
    }
}
