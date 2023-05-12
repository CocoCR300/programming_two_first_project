package com.una.programming_two_first_project.model;

import com.una.programming_two_first_project.controller.ModelController;
import org.jetbrains.annotations.NotNull;

public class ControllerCommand extends Token {

    public final Class<? extends ModelController> controllerClass;

    public ControllerCommand(@NotNull String name, @NotNull String description, @NotNull Class<? extends ModelController> controllerClass) {
        super(name, description);

        this.controllerClass = controllerClass;
    }
}
