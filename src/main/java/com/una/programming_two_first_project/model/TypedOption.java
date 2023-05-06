package com.una.programming_two_first_project.model;

import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class TypedOption extends Option
{
    public final Function<String, Result<Object, String>> converterFunction;

    public TypedOption(@NotNull String name, @NotNull String shortName, @NotNull String description, boolean isRequired,
                       Function<String, Result<Object, String>> converterFunction) {
        super(name, shortName, description, isRequired);

        this.converterFunction = converterFunction;
    }
}
