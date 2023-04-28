package com.una.programming_two_first_project.model;

import org.jetbrains.annotations.NotNull;

public class ArgumentOption extends Option
{
    public final boolean isSwitch;

    public ArgumentOption(@NotNull String name, @NotNull String shortName, @NotNull String description, boolean isSwitch) {
        super(name, shortName, description);

        this.isSwitch = isSwitch;
    }
}
