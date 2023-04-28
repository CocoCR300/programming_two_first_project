package com.una.programming_two_first_project.view;

import com.una.programming_two_first_project.controller.Controller;
import org.jetbrains.annotations.NotNull;

public class MainView {

    private final Controller _controller;

    public MainView(@NotNull Controller controller) {
        _controller = controller;
    }

    public String sendArgs(String[] args) {
        return _controller.resolveArgs(args);
    }
}
