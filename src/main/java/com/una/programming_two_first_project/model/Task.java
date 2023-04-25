package com.una.programming_two_first_project.model;

public class Task {

    public final long id;
    public final Sprint sprint;
    public final String description, name, neededResources;

    public Task(long id, String name, String description, Sprint sprint, String neededResources) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.sprint = sprint;
        this.neededResources = neededResources;
    }
}
