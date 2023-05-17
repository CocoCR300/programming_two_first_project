package com.una.programming_two_first_project.controller;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Singleton;
import com.una.programming_two_first_project.model.Command;
import com.una.programming_two_first_project.model.ControllerCommand;
import com.una.programming_two_first_project.model.Token;
import com.una.programming_two_first_project.util.TokenMapGenerator;
import com.una.programming_two_first_project.view.View;

import java.util.*;

@Singleton
public class MainEntryController implements EntryController
{
    public final Token addToken = new Token("add", "Add a new item to the system.");
    public final Token removeToken = new Token("delete", "Remove an item from the system.");
    public final Token editToken = new Token("edit", "Edit an existing item in the system.");
    public final Token helpToken = new Token("help", "Show information about the program or a specific items for an option.");
    public final Token listToken = new Token("list", "List all items in the system.");
    public final Token searchToken = new Token("search", "Search for an item in the system.");

    private final List<Token> tokens = List.of(addToken, editToken, helpToken, listToken, searchToken, removeToken);
    private final Map<String, ControllerCommand> controllerCommandsMap = new HashMap<>();
    private final Map<String, Token> tokensMap = TokenMapGenerator.generateMap(tokens);
    private final Injector injector;
    private final View view;

    @Inject
    public MainEntryController(Injector injector, View view) {
        this.injector = injector;
        this.view = view;
    }


    @Override
    public Token[] getCommands() {
        return tokens.toArray(Token[]::new);
    }

    @Override
    public String askForInput(String message) {
        return view.askForInput(message);
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

        for (Token token : tokens) {
            help += String.format(" %s\t%s\n", token.name, token.description);
        }

        help += "\nItems:\n";
        for (String controllerName : controllerCommandsMap.keySet()) {
            help += String.format(" %s\n", controllerName);
        }

        return help;
    }

    @Override
    public String resolveArgs(String[] args) {
        if (args.length > 0) {
            Token helpCommand = null;
            ControllerCommand controllerCommand = null;
            Token actionToken = null;

            List<String> argsForController = new ArrayList<>();
            for (String arg : args) {
                if (tokensMap.containsKey(arg)) {
                    Token option = tokensMap.get(arg);

                    if (option == helpToken) {
                        if (helpCommand != null) {
                            return "Help command was entered more than one time";
                        }

                        helpCommand = option;
                    } else {
                        if (actionToken != null) {
                            return "Only one command that is not the help command was expected.";
                        }

                        actionToken = option;
                    }

                } else if (controllerCommandsMap.containsKey(arg)) {
                    if (controllerCommand != null) {
                        return "Only one item was expected.";
                    }

                    controllerCommand = controllerCommandsMap.get(arg);
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
                        ex.printStackTrace();
                        return "";
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
    public void registerControllerOption(String key, String description, Class<? extends ModelController> controllerType) {
        ControllerCommand option = new ControllerCommand(key, description, controllerType);
        controllerCommandsMap.put(key, option);
    }
}
