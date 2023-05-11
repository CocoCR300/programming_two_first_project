package com.una.programming_two_first_project.util;

import com.google.inject.Inject;
import com.una.programming_two_first_project.model.*;
import com.una.programming_two_first_project.service.DataStore;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
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

    private Result<Object, String> extractArgumentValue(Option option, String optionNameUsed, String currentArg,
                                                        List<String> argsForCommand, IntWrapper currentIndex) {
        String nextArg;

        if (option instanceof SwitchOption) {
            return Result.ok(true);
        } else if (currentIndex.value < argsForCommand.size()
                && extractOptionName((nextArg = argsForCommand.get(currentIndex.value + 1))).isEmpty()) {
            currentIndex.value++; // Value found, go to next argument option now
            return validateArgForOption(option, optionNameUsed, nextArg);
        } else {
            // Non-switch option must be followed by a value, there wasn't enough arguments or
            // the argument that followed the current one was an option, warn the user in this case.
            return Result.err(String.format("Missing value for option %s.", currentArg));
        }
    }

    private Result<Object, String> validateArgForOption(Option option, String optionNameUsed, String arg) {
        if (option instanceof ConvertibleArgOption validatableOption) {
            Result<Object, String> converterResult = validatableOption.converterFunction.apply(arg);

            return converterResult.mapErr(e -> String.format(e, optionNameUsed));
        }

        return Result.ok(arg);
    }

    private <T extends Model> Result<Map<String, Object>, String> extractArgumentValues(Command command,
                                                                                        List<String> argsForCommand) {
        Map<String, Object> argsByName = new HashMap<>();
        for (IntWrapper i = new IntWrapper(); i.value < argsForCommand.size(); ++i.value) {
            String arg = argsForCommand.get(i.value);
            Optional<String> possibleOptionName = extractOptionName(arg);
            if (possibleOptionName.isPresent()) {
                String optionName = possibleOptionName.get();
                @SuppressWarnings("unchecked") // Type erasure doing its thing
                Optional<Option> possibleOption = command.getOption(optionName);

                // Two checks
                if (possibleOption.isPresent()) {
                    Option option = possibleOption.get();

                    Result<Object, String> result = extractArgumentValue(option, optionName, arg, argsForCommand, i);
                    if (result.isErr()) {
                        return Result.err(result.unwrapErr());
                    }

                    result.inspect(v -> argsByName.put(option.name, v));
                } else {
                    return Result.err(String.format("Invalid option: %s", arg));
                }
            } else { // No option provided, fallback to the ordered arguments list, useful for single argument commands
                Option option = (Option) command.getOptionAt(i.value).unwrap();

                // Duplicate code
                Result<Object, String> result =  validateArgForOption(option, option.name, arg);
                if (result.isErr()) {
                    return Result.err(result.unwrapErr());
                }

                result.inspect(v -> argsByName.put(option.name, v));
            }
        }

        return Result.ok(argsByName);
    }

    public <T extends Model> Result<Object[], String> mapCommandArgsToConstructor(Command command,
                                                                                  Constructor<T> modelConstructor,
                                                                                  Map<String, Object> argsByOptionName,
                                                                                  T existingEntity) {
        Stream<Parameter> constructorParameters = Arrays.stream(modelConstructor.getParameters());

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

                if (entityId != null) {
                    Optional<Model> possibleEntity = dataStore.get((Class<Model>) p.getType(), entityId).unwrap();

                    if (possibleEntity.isEmpty()) {
                        return Result.err(String.format("There is no %s with ID: %s", p.getType().getSimpleName().toLowerCase(), entityId));
                    }

                    valueForArg = possibleEntity.get();
                }
            } else {
                argumentOptionName = builder.toString();
                valueForArg = argsByOptionName.get(argumentOptionName);

                if (valueForArg == null) { // Option for constructor parameter was not provided
                    if (existingEntity != null) {
                        Class<T> modelClass = (Class<T>) existingEntity.getClass();
                        try {
                            Field modelField = modelClass.getField(p.getName());
                            modelField.setAccessible(true);
                            valueForArg = modelField.get(existingEntity);
                        } catch (Exception ex) {
                            // TODO
                        }
                    } else {
                        @SuppressWarnings("unchecked") // Type erasure doing its thing
                        Optional<Option> possibleOption = command.getOption(argumentOptionName);
                        Option option;

                        // Already checked that "command.isRequired(option = possibleOption.get()).get()" will be present
                        //noinspection OptionalGetWithoutIsPresent
                        if (possibleOption.isPresent() && (Boolean) command.isRequired(option = possibleOption.get()).get()) {
                            return Result.err(String.format(REQUIRED_OPTION_NOT_PRESENT, option.name));
                        }
                        valueForArg = Defaults.getDefault(p.getType());
                    }
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
            String[] args, Map<String, Command> availableCommands, Command helpCommand) {
        Command command = null;
        List<String> argsForCommand = new ArrayList<>();

        // Not checking for more than one command because that is being done on the MainEntryController
        for (String arg : args) {
            if (availableCommands.containsKey(arg)) {
                Command foundCommand = availableCommands.get(arg);
                if (command == helpCommand || foundCommand == helpCommand) {
                    argsForCommand.add(arg);
                }

                command = foundCommand;
            } else {
                argsForCommand.add(arg);
            }
        }

        if (command == null) {
            return Result.err("No command was provided.");
        }

        // TODO: This may not cover all cases...
        if (command.requiredOptionsCount() == 0 && command.optionalOptionsCount() > 1 && argsForCommand.size() == 0) {
            return Result.err(String.format("Expected at least 1 argument for command '%s'", command.name));
        }

        Result<Map<String, Object>, String> result = extractArgumentValues(command, argsForCommand);
        Command finalCommand = command;
        return result.andThen(m -> {
            if (m.size() < finalCommand.requiredOptionsCount()) {
                return Result.err(String.format(
                        "Expected at least %s arguments for command '%s', found %s arguments", finalCommand.requiredOptionsCount(),
                        finalCommand.name, argsForCommand.size() - 1));
            }

            return Result.ok(m);
        }).map(m -> tuple(finalCommand, m));
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
