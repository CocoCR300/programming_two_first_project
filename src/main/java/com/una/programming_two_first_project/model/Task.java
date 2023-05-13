package com.una.programming_two_first_project.model;

import com.una.programming_two_first_project.annotation.ForeignKey;
import com.una.programming_two_first_project.annotation.PrimaryKey;

public class Task implements Model
{
    public final transient Collaborator collaborator;
    public final transient Sprint sprint;
    @ForeignKey(relationModelClass = Collaborator.class, relationFieldName = "collaborator")
    public final String collaboratorId;
    public final String description, name, neededResources;
    @PrimaryKey
    public final String id;
    @ForeignKey(relationModelClass = Sprint.class, relationFieldName = "sprint")
    public final String sprintId;

    public Task() {
        collaborator = null;
        sprint = null;
        collaboratorId = description = id = name = neededResources = sprintId = "";
    }

    public Task(String id, String name, String description, Collaborator collaborator, Sprint sprint, String neededResources) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.collaborator = collaborator;
        this.sprint = sprint;
        this.neededResources = neededResources;

        if (collaborator != null) {
            collaboratorId = collaborator.id;
        } else {
            collaboratorId = "";
        }

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
