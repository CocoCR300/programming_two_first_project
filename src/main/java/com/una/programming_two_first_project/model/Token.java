package com.una.programming_two_first_project.model;

import org.jetbrains.annotations.NotNull;

public class Token
{
    public final String description, name;

    public Token(@NotNull String name, @NotNull String description) {
        this.description = description;
        this.name = name;
    }
}
