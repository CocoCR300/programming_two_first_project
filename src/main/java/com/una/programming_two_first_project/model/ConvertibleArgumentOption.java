package com.una.programming_two_first_project.model;

import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class ConvertibleArgumentOption extends Option
{
    public final Function<String, Result<Object, String>> converterFunction;

    public ConvertibleArgumentOption(@NotNull String name, @NotNull String shortName, @NotNull String description, Function<String, Result<Object, String>> converterFunction) {
        super(name, shortName, description);

        this.converterFunction = converterFunction;
    }
}