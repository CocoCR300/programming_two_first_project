package com.una.programming_two_first_project.model;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class ArgsCapableOption extends Option
{
    public final Consumer function;
    public final String[] argsNames;

    public ArgsCapableOption(@NotNull String name, @NotNull String shortName, @NotNull String description,
                             @NotNull Consumer function, @NotNull String... argsNames) {
        super(name, shortName, description);
        this.function = function;
        this.argsNames = argsNames;
    }
}
