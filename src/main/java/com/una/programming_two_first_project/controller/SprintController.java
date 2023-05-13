package com.una.programming_two_first_project.controller;

import com.una.programming_two_first_project.model.Command;
import com.una.programming_two_first_project.model.Sprint;
import com.una.programming_two_first_project.service.DataStore;
import com.una.programming_two_first_project.formatter.Formatter;
import com.una.programming_two_first_project.util.TokenResolver;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SprintController extends BaseModelController<Sprint>
{
    public SprintController(@NotNull Class<Sprint> modelClass, @NotNull DataStore dataStore, @NotNull Formatter<Sprint> formatter, @NotNull EntryController entryController, @NotNull TokenResolver tokenResolver) {
        super(modelClass, dataStore, formatter, entryController, tokenResolver);
    }

    @Override
    protected Command getAddCommand() {
        return null;
    }

    @Override
    protected Command getHelpCommand() {
        return null;
    }

    @Override
    public String getHelp(String tokenName) {
        return null;
    }

    @Override
    public List<Command> getCommands() {
        return null;
    }

    @Override
    public String getCommandInfo(String command) {
        return null;
    }

    @Override
    public void selectOption(int optionIndex) {

    }
}
