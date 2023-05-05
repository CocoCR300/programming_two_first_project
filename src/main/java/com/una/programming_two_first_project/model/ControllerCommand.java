package com.una.programming_two_first_project.model;

import com.una.programming_two_first_project.controller.ArgsCapableController;
import org.jetbrains.annotations.NotNull;

public class ControllerCommand extends Token {

    public final Class<? extends ArgsCapableController> controllerClass;

    public ControllerCommand(@NotNull String name, @NotNull String description, @NotNull Class<? extends ArgsCapableController> controllerClass) {
        super(name, description);

        this.controllerClass = controllerClass;
    }
}
