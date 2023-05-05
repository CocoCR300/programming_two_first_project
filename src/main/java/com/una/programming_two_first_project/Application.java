package com.una.programming_two_first_project;

import com.google.inject.*;
import com.google.inject.Module;
import com.una.programming_two_first_project.controller.CollaboratorController;
import com.una.programming_two_first_project.controller.EntryController;
import com.una.programming_two_first_project.controller.MainEntryController;
import com.una.programming_two_first_project.service.DataStore;
import com.una.programming_two_first_project.service.SimpleDataStore;
import com.una.programming_two_first_project.util.TokenResolver;
import com.una.programming_two_first_project.view.MainView;

import java.nio.file.Path;

public class Application implements Module, Provider<DataStore>
{
    public static void main(String[] args) {
        Injector injector = Guice.createInjector(new Application());

        // TODO: Handle dates in the ISO 8601 format: YYYY-MM-DD
        EntryController controller = injector.getInstance(EntryController.class);
        controller.registerControllerOption("collaborator", "", CollaboratorController.class);
        MainView view = new MainView(controller);
        System.out.println(view.sendArgs(args));
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(DataStore.class).toProvider(Application.class);
        binder.bind(EntryController.class).to(MainEntryController.class);
        binder.bind(TokenResolver.class);
    }

    @Override
    public SimpleDataStore get() {
        return new SimpleDataStore(Path.of(System.getenv("APPDATA"), "programming_two_first_project").toString());
    }
}