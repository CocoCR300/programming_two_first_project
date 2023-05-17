package com.una.programming_two_first_project;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.una.programming_two_first_project.controller.EntryController;
import com.una.programming_two_first_project.controller.MainEntryController;
import com.una.programming_two_first_project.formatter.*;
import com.una.programming_two_first_project.data_store.DataStore;
import com.una.programming_two_first_project.data_store.SimpleDataStore;
import com.una.programming_two_first_project.model.Collaborator;
import com.una.programming_two_first_project.util.TokenResolver;
import com.una.programming_two_first_project.view.MainView;
import com.una.programming_two_first_project.view.View;

import java.io.Console;
import java.nio.file.Path;

public class DefaultApplicationModule implements Module
{
    @Override
    public void configure(Binder binder) {
        binder.bind(CollaboratorFormatter.class);
        binder.bind(Console.class).toProvider(ConsoleProvider.class);
        binder.bind(DataStore.class).toProvider(DataStoreProvider.class);
        binder.bind(DepartmentFormatter.class);
        binder.bind(CollaboratorFormatter.class);
        binder.bind(ProjectFormatter.class);
        binder.bind(SprintFormatter.class);
        binder.bind(TaskFormatter.class);
        binder.bind(EntryController.class).to(MainEntryController.class).asEagerSingleton();
        binder.bind(View.class).to(MainView.class);
        binder.bind(TokenResolver.class);
    }

    public static class DataStoreProvider implements Provider<DataStore>
    {
        @Override
        @Singleton
        public SimpleDataStore get() {
            return new SimpleDataStore(Path.of(System.getenv("APPDATA"), "programming_two_first_project").toString());
        }
    }

    public static class ConsoleProvider implements Provider<Console>
    {
        @Override
        public Console get() {
            return System.console();
        }
    }
}
