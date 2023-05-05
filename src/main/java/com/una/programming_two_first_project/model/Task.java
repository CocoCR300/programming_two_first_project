package com.una.programming_two_first_project.model;

public class Task implements Model
{
    public final Sprint sprint;
    public final String description, id, name, neededResources;

    public Task() {
        sprint = null;
        description = id = name = neededResources = "";
    }

    public Task(String id, String name, String description, Sprint sprint, String neededResources) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.sprint = sprint;
        this.neededResources = neededResources;
    }

    @Override
    public String getId() {
        return id;
    }
}
