package com.una.programming_two_first_project.model;

import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class ConvertibleArgumentOption<T> extends Option
{
    public final Function<String, Result<T, String>> converterFunction;

    public ConvertibleArgumentOption(@NotNull String name, @NotNull String shortName, @NotNull String description, Function<String, Result<T, String>> converterFunction) {
        super(name, shortName, description);

        this.converterFunction = converterFunction;
    }

    public Result<T, String> validate(String argument) {
        return converterFunction.apply(argument);
    }
}
