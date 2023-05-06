package com.una.programming_two_first_project.util;

import com.google.inject.Inject;
import com.una.programming_two_first_project.model.*;
import com.una.programming_two_first_project.service.DataStore;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Stream;

import static com.una.programming_two_first_project.model.Tuple.tuple;

public class TokenResolver
{
    public static final String REQUIRED_OPTION_NOT_PRESENT = "Expected option '%s' not found.";

    private final DataStore dataStore;

    @Inject
    public TokenResolver(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    public <T> Result<Object[], String> mapCommandArgsToConstructor(Command command,
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

        Stream<Result<Object, String>> mappedArgs = constructorParameters.map(p -> {
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
                String entityId = (String) argsByOptionName.get(argumentOptionName);

                Result<Optional<Model>, String> result = dataStore.get((Class<Model>) p.getType(), entityId);
                // TODO: What if an entity with the given ID does not exist?
                valueForArg = result.unwrapOr(Optional.empty());
            } else {
                argumentOptionName = builder.toString();
                valueForArg = argsByOptionName.get(argumentOptionName);

                if (valueForArg == null) {
                    // NOTE: Option for constructor parameter was not provided, so take default value
                    Option option = command.getArgument(argumentOptionName);

                    if (option.isRequired) {
                        return Result.err(String.format(REQUIRED_OPTION_NOT_PRESENT, option.name));
                    }
                    valueForArg = Defaults.getDefault(p.getType());
                }
            }

            return Result.ok(valueForArg);
        });

        Optional<Result<Object, String>> argsForCommand = mappedArgs.reduce((r1, r2) -> {
            Result<Object, String> combinedResult = r1.and(r2);
            return combinedResult.map(v2 -> {
                Object v1 = r1.unwrap();

                List<Object> list;
                if (List.class.isAssignableFrom(v1.getClass())) {
                    list = (List<Object>) v1;
                    list.add(v2);
                } else {
                    list = new ArrayList<>();
                    list.add(v1);
                    list.add(v2);
                }

                return list;
            });
        });

        // TODO: Is it possible to receive no arguments here? Do we have enough checks before this line?
        return argsForCommand.get().map(o -> ((List<Object>) o).toArray());
    }

    public Result<Tuple<Command, Map<String, Object>>, String> extractCommandAndArgs(
            String[] args, Map<String, Command> availableCommands) {
        Command command = null;
        int skip = -1;
        Map<String, Object> argsByName = new HashMap<>();

        for (int i = 0; i != skip && i < args.length; ++i) {
            String arg = args[i];

            if (availableCommands.containsKey(arg)) {
                command = availableCommands.get(arg);
                skip = i;
                i = 0;
                continue;
            }

            String optionName = extractOptionName(arg).orElse("");
            if (command != null && command.args.containsKey(optionName)) {
                Option option = command.getArgument(optionName);
                Object valueForArg;

                if (option instanceof SwitchOption) {
                    valueForArg = true;
                } else if (i + 1 < args.length && extractOptionName(args[i + 1]).isEmpty()) {
                    // Value found, go to next argument option now

                    if (option instanceof TypedOption typedOption) {
                        Result<?, String> result = typedOption.converterFunction.apply(args[++i]);
                        Object value = result.unwrapSafe(); // Better than calling unwrap and unwrapErr, ending with
                        // three different checks

                        if (result.isOk()) {
                            valueForArg = value;
                        } else {
                            // Argument could not be converted, return error message to the caller
                            return Result.err((String) value);
                        }
                    }

                    valueForArg = args[++i];
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

        return Result.ok(tuple(command, argsByName));
    }

    public @NotNull Optional<String> extractOptionName(String optionArg) {
        if (optionArg.startsWith("--")) {
            return Optional.of(optionArg.substring(2));
        } else if (optionArg.startsWith("-")) {
            return Optional.of(optionArg.substring(1));
        }

        return Optional.empty();
    }

    public @NotNull String[] extractOptionNames(String[] args) {
        List<String> optionNames = new ArrayList<>();

        for (int i = 0; i < args.length; ++i) {
            extractOptionName(args[i]).ifPresent(optionNames::add);
        }

        return optionNames.toArray(String[]::new);
    }
}
