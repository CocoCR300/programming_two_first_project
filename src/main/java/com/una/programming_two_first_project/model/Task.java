package com.una.programming_two_first_project.model;

import com.una.programming_two_first_project.annotation.ForeignKey;
import com.una.programming_two_first_project.annotation.PrimaryKey;

public class Task implements Model
{
    public final Sprint sprint;
    public final String description, name, neededResources;
    @PrimaryKey
    public final String id;
    @ForeignKey(relationModelType = Sprint.class, relationFieldName = "sprint")
    public final String sprintId;

    public Task() {
        sprint = null;
        description = id = name = neededResources = sprintId = "";
    }

    public Task(String id, String name, String description, Sprint sprint, String neededResources) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.sprint = sprint;
        this.neededResources = neededResources;

        if (sprint != null) {
            sprintId = sprint.id;
        } else {
            sprintId = "";
        }
    }

    @Override
    public String getId() {
        return id;
    }
}
