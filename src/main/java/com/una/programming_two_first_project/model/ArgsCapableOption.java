package com.una.programming_two_first_project.model;

import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class ArgsCapableOption<TArg> extends Option
{
    public final Function<TArg, String> function;
    public final String[] argsNames;

    public ArgsCapableOption(@NotNull String name, @NotNull String shortName, @NotNull String description,
                             @NotNull Function<TArg, String> function, @NotNull String... argsNames) {
        super(name, shortName, description);
        this.function = function;
        this.argsNames = argsNames;
    }
}
