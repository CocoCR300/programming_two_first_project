package com.una.programming_two_first_project.controller;

import com.google.inject.Inject;
import com.una.programming_two_first_project.model.*;
import com.una.programming_two_first_project.service.DataStore;
import com.una.programming_two_first_project.util.ArgsValidator;
import com.una.programming_two_first_project.util.TokenResolver;

import java.util.List;
import java.util.Map;

public class DepartmentController extends BaseModelController<Department>
{
    private final Option commandNameOption = new Option("command-name", "n", "");
    private final Command<String> helpCommand = new Command<>("help", "", this::getHelp,
            new Option[] { commandNameOption }, null);

    private final Option collaboratorIds = new ConvertibleArgumentOption("collaborator-ids", "c", "",
            ArgsValidator::isCommaSeparatedList);
    private final Option idOption = new ConvertibleArgumentOption("id", "i", "",
            ArgsValidator::isNotBlank);
    private final Option nameOption = new ConvertibleArgumentOption("name", "n", "",
            ArgsValidator::isNotBlank);
    private final Command<Map<String, Object>> addCommand = new Command<>("add", "", this::add,
            new Option[] { nameOption },
            new Option[] { collaboratorIds });

    private final Command<String> deleteCommand = new Command<>("delete", "", this::delete,
            new Option[]{ idOption }, null);

    private final Option addCollaboratorsOption = new SwitchOption("remove-collaborators", "r", "");
    private final Option removeCollaboratorsOption = new SwitchOption("remove-collaborators", "r", "");
//    private final Command<Map<String, Object>> editCommand = new Command<>("edit", "", this::edit,
//            new Option[]{ idOption },
//            new Option[] { nameOption, emailAddressOption, collaboratorIds, removeCollaboratorsOption });
//    private final Command<Map<String, String>> searchCommand = new Command<>("search", "", this::search,
//            null, new Option[] { idOption });

    private final List<Command> commands = List.of(addCommand);

    @Inject
    public DepartmentController(DataStore dataStore, TokenResolver tokenResolver) {
        super(Department.class, dataStore, tokenResolver);
    }

    @Override
    public Command getAddCommand() {
        return addCommand;
    }

    @Override
    public Command<String> getHelpCommand() {
        return helpCommand;
    }

    @Override
    public List<Command> getCommands() {
        return commands;
    }

    @Override
    public String getCommandInfo(String command) {
        return null;
    }

    @Override
    public String getHelp(String tokenName) {
        return null;
    }

    @Override
    public void selectOption(int optionIndex) {

    }
}
