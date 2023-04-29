package com.una.programming_two_first_project.controller;

import com.una.programming_two_first_project.model.ArgsCapableOption;
import com.una.programming_two_first_project.model.ControllerOption;
import com.una.programming_two_first_project.model.Option;
import com.una.programming_two_first_project.util.OptionMapGenerator;
import com.una.programming_two_first_project.util.OptionResolver;

import java.lang.reflect.Constructor;
import java.util.*;

public class MainEntryController implements EntryController
{
    public final Option AddOption = new Option("add", "a", "Add a new item to the system.");
    public final Option EditOption = new Option("edit", "e", "Edit an existing item in the system.");
    public final Option HelpOption = new Option("help", "h", "Show information about the program or a specific items for an option.");
    public final Option ListOption = new Option("list", "l", "List all the items in the system.");
    public final Option RemoveOption = new Option("remove", "r", "Remove an item from the system.");

    private final List<Option> Options = List.of(AddOption, EditOption, HelpOption, ListOption, RemoveOption);
    private final Map<String, ControllerOption> ControllerOptionsMap = new HashMap<>();
    private final Map<String, Option> OptionsMap = OptionMapGenerator.generateMap(Options);

    @Override
    public List<Option> getOptions() {
        return Options;
//        return List.of(new Option("collaborators", "c", "Manage collaborators info."),
//                new Option("projects", "p", "Manage projects info."),
//                new Option("sprints", "s", "Manage sprints info."),
//                new Option("tasks", "t",  "Manage tasks info."));
    }

    @Override
    public void registerControllerOption(String key, String shortKey, String description, Class<? extends ArgsCapableController> controllerType) {
        ControllerOption option = new ControllerOption(key, shortKey, description, controllerType);
        ControllerOptionsMap.put(key, option);
        ControllerOptionsMap.put(shortKey, option);
    }

    @Override
    public String getHelp(String optionName) {
        String help = "";

        help = "|--------| Project Management System |--------|\n" +
                " Version: 1.0\n" +
                " Author: Oscar Rojas Alvarado (CocoCR300)\n\n" +
                "Usage: management.exe [options] [item] [arguments]\n\n" +
                "Options:\n";

        for (Option option : Options) {
            help += " -" + option.shortName + "|--" + option.name + "\t" + option.description + "\n";
        }

        help += "\nItems:\n";

        for (String controllerName : ControllerOptionsMap.keySet()) {
            // TODO: What if it's unordered? Let's guarantee an ordered collection or find another way to do this
            String prefix, separator;

            if (controllerName.length() == 1) {
                prefix = "-";
                separator = "|";
            } else {
                prefix = "--";
                separator = "\n";
            }

            help += prefix + controllerName + separator;
        }

        return help;
    }

    @Override
    public String resolveArgs(String[] args) {
        String keyForHelp = "";

        if (args.length > 0) {
            ArgsCapableOption helpOption = null;
            ControllerOption controllerOption = null;
            Option actionOption = null;

            List<String> argsForController = new ArrayList<>();
            for (String arg : args) {
                var result = OptionResolver.extractOptionName(arg);

                if (result.getKey()) {
                    String optionName = result.getValue();
                    Option option = OptionsMap.get(optionName);

                    if (option == HelpOption) {
                        helpOption = (ArgsCapableOption) option;
//                        argsForController.add(arg);
                    } else if (option != null) {
                        actionOption = option;
//                        argsForController.add(arg);
                    } else if (ControllerOptionsMap.containsKey(optionName)) {
                        controllerOption = ControllerOptionsMap.get(optionName);
                        continue;
                    }
                    /* else {
                        argsForController.add(arg);
//                        return "Invalid option: " + arg;
                    } */
                } /*else {
                    argsForController.add(arg);
                } */

                argsForController.add(arg);
            }

            if (actionOption != null) {
                if (controllerOption != null) {
                    Class<? extends ArgsCapableController> controllerType = controllerOption.controllerType;

                    try {
                        Constructor<? extends ArgsCapableController> controllerConstructor = controllerType.getConstructor();
                        ArgsCapableController childController = controllerConstructor.newInstance();
                        return childController.resolveArgs(argsForController.toArray(String[]::new));
                    } catch (Exception ex) {
                        return "An error occurred: \n" + ex;
                    }
                } else if (helpOption == null && args.length > 1) {
                    return "Unrecognized arguments for " + actionOption.name + " option. Did you intend to put an item after this option?";
                }
            }
        }

        return getHelp("");
    }

    @Override
    public void selectOption(int optionIndex) {

    }
}
