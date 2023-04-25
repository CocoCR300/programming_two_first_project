package com.una.programming_two_first_project.model;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class Department {
    public final List<Collaborator> collaborators;
    public final long id;
    public final String name;

    public Department(long id, @NotNull String name) {
        collaborators = new ArrayList<>();
        this.id = id;
        this.name = name;
    }
}
