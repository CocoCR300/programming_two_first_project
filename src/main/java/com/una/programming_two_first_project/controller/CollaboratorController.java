package com.una.programming_two_first_project.controller;


import com.una.programming_two_first_project.model.*;
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
    public final ArgsCapableOption<String[]> AddOption = new ArgsCapableOption<>("add", "a", "", this::add);
    public final ArgsCapableOption<String> HelpOption = new ArgsCapableOption<>("help", "h", "", this::getHelp);
    public final ArgumentOption DepartmentOption = new ArgumentOption("department-id", "d", "", -1);
    public final ArgumentOption EmailAddressOption = new ArgumentOption("email-address", "f", "", "");
    public final ArgumentOption IdOption = new ArgumentOption("id", "i", "", -1);
    public final ArgumentOption IsActiveOption = new SwitchArgumentOption("is-active", "j", "");
    public final ArgumentOption NameOption = new ArgumentOption("name", "n", "", "");
    public final ArgumentOption LastNameOption = new ArgumentOption("last-name", "m", "", "");
    public final ArgumentOption TelephoneNumberOption = new ArgumentOption("telephone-number", "t", "", "");

    private final List<ArgsCapableOption> Options = List.of(AddOption, HelpOption);
    private final Map<String, ArgumentOption> ArgumentOptionsMap = OptionMapGenerator.generateMap(DepartmentOption,
            EmailAddressOption, IdOption, IsActiveOption, NameOption, LastNameOption, TelephoneNumberOption);
    private final Map<String, ArgsCapableOption> OptionsMap = OptionMapGenerator.generateMap(Options);

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
                Map.Entry<Boolean, String> optionResolverResult = OptionResolver.extractOptionName(arg);

                if (optionResolverResult.getKey()) {
                    String optionName = optionResolverResult.getValue();

                    if (OptionsMap.containsKey(optionName)) {
                        actionOption = OptionsMap.get(optionName);
                    } else if (ArgumentOptionsMap.containsKey(optionName)) {
                        ArgumentOption argumentOption = ArgumentOptionsMap.get(optionName);
                        Object valueForArg;

                        if (argumentOption instanceof SwitchArgumentOption) {
                            valueForArg = true;
                        } else if (i + 1 < args.length && !OptionResolver.extractOptionName(args[i + 1]).getKey()) {
                            valueForArg = args[i + 1];
                        } else {
                            // Non-switch argument option must be followed by a value, there wasn't enough arguments or
                            // the argument that followed the current one was an option, warn the user in this case.
                            return String.format("Missing value for option %s.", arg);
                        }

                        argsByName.put(argumentOption.name, valueForArg);
                    } else {
                        return String.format("Invalid option: %s", arg);
                    }
                }
            }

            if (actionOption != null) {
                @SuppressWarnings("unchecked") // Not going to change the array, see the API note for Class<T>.getConstructors()
                Constructor<Collaborator> controllerConstructor = (Constructor<Collaborator>) Collaborator.class.getConstructors()[0];
                Stream<Parameter> constructorParameters = Arrays.stream(controllerConstructor.getParameters());

                Object[] argsForAction = constructorParameters.map(p -> {
                    String constructorParameterName = p.getName();
                    StringBuilder builder = new StringBuilder(constructorParameterName);

                    for (int i = 0; i < builder.length(); ++i) {
                        char currentChar = builder.charAt(i);
                        if (Character.isUpperCase(currentChar)) {
                            builder.replace(i, i + 1, String.valueOf(Character.toLowerCase(currentChar)));
                            builder.insert(i, '-');
                        }
                    }

                    if (Model.class.isAssignableFrom(p.getType())) {
                        builder.append("-id");
                    }

                    String argumentOptionName = builder.toString();
                    ArgumentOption argumentOption = ArgumentOptionsMap.get(argumentOptionName);
                    Object valueForArg = argsByName.get(argumentOptionName);

                    if (valueForArg == null) {
                        // NOTE: Option for constructor parameter was not provided, so take default value
                        return argumentOption.defaultValue;
                    }

                    return valueForArg;
                }).toArray(Object[]::new);
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
