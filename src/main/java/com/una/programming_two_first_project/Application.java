package com.una.programming_two_first_project;

import com.google.gson.Gson;
import com.google.inject.*;
import com.una.programming_two_first_project.controller.*;
import com.una.programming_two_first_project.view.MainView;

import java.time.LocalDate;

public class Application
{
    public static void main(String[] args) {
        Injector injector = Guice.createInjector(new DefaultApplicationModule());

//        DataStore dataStore = injector.getInstance(DataStore.class);
//        dataStore.delete(Department.class, "1");
//        Collaborator c = dataStore.get(Collaborator.class, "899103014").unwrap().get();
//        Department scndDepartment = dataStore.get(Department.class, "3").unwrap().get();
//        Collaborator n = new Collaborator(c.id, c.name, c.lastName, c.telephoneNumber, c.emailAddress, scndDepartment, c.isActive);
//        dataStore.update(n, false);

        // TODO: Handle dates in the ISO 8601 format: YYYY-MM-DD
        EntryController controller = injector.getInstance(EntryController.class);
        controller.registerControllerOption("collaborator", "", CollaboratorController.class);
        controller.registerControllerOption("department", "", DepartmentController.class);
        controller.registerControllerOption("project", "", ProjectController.class);
        controller.registerControllerOption("sprint", "", SprintController.class);
        controller.registerControllerOption("task", "", TaskController.class);

        MainView view = injector.getInstance(MainView.class);
        view.sendArgs(args);
    }
}