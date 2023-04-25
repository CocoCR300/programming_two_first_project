package com.una.programming_two_first_project.controller;

import com.una.programming_two_first_project.model.NoArgsOption;
import com.una.programming_two_first_project.model.Option;
import com.una.programming_two_first_project.util.OptionMapGenerator;
import com.una.programming_two_first_project.util.OptionResolver;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainEntryController implements EntryController
{
    private Map<String, Class<? extends ArgsCapableController>> _childControllerTypes;

    public final Option AddOption = new Option("add", "a", "");
    public final Option EditOption = new Option("edit", "e", "");
    public final Option HelpOption = new NoArgsOption("help", "h", "", this::getHelp);
    public final Option ListOption = new Option("list", "l", "");
    public final Option RemoveOption = new Option("remove", "r", "");

    private final List<Option> Options = List.of(AddOption, EditOption, HelpOption, ListOption, RemoveOption);
    private final Map<String, Option> OptionsMap = OptionMapGenerator.generateMap(Options);

    public MainEntryController() {
        _childControllerTypes = new HashMap<>();
    }
    @Override
    public List<Option> getOptions() {
        return Options;
//        return List.of(new Option("collaborators", "c", "Manage collaborators info."),
//                new Option("projects", "p", "Manage projects info."),
//                new Option("sprints", "s", "Manage sprints info."),
//                new Option("tasks", "t",  "Manage tasks info."));
    }

    @Override
    public void registerChildController(String key, String shortKey, Class<? extends ArgsCapableController> controllerType) {
        _childControllerTypes.put(key, controllerType);
        _childControllerTypes.put(shortKey, controllerType);
    }

    @Override
    public String getHelp() {
        return "|--------| Project Management System |--------|\n" +
                "* Version: 1.0\n" +
                "* Author: Oscar Rojas Alvarado (CocoCR300)";
    }

    @Override
    public String resolveArgs(String[] args) {
        if (args.length > 0) {
            String firstOptionName = OptionResolver.extractOptionName(args[0]);

            if (!OptionsMap.containsKey(firstOptionName)) {
                return "Invalid option: " + args[0];
            }

            Option option = OptionsMap.get(firstOptionName);
            if (args.length == 1 && option instanceof NoArgsOption noArgsOption) {
                return noArgsOption.function.get();
            } else {
                String secondOptionName = OptionResolver.extractOptionName(args[1]);

                if (!secondOptionName.equals("")) {
                    Class<? extends ArgsCapableController> controllerType = _childControllerTypes.get(secondOptionName);

                    try {
                        Constructor<? extends ArgsCapableController> controllerConstructor = controllerType.getConstructor();
                        ArgsCapableController childController = controllerConstructor.newInstance();
                        return childController.resolveArgs(Arrays.stream(args).filter(optionName ->
                                !optionName.equals(args[1])).toArray(String[]::new));
                    } catch (Exception ex) {
                        return "An error occurred: \n" + ex;
                    }
                } else {
                    return "Invalid option: " + args[1];
                }
            }
        }

        return getHelp();
    }

    @Override
    public void selectOption(int optionIndex) {

    }
}
