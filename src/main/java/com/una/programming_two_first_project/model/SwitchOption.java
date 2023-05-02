package com.una.programming_two_first_project.model;

import org.jetbrains.annotations.NotNull;

public class SwitchOption extends Option {

    public SwitchOption(@NotNull String name, @NotNull String shortName, @NotNull String description) {
        super(name, shortName, description, false);
    }
}
