package com.una.programming_two_first_project.model;

import org.jetbrains.annotations.NotNull;

public class Option extends Token
{
    public final String shortName;

    public Option(@NotNull String name, @NotNull String shortName, @NotNull String description) {
        super(name, description);

        this.shortName = shortName;
    }
}
