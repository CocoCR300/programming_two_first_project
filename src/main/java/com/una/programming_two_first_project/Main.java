package com.una.programming_two_first_project;

import com.una.programming_two_first_project.controller.CollaboratorController;
import com.una.programming_two_first_project.controller.EntryController;
import com.una.programming_two_first_project.controller.MainEntryController;

public class Main {
    public static void main(String[] args) {
        // TODO: Handle dates in the ISO 8601 format: YYYY-MM-DD
        EntryController controller = new MainEntryController();
        controller.registerChildController("collaborator", "c", CollaboratorController.class);
        System.out.println(controller.resolveArgs(args));
    }
}