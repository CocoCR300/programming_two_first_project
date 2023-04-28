package com.una.programming_two_first_project.model;

import com.una.programming_two_first_project.controller.ArgsCapableController;
import org.jetbrains.annotations.NotNull;

public class ControllerOption extends Option {

    public final Class<? extends ArgsCapableController> controllerType;

    public ControllerOption(@NotNull String name, @NotNull String shortName, @NotNull String description, @NotNull Class<? extends ArgsCapableController> controllerType) {
        super(name, shortName, description);

        this.controllerType = controllerType;
    }
}
