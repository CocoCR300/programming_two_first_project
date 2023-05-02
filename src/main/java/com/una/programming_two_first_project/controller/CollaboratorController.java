package com.una.programming_two_first_project.controller;


import com.una.programming_two_first_project.model.*;
import com.una.programming_two_first_project.util.TokenMapGenerator;
import com.una.programming_two_first_project.util.TokenResolver;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

public class CollaboratorController implements ArgsCapableController
{
    public final Option DepartmentOption = new Option("department-id", "d", "", -1);
    public final Option EmailAddressOption = new Option("email-address", "e", "", "");
    public final Option IdOption = new Option("id", "i", "", -1);
    public final Option IsActiveOption = new SwitchOption("is-active", "j", "");
    public final Option NameOption = new Option("name", "n", "", "");
    public final Option LastNameOption = new Option("last-name", "m", "", "");
    public final Option TelephoneNumberOption = new Option("telephone-number", "t", "", "");

    public final Command<Object[]> AddCommand = new Command<>("add", "", this::add,
            TokenMapGenerator.generateMap(IdOption, NameOption, LastNameOption, TelephoneNumberOption, EmailAddressOption, DepartmentOption, IsActiveOption));
    public final Command<String> HelpCommand = new Command<>("help", "", this::getHelp,
            TokenMapGenerator.generateMap(NameOption));

    private final List<Command> Commands = List.of(AddCommand, HelpCommand);
    private final Map<String, Command> CommandsMap = TokenMapGenerator.generateMap(Commands);

    public String add(Object[] args) {
        try {
            Collaborator c = (Collaborator) Collaborator.class.getConstructors()[0].newInstance(args);
            String ln = c.lastName.toLowerCase();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        return "";
    }

    @Override
    public String getHelp(String tokenName) {
        return "Collaborator management";
    }

    @Override
    public String resolveArgs(String[] args) {
        if (args.length > 0) {

            Result<Tuple<Command, Map<String, Object>>, String> result = TokenResolver.extractCommandAndArgs(args, CommandsMap);
//            var argsForConstructor = result.andThen(t -> {
//                Constructor<Collaborator> collaboratorConstructor = (Constructor<Collaborator>) Collaborator.class.getConstructors()[0];
//                Object[] constructorArgs = TokenResolver.mapCommandArgsToConstructor(t.x(), collaboratorConstructor, t.y());
//
//                try {
//                    Collaborator collaborator = collaboratorConstructor.newInstance(constructorArgs);
//                    return Result.ok("Operation completed successfully.");
//                } catch (Exception ex) {
//                    return Result.err(String.format("An error occurred: %s", ex));
//                }
//            });

            String resultMessage = (String) result.map(t -> {
                Constructor<Collaborator> collaboratorConstructor = (Constructor<Collaborator>) Collaborator.class.getConstructors()[0];
                Object[] constructorArgs = TokenResolver.mapCommandArgsToConstructor(t.x(), collaboratorConstructor, t.y());

                try {
                    Collaborator collaborator = collaboratorConstructor.newInstance(constructorArgs);
                    return "Operation completed successfully.";
                } catch (Exception ex) {
                    return String.format("An error occurred: %s", ex);
                }
            }).unwrapSafe();

            return resultMessage;

//            Command actionOption = null;
//            int indexToSkip = -1;
//            Map<String, Object> argsByName = new HashMap<>();
//
//            for (int i = 0; i != indexToSkip && i < args.length; ++i) {
//                String arg = args[i];
//                Tuple<Boolean, String> optionResolverResult = TokenResolver.extractOptionName(arg);
//
//                if (optionResolverResult.x()) {
//                    String optionName = optionResolverResult.y();
//
//                    if (CommandsMap.containsKey(optionName)) {
//                        actionOption = CommandsMap.get(optionName);
//                        indexToSkip = i;
//                        i = 0;
//                    } else if (actionOption != null && actionOption.args.containsKey(optionName)) {
//                        Option argumentOption = actionOption.getArgument(optionName);
//                        Object valueForArg;
//
//                        if (argumentOption instanceof SwitchOption) {
//                            valueForArg = true;
//                        } else if (i + 1 < args.length && !TokenResolver.extractOptionName(args[i + 1]).x()) {
//                            valueForArg = args[i++]; // Value found, go to next argument option now
//                        } else {
//                            // Non-switch argument option must be followed by a value, there wasn't enough arguments or
//                            // the argument that followed the current one was an option, warn the user in this case.
//                            return String.format("Missing value for option %s.", arg);
//                        }
//
//                        argsByName.put(argumentOption.name, valueForArg);
//                    } else {
//                        return String.format("Invalid option: %s", arg);
//                    }
//                }
//            }
//
//            if (actionOption != null) {
//                actionOption.function.apply(argsByName);
//
//                // TODO: Move this to TokenResolver? This should be done by the "add" method
//                @SuppressWarnings("unchecked") // Not going to change the array, see the API note for Class<T>.getConstructors()
//                Constructor<Collaborator> controllerConstructor = (Constructor<Collaborator>) Collaborator.class.getConstructors()[0];
//                Stream<Parameter> constructorParameters = Arrays.stream(controllerConstructor.getParameters());
//
//                // Must be "effectively" final to be captured in closure passed to "map" method
//                Command finalActionOption = actionOption;
//                Object[] argsForAction = constructorParameters.map(p -> {
//                    String constructorParameterName = p.getName();
//                    StringBuilder builder = new StringBuilder(constructorParameterName);
//
//                    for (int i = 0; i < builder.length(); ++i) {
//                        char currentChar = builder.charAt(i);
//                        if (Character.isUpperCase(currentChar)) {
//                            builder.replace(i, i + 1, String.valueOf(Character.toLowerCase(currentChar)));
//                            builder.insert(i, '-');
//                        }
//                    }
//
//                    if (Model.class.isAssignableFrom(p.getType())) {
//                        builder.append("-id");
//                    }
//
//                    String argumentOptionName = builder.toString();
//                    Option argumentOption = finalActionOption.getArgument(argumentOptionName);
//                    Object valueForArg = argsByName.get(argumentOptionName);
//
//                    if (valueForArg == null) {
//                        // NOTE: Option for constructor parameter was not provided, so take default value
//                        return argumentOption.defaultValue;
//                    }
//
//                    return valueForArg;
//                }).toArray(Object[]::new);
//                actionOption.function.apply(argsForAction);
//            }

//            String selectedOptionName = args[0];
//
//            if (!CommandsMap.containsKey(selectedOptionName)) {
//                return "Invalid option: " + args[0];
//            }
//
//            if (args.length == 1) {
//                Token option = CommandsMap.get(selectedOptionName);
//
//                if (option instanceof Command argsCapableOption) {
//                    // TODO: Use this to check a valid cast, maybe... What's the return value for an invalid cast?
//                    String argsCapableOption1 = CastUtils.cast(option);
//                    // TODO: Unchecked cast
//                    return (String) argsCapableOption.function.apply("");
//                }
//            } else {
//                for (int i = 0; i < args.length; ++i) {
//                    if (CommandsMap.containsKey(TokenResolver.extractOptionName(args[0]).getValue())) {
//
//                    }
//                }
//            }
        }

        return "";
    }

    @Override
    public void selectOption(int optionIndex) {

    }

    @Override
    public String getCommandInfo(String command) {
        return "";
    }
}
