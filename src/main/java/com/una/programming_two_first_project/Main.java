package com.una.programming_two_first_project;

import com.una.programming_two_first_project.controller.CollaboratorController;
import com.una.programming_two_first_project.controller.EntryController;
import com.una.programming_two_first_project.controller.MainEntryController;
import com.una.programming_two_first_project.model.Collaborator;
import com.una.programming_two_first_project.service.DataStore;
import com.una.programming_two_first_project.service.SimpleDataStore;
import com.una.programming_two_first_project.view.MainView;

public class Main
{
    public static void main(String[] args) {
        DataStore store = new SimpleDataStore(System.getenv("APPDATA"));
        var entities = store.getAll(Collaborator.class.getSimpleName().toLowerCase());

        // TODO: Handle dates in the ISO 8601 format: YYYY-MM-DD
        EntryController controller = new MainEntryController();
        controller.registerControllerOption("collaborator", "", CollaboratorController.class);
        MainView view = new MainView(controller);
        System.out.println(view.sendArgs(args));
    }
}