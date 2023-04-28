package com.una.programming_two_first_project.controller;


import com.una.programming_two_first_project.model.ArgsCapableOption;
import com.una.programming_two_first_project.model.ArgumentOption;
import com.una.programming_two_first_project.model.Collaborator;
import com.una.programming_two_first_project.model.Option;
import com.una.programming_two_first_project.util.OptionMapGenerator;
import com.una.programming_two_first_project.util.OptionResolver;
import org.springframework.data.util.CastUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class CollaboratorController implements ArgsCapableController
{
    public final ArgsCapableOption AddOption = new ArgsCapableOption<String[]>("add", "a", "", this::add);
    public final ArgsCapableOption HelpOption = new ArgsCapableOption<String>("help", "h", "", this::getHelp);
    public final ArgumentOption IdOption = new ArgumentOption("id", "i", "", false);
    public final ArgumentOption IsActiveOption = new ArgumentOption("isActive", "j", "", true);
    public final ArgumentOption NameOption = new ArgumentOption("name", "n", "", false);
    public final ArgumentOption LastNameOption = new ArgumentOption("lastName", "m", "", false);

    private final List<Option> Options = List.of(AddOption, HelpOption);
    private final Map<String, ArgumentOption> ArgumentOptionsMap = OptionMapGenerator.generateMap(IdOption, IsActiveOption, NameOption, LastNameOption);
    private final Map<String, Option> OptionsMap = OptionMapGenerator.generateMap(Options);

    public String add(String[] args) {
        return "";
    }

    @Override
    public String getHelp(String optionName) {
        return "Collaborator management";
    }

    @Override
    public String resolveArgs(String[] args) {
        if (args.length > 0) {
            ArgsCapableOption actionOption = null;
            Map<String, Object> argsByName = new HashMap<>();

            for (int i = 0; i < args.length; ++i) {
                String arg = args[i];

                if (OptionsMap.containsKey(arg)) {
                    actionOption = (ArgsCapableOption) OptionsMap.get(arg);
                } else if (ArgumentOptionsMap.containsKey(arg) || arg.contains("-")) {
                    // TODO: Send argument options with the prefixes included, we need some way to identify them here,
                    // and that seems to be the simplest way, at least is more reliable than looking for a single dash
                    // on them
                    StringBuilder builder = new StringBuilder(arg);
                    int dashIndex = builder.indexOf("-");
                    while (dashIndex != -1) {
                        char charNextToDash = builder.charAt(dashIndex + 1);
                        builder.replace(dashIndex, dashIndex + 2, String.valueOf(Character.toUpperCase(charNextToDash)));
                        dashIndex = builder.indexOf("-");
                    }

                    ArgumentOption argumentOption = ArgumentOptionsMap.get(builder.toString());
                    argsByName.put(argumentOption.name, argumentOption.isSwitch ? true : args[i + 1]);
                }
            }

            if (actionOption != null) {
                Constructor<Collaborator> controllerConstructor = (Constructor<Collaborator>) Collaborator.class.getConstructors()[0];
                Stream<String> constructorParameterNames = Arrays.stream(controllerConstructor.getParameters()).map(Parameter::getName);

                Object[] argsForAction = constructorParameterNames.map(argsByName::get).toArray(Object[]::new);
                actionOption.function.apply(argsForAction);
            }

            String selectedOptionName = args[0];

            if (!OptionsMap.containsKey(selectedOptionName)) {
                return "Invalid option: " + args[0];
            }

            if (args.length == 1) {
                Option option = OptionsMap.get(selectedOptionName);

                if (option instanceof ArgsCapableOption argsCapableOption) {
                    // TODO: Use this to check a valid cast, maybe... What's the return value for an invalid cast?
                    String argsCapableOption1 = CastUtils.cast(option);
                    // TODO: Unchecked cast
                    return (String) argsCapableOption.function.apply("");
                }
            } else {
                for (int i = 0; i < args.length; ++i) {
                    if (OptionsMap.containsKey(OptionResolver.extractOptionName(args[0]).getValue())) {

                    }
                }
            }
        }

        return "";
    }

    @Override
    public void selectOption(int optionIndex) {

    }

    @Override
    public String getOptionInfo(String option) {
        return "";
    }
}
