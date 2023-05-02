package com.una.programming_two_first_project.model;

import org.jetbrains.annotations.NotNull;

public class Option extends Token
{
    public final Object defaultValue;
    public final String shortName;

    public Option(@NotNull String name, @NotNull String shortName, @NotNull String description, Object defaultValue) {
        super(name, description);

        this.defaultValue = defaultValue;
        this.shortName = shortName;
    }
}
