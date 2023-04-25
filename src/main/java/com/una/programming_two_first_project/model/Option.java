package com.una.programming_two_first_project.model;

import org.jetbrains.annotations.NotNull;

public class Option
{
    public final String description, name, shortName;

    public Option(@NotNull String name, @NotNull String shortName, @NotNull String description) {
        this.description = description;
        this.name = name;
        this.shortName = shortName;
    }
}
