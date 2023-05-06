package com.una.programming_two_first_project.model;

import org.jetbrains.annotations.NotNull;

public class Option extends Token
{
    public final boolean isRequired;
    public final String shortName;

    public Option(@NotNull String name, @NotNull String shortName, @NotNull String description, boolean isRequired) {
        super(name, description);

        this.isRequired = isRequired;
        this.shortName = shortName;
    }
}
