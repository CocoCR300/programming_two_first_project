package com.una.programming_two_first_project.model;

import com.una.programming_two_first_project.annotation.InverseProperty;
import com.una.programming_two_first_project.annotation.PrimaryKey;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class Department implements Model {
    @InverseProperty(relationModelClass = Collaborator.class,
            relationModelRelationFieldName = "department",
            relationModelRelationIdFieldName = "departmentId")
    public final transient List<Collaborator> collaborators;
    @PrimaryKey
    public final String id;
    public final String name;

    public Department() {
        collaborators = new ArrayList<>();
        id = name = "";
    }

    public Department(String id, @NotNull String name) {
        collaborators = new ArrayList<>();
        this.id = id;
        this.name = name;
    }

    @Override
    public String getId() {
        return id;
    }
}
