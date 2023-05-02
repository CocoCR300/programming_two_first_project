package com.una.programming_two_first_project.model;

import com.una.programming_two_first_project.controller.ArgsCapableController;
import org.jetbrains.annotations.NotNull;

public class ControllerCommand extends Token {

    public final Class<? extends ArgsCapableController> controllerType;

    public ControllerCommand(@NotNull String name, @NotNull String description, @NotNull Class<? extends ArgsCapableController> controllerType) {
        super(name, description);

        this.controllerType = controllerType;
    }
}
