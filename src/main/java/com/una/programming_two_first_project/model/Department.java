package com.una.programming_two_first_project.model;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class Department implements Model {
    public final transient List<Collaborator> collaborators;
    public final String id, name;

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
