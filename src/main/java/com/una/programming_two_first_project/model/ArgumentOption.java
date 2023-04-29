package com.una.programming_two_first_project.model;

import org.jetbrains.annotations.NotNull;

public class ArgumentOption extends Option
{
    public final Object defaultValue;

    public ArgumentOption(@NotNull String name, @NotNull String shortName, @NotNull String description, Object defaultValue) {
        super(name, shortName, description);

        this.defaultValue = defaultValue;
    }
}
