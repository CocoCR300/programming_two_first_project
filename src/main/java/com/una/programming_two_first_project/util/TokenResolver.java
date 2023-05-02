package com.una.programming_two_first_project.util;

import com.una.programming_two_first_project.model.*;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Stream;

import static com.una.programming_two_first_project.model.Tuple.tuple;

public class TokenResolver
{
    public static <T> Object[] mapCommandArgsToConstructor(Command command,
                                                           Constructor<T> modelConstructor,
                                                           Map<String, Object> argsByOptionName) {
        Stream<Parameter> constructorParameters = Arrays.stream(modelConstructor.getParameters());

//        Stream<String> optionNames = argsByOptionName.keySet().stream();
//        optionNames.map(o -> {
//            StringBuilder builder = new StringBuilder(o);
//            int indexOfDash = builder.indexOf("-");
//
//            while (indexOfDash != -1) {
//                builder.replace(indexOfDash + 1, indexOfDash + 2, String.valueOf(Character.toUpperCase(builder.charAt(indexOfDash + 1))));
//                builder.replace(indexOfDash, indexOfDash + 1, "");
//                indexOfDash = builder.indexOf("-");
//            }
//
//            String constructorParameterName = builder.toString();
//
//
//
//            return builder.toString();
//        }).toArray();

        Object[] argsForCommand = constructorParameters.map(p -> {
            String constructorParameterName = p.getName();
            StringBuilder builder = new StringBuilder(constructorParameterName);

            for (int i = 0; i < builder.length(); ++i) {
                char currentChar = builder.charAt(i);
                if (Character.isUpperCase(currentChar)) {
                    builder.replace(i, i + 1, String.valueOf(Character.toLowerCase(currentChar)));
                    builder.insert(i, '-');
                }
            }

            Object valueForArg = null;
            String argumentOptionName;
            if (Model.class.isAssignableFrom(p.getType())) {
                builder.append("-id");
                argumentOptionName = builder.toString();
                int entityId = (int) argsByOptionName.get(argumentOptionName);
                String modelNameKey = p.getType().getSimpleName().toLowerCase();
                /* TODO
                    valueForArg = dataStore.getByModelName(modelNameKey).get(entityId);
                */
            } else {
                argumentOptionName = builder.toString();
                valueForArg = argsByOptionName.get(argumentOptionName);

                if (valueForArg == null) {
                    // NOTE: Option for constructor parameter was not provided, so take default value
                    Option option = command.getArgument(argumentOptionName);
                    valueForArg = option.defaultValue;
                }
            }

            return valueForArg;
        }).toArray();

        return argsForCommand;
    }

    public static Result<Tuple<Command, Map<String, Object>>, String> extractCommandAndArgs(
            String[] args, Map<String, Command> availableCommands) {
        Command command = null;
        Map<String, Object> argsByName = new HashMap<>();

        for (int i = 0; i < args.length; ++i) {
            String arg = args[i];

            if (availableCommands.containsKey(arg)) {
                command = availableCommands.get(arg);
                continue;
            }

            Tuple<Boolean, String> optionResolverResult = TokenResolver.extractOptionName(arg);

            if (optionResolverResult.x()) {
                String optionName = optionResolverResult.y();

                if (command != null && command.args.containsKey(optionName)) {
                    Option option = command.getArgument(optionName);
                    Object valueForArg;

                    if (option instanceof SwitchOption) {
                        valueForArg = true;
                    } else if (i + 1 < args.length && !TokenResolver.extractOptionName(args[i + 1]).x()) {
                        valueForArg = args[++i]; // Value found, go to next argument option now
                    } else {
                        // Non-switch option must be followed by a value, there wasn't enough arguments or
                        // the argument that followed the current one was an option, warn the user in this case.
                        return Result.err(String.format("Missing value for option %s.", arg));
                    }

                    argsByName.put(option.name, valueForArg);
                } else {
                    return Result.err(String.format("Invalid option: %s", arg));
                }
            }
        }

        return Result.ok(tuple(command, argsByName));
    }

    public static @NotNull Tuple<Boolean, String> extractOptionName(String optionArg) {
        if (optionArg.startsWith("--")) {
            return tuple(true, optionArg.substring(2));
        } else if (optionArg.startsWith("-")) {
            return tuple(true, optionArg.substring(1));
        }

        return tuple(false, "");
    }

    public static @NotNull String[] extractOptionNames(String[] args) {
        List<String> optionNames = new ArrayList<>();

        for (int i = 0; i < args.length; ++i) {
            Tuple<Boolean, String> optionName = extractOptionName(args[i]);

            if (!optionName.x()) {
                optionNames.add(optionName.y());
            }
        }

        return optionNames.toArray(String[]::new);
    }
}
