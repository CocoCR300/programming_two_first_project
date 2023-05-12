package com.una.programming_two_first_project.controller;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.una.programming_two_first_project.model.Command;
import com.una.programming_two_first_project.model.ControllerCommand;
import com.una.programming_two_first_project.model.Token;
import com.una.programming_two_first_project.util.TokenMapGenerator;

import java.util.*;

public class MainEntryController implements EntryController
{
    public final Token AddToken = new Token("add", "Add a new item to the system.");
    public final Token RemoveToken = new Token("delete", "Remove an item from the system.");
    public final Token EditToken = new Token("edit", "Edit an existing item in the system.");
    public final Token HelpToken = new Token("help", "Show information about the program or a specific items for an option.");
    public final Token ListToken = new Token("search", "Searches all the items in the system.");

    private final List<Token> Tokens = List.of(AddToken, EditToken, HelpToken, ListToken, RemoveToken);
    private final Map<String, ControllerCommand> ControllerCommandsMap = new HashMap<>();
    private final Map<String, Token> TokensMap = TokenMapGenerator.generateMap(Tokens);
    private final Injector injector;

    @Inject
    public MainEntryController(Injector injector) {
        this.injector = injector;
    }


    @Override
    public List<Token> getCommands() {
        return Tokens;
//        return List.of(new Token("collaborators", "c", "Manage collaborators info."),
//                new Token("projects", "p", "Manage projects info."),
//                new Token("sprints", "s", "Manage sprints info."),
//                new Token("tasks", "t",  "Manage tasks info."));
    }

    @Override
    public void registerControllerOption(String key, String description, Class<? extends ModelController> controllerType) {
        ControllerCommand option = new ControllerCommand(key, description, controllerType);
        ControllerCommandsMap.put(key, option);
    }

    @Override
    public String getHelp(String tokenName) {
        // TODO: Hardcoded version string
        String help = """
                |--------| Project Management System |--------|
                 Version: 1.0
                 Author: Oscar Rojas Alvarado (CocoCR300)

                Usage: management.exe [command(s)] [item]

                Commands:
                """;

        for (Token token : Tokens) {
            help += String.format(" %s\t%s\n", token.name, token.description);
        }

        help += "\nItems:\n";
        for (String controllerName : ControllerCommandsMap.keySet()) {
            help += String.format(" %s\n", controllerName);
        }

        return help;
    }

    @Override
    public String resolveArgs(String[] args) {
        String keyForHelp = "";

        if (args.length > 0) {
            Command helpCommand = null;
            ControllerCommand controllerCommand = null;
            Token actionToken = null;

            List<String> argsForController = new ArrayList<>();
            for (String arg : args) {
                if (TokensMap.containsKey(arg)) {
                    Token option = TokensMap.get(arg);

                    if (option == HelpToken) {
                        if (helpCommand != null) {
                            return "Help command was entered more than one time";
                        }

                        helpCommand = (Command) option;
                    } else {
                        if (actionToken != null) {
                            return "Only one command that is not the help command was expected.";
                        }

                        actionToken = option;
                    }

                } else if (ControllerCommandsMap.containsKey(arg)) {
                    if (controllerCommand != null) {
                        return "Only one item was expected.";
                    }

                    controllerCommand = ControllerCommandsMap.get(arg);
                    continue;
                }

                argsForController.add(arg);
            }

            if (actionToken != null) {
                if (controllerCommand != null) {
                    Class<? extends ModelController> controllerClass = controllerCommand.controllerClass;

                    try {
                        ModelController childController = injector.getInstance(Key.get(controllerClass));
                        return childController.resolveArgs(argsForController.toArray(String[]::new));
                    } catch (Exception ex) {
                        return "An error occurred: \n" + ex;
                    }
                } else if (helpCommand == null && args.length > 1) {
                    return String.format(
                            "Unrecognized arguments for %s command. Did you intend to put an item after this command?",
                            actionToken.name);
                }
            }
        }

        return getHelp("");
    }

    @Override
    public void selectOption(int optionIndex) {

    }
}
