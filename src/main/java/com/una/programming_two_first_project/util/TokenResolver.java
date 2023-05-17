package com.una.programming_two_first_project.util;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.una.programming_two_first_project.data_store.Model;
import com.una.programming_two_first_project.model.*;
import com.una.programming_two_first_project.data_store.DataStore;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.una.programming_two_first_project.model.Tuple.tuple;

@Singleton
public class TokenResolver
{
    public static final String ID_DOES_NOT_EXIST = "ID_DOES_NOT_EXIST";
    public static final String ID_IN_OPTION_LIST_NOT_RELATED = "ID_IN_OPTION_LIST_NOT_RELATED";
    public static final String INVALID_ID_LIST_OPTION_COMBINATION = "INVALID_ID_LIST_OPTION_COMBINATION";
    public static final String SOME_IDS_DO_NOT_EXIST = "SOME_IDS_DO_NOT_EXIST";
    public static final String REQUIRED_OPTION_NOT_PRESENT = "REQUIRED_OPTION_NOT_PRESENT";

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
        if (option instanceof ConvertibleArgumentOption convertibleArgumentOption) {
            Result<Object, String> converterResult = convertibleArgumentOption.validate(arg);
            return converterResult.mapErr(e -> String.format(e, optionNameUsed));
        }

        return Result.ok(arg);
    }

    private <T extends Model> Result<Map<String, Object>, String> extractArgumentValues(Command command,
                                                                                        List<String> argsForCommand) {
        Map<String, Object> argsByName = new HashMap<>();
        int commandIndex = 0;
        IntWrapper argumentIndex = new IntWrapper();
        for (; argumentIndex.value < argsForCommand.size(); ++commandIndex, ++argumentIndex.value) {
            String arg = argsForCommand.get(argumentIndex.value);
            Optional<String> possibleOptionName = extractOptionName(arg);
            if (possibleOptionName.isPresent()) {
                String optionName = possibleOptionName.get();
                @SuppressWarnings("unchecked") // Type erasure doing its thing
                Optional<Option> possibleOption = command.getOption(optionName);

                // Two checks
                if (possibleOption.isPresent()) {
                    Option option = possibleOption.get();

                    Result<Object, String> result = extractArgumentValue(option, optionName, arg, argsForCommand, argumentIndex);
                    if (result.isErr()) {
                        return Result.err(result.unwrapErr());
                    }

                    result.inspect(v -> argsByName.put(option.name, v));
                } else {
                    return Result.err(String.format("Invalid option: %s", arg));
                }
            } else { // No option provided, fallback to the ordered arguments list, useful for single argument commands
                Option option = (Option) command.getOptionAt(commandIndex).unwrap();

                // Duplicate code
                Result<Object, String> result = validateArgForOption(option, option.name, arg);
                if (result.isErr()) {
                    return Result.err(result.unwrapErr());
                }

                result.inspect(v -> argsByName.put(option.name, v));
            }
        }

        return Result.ok(argsByName);
    }

    public Result<Map<String, Object>, Tuple<String, String[]>> checkIdListOption(Map<String, Object> fieldMappings,
                                                                 Map<String, String> argsByOptionName,
                                                                 Option idListOption, Option addEntitiesOption,
                                                                 Option removeEntitiesOption,
                                                                 List<? extends Model> existingInstanceRelatedEntities) {
        List<Model> selectedEntities;
        if ((selectedEntities = (List<Model>) fieldMappings.get(idListOption.name)) != null) {
            List<Model> newEntities;
            boolean addEntities = argsByOptionName.containsKey(addEntitiesOption.name),
                    removeEntities = argsByOptionName.containsKey(removeEntitiesOption.name);

            if (addEntities && removeEntities) {
                return Result.err(
                        tuple(INVALID_ID_LIST_OPTION_COMBINATION, new String[] { addEntitiesOption.name , removeEntitiesOption.name }));
            } else if (!addEntities && !removeEntities) {
                fieldMappings.replace(idListOption.name, selectedEntities);
            } else {
                if (addEntities) {
                    newEntities = Stream
                            .concat(existingInstanceRelatedEntities.stream(), selectedEntities.stream())
                            .distinct()
                            .toList();
                } else {
                    newEntities = new ArrayList<>(existingInstanceRelatedEntities);

                    for (Model entity : selectedEntities) {
                        if (!newEntities.remove(entity)) {
                            return Result.err(tuple(ID_IN_OPTION_LIST_NOT_RELATED, new String[] { entity.getId() }));
                        }
                    }
                }

                fieldMappings.replace(idListOption.name, newEntities);
            }
        }

        return Result.ok(fieldMappings);
    }

    public <T extends Model> Result<Map<String, Object>, Tuple<String, String[]>> mapCommandArgsToModelFields(Command command,
                                                                                             Class<T> modelClass,
                                                                                             Map<String, String> argsByOptionName,
                                                                                             T existingEntity) {
        Stream<Field> modelFields = Arrays.stream(modelClass.getFields());

        Stream<Result<Tuple<String, Object>, Tuple<String, String[]>>> mappedArgs = modelFields.map(f -> {
            String fieldName = f.getName();
            StringBuilder optionNameFromFieldNameBuilder = new StringBuilder(fieldName);

            for (int i = 0; i < optionNameFromFieldNameBuilder.length(); ++i) {
                char currentChar = optionNameFromFieldNameBuilder.charAt(i);
                if (Character.isUpperCase(currentChar)) {
                    optionNameFromFieldNameBuilder.replace(i, i + 1, String.valueOf(Character.toLowerCase(currentChar)));
                    optionNameFromFieldNameBuilder.insert(i, '-');
                }
            }

            Object valueForField = null;
            String optionName;
            if (Model.class.isAssignableFrom(f.getType())) {
                optionNameFromFieldNameBuilder.append("-id");
                optionName = optionNameFromFieldNameBuilder.toString();
                String entityId = argsByOptionName.get(optionName);

                if (entityId != null) {
                    Class<Model> relatedModelClass = (Class<Model>) f.getType();
                    Optional<Model> possibleEntity = dataStore.get(relatedModelClass, entityId).unwrap();

                    if (possibleEntity.isEmpty()) {
                        return Result.err(tuple(ID_DOES_NOT_EXIST, new String[] { relatedModelClass.getSimpleName().toLowerCase(), entityId }));
                    }

                    valueForField = possibleEntity.get();
                }
            } else if (Collection.class.isAssignableFrom(f.getType())) {
                String modelClassName = optionNameFromFieldNameBuilder.toString();
                modelClassName = modelClassName.substring(0, modelClassName.length() - 1);
                optionNameFromFieldNameBuilder.insert(optionNameFromFieldNameBuilder.length() - 1, "-id");
                optionName = optionNameFromFieldNameBuilder.toString();
                String entityIdsJoined = argsByOptionName.get(optionName);

                if (entityIdsJoined != null) {
                    String[] entityIds = Arrays.stream(entityIdsJoined.split(",")).map(String::trim).toArray(String[]::new);
                    Class<Model> collectionFieldModelClass = (Class<Model>) dataStore.getClassFromSimpleName(modelClassName).get();
                    List<Optional<Model>> possibleEntities = dataStore.getMany(collectionFieldModelClass, entityIds).unwrap();
                    List<String> nonExistingIds = new ArrayList<>(0);

                    for (int i = 0; i < possibleEntities.size(); ++i) {
                        if (possibleEntities.get(i).isEmpty()) {
                            nonExistingIds.add(entityIds[i]);
                        }
                    }

                    if (nonExistingIds.size() != 0) {
                        String relatedModelNamePlural = String.format("%ss", collectionFieldModelClass.getSimpleName().toLowerCase());
                        nonExistingIds.add(0, relatedModelNamePlural);
                        return Result.err(tuple(SOME_IDS_DO_NOT_EXIST, new String[]{ relatedModelNamePlural, String.join(", ", nonExistingIds) }));
                    }

                    valueForField = possibleEntities
                            .stream()
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .collect(Collectors.toList());
                    // NOTE: The returned list has no guarantees, revisit this thing if modifications to it need to be
                    // done sometime
                }
            } else {
                optionName = optionNameFromFieldNameBuilder.toString();
                valueForField = argsByOptionName.get(optionName);

                if (valueForField == null) { // Option for constructor parameter was not provided
                    if (existingEntity != null) {
                        // Extract field value from existing entity if present
                        try {
                            Field modelField = modelClass.getField(f.getName());
                            modelField.setAccessible(true);
                            valueForField = modelField.get(existingEntity);
                        } catch (Exception ex) {
                            // TODO
                        }
                    } else {
                        valueForField = Defaults.getDefault(f.getType());
                    }
                }
            }

            @SuppressWarnings("unchecked") // Type erasure doing its thing
            Optional<Option> possibleOption = command.getOption(optionName);
            Option option;

            // TODO: Shouldn't this check be at the top of the above if/else chain?
            // Already checked that "command.isRequired(option = possibleOption.get()).get()" will be present
            //noinspection OptionalGetWithoutIsPresent
            if (valueForField == (valueForField != null ? Defaults.getDefault(valueForField.getClass()) : null)
                    && possibleOption.isPresent()
                    && (Boolean) command.isRequired(option = possibleOption.get()).get()) {
                // "argsByOptionName" didn't have a value associated with the option and it is required,
                // so return an error.
                return Result.err(tuple(REQUIRED_OPTION_NOT_PRESENT, new String[] { option.name }));
            }

            return Result.ok(tuple(f.getName(), valueForField));
        });

        Result<Map<String, Object>, Tuple<String, String[]>> mappedFieldValues = mappedArgs.reduce(
                Result.ok(new HashMap<>()),
                (fullResult, itemResult) -> {
                    Result<Tuple<String, Object>, Tuple<String, String[]>> combinedResult = fullResult.and(itemResult);
                    return combinedResult.map(r -> {
                        Map<String, Object> map = fullResult.unwrap();
                        map.put(r.x(), r.y());
                        return map;
                });},
                (result1, result2) -> {
                    // TODO: This is never being executed, and I don't think I got this right... But it is working
                    Result<Map<String, Object>, Tuple<String, String[]>> combinedResult = result1.and(result2);
                    return combinedResult.map(r -> {
                        Map<String, Object> finalMap = new HashMap<>();
                        finalMap.putAll(result1.unwrap());
                        finalMap.putAll(result2.unwrap());
                        return finalMap;
                });}
        );

//        Optional<Result<Object, String>> argsForCommand = mappedArgs.reduce((r1, r2) -> {
//            Result<Object, String> combinedResult = r1.and(r2);
//            return combinedResult.map(v2 -> {
//                Tuple<String, Object> entry2 = (Tuple<String, Object>) v2;
//                Object v1 = r1.unwrap();
//
//                Map<String, Object> map;
//                if (Map.class.isAssignableFrom(v1.getClass())) {
//                    map = (Map<String, Object>) v1;
//                } else {
//                    Tuple<String, Object> entry1 = (Tuple<String, Object>) v1;
//                    map = new HashMap();
//                    map.put(entry1.x(), entry1.y());
//                }
//
//                map.put(entry2.x(), entry2.y());
//                return map;
//            });
//        });

        // TODO: Is it possible to receive no arguments here? Do we have enough checks before this line?
        return mappedFieldValues;
    }

    public <T extends Model> Result<Object[], String> mapCommandArgsToConstructor(Command command,
                                                                                  Constructor<T> modelConstructor,
                                                                                  Map<String, Object> argsByOptionName,
                                                                                  T existingEntity) {
        // Better do this like "mapCommandArgsToModelFields", rework entity creation to access fields directly instead
        // of using a parameterized constructor (create a EntityCreator class that works with the output of this method,
        // the name is not so accurate, because it would also create new entities based on existing ones, like modify them).
        // All of this in order to make it easier to work with list-type options (DepartmentController.collaboratorIds),
        // in this situation, there is no constructor parameter for the collection fields, so we need to access the field
        // directly, and Gson and others do this already, let's copy from them.
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
                if (command == helpCommand) {
                    argsForCommand.add(arg);
                    break;
                } else if (foundCommand == helpCommand && command != null) {
                    argsForCommand.add(command.name);
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
