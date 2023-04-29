package com.una.programming_two_first_project.model;

import org.jetbrains.annotations.NotNull;

public class SwitchArgumentOption extends ArgumentOption {

    public SwitchArgumentOption(@NotNull String name, @NotNull String shortName, @NotNull String description) {
        super(name, shortName, description, false);
    }
}
