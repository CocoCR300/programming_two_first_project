package com.una.programming_two_first_project.view;

import com.google.inject.Inject;
import com.una.programming_two_first_project.controller.Controller;
import com.una.programming_two_first_project.controller.EntryController;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.Console;
import java.util.Scanner;

public class MainView implements View
{
    private final Console console;
    private final EntryController controller;
    private final Scanner scanner;

    @Inject
    public MainView(@Nullable Console console, @NotNull EntryController controller) {
        this.console = console;
        this.controller = controller;

        if (console == null) {
            scanner = new Scanner(System.in);
        } else {
            scanner = null;
        }
    }

    public void sendArgs(String[] args) {
        String output = controller.resolveArgs(args);
        show(output);
    }

    @Override
    public String askForInput(String message) {
        show(message);
        return console != null ? console.readLine() : scanner.nextLine();
    }

    @Override
    public void show(String message) {
        if (console != null) {
            console.printf(message);
        } else {
            System.out.print(message);
        }
    }
}
