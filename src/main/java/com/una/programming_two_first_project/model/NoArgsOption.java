package com.una.programming_two_first_project.model;

import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class NoArgsOption extends Option {
    public final Supplier<String> function;

    public NoArgsOption(@NotNull String name, @NotNull String shortName, @NotNull String description, @NotNull Supplier<String> function) {
        super(name, shortName, description);

        this.function = function;
    }
}
