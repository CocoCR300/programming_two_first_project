package com.una.programming_two_first_project.controller;


import com.google.inject.Inject;
import com.una.programming_two_first_project.model.*;
import com.una.programming_two_first_project.formatter.CollaboratorFormatter;
import com.una.programming_two_first_project.service.DataStore;
import com.una.programming_two_first_project.util.ArgsValidator;
import com.una.programming_two_first_project.util.EntityCreator;
import com.una.programming_two_first_project.util.TokenMapGenerator;
import com.una.programming_two_first_project.util.TokenResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class CollaboratorController extends BaseModelController<Collaborator>
{
    private final Option commandNameOption = new Option("command-name", "n", "");
    private final Command<String> helpCommand = new Command<>("help", "", this::getHelp,
            new Option[] { commandNameOption }, null);

    private final Option departmentIdOption = new ConvertibleArgumentOption("department-id", "d", "",
            ArgsValidator::isNotBlank);
    private final Option emailAddressOption = new ConvertibleArgumentOption("email-address", "e", "",
            ArgsValidator::isNotBlank);
    private final Option idOption = new ConvertibleArgumentOption("id", "i", "",
            ArgsValidator::isNotBlank);
    private final Option isActiveOption = new SwitchOption("is-active", "j", "");
    private final Option nameOption = new ConvertibleArgumentOption("name", "n", "",
            ArgsValidator::isNotBlank);
    private final Option lastNameOption = new ConvertibleArgumentOption("last-name", "l", "",
            ArgsValidator::isNotBlank);
    private final Option telephoneNumberOption = new ConvertibleArgumentOption("telephone-number", "t", "",
            ArgsValidator::isNotBlank);
    private final Command<Map<String, String>> addCommand = new Command<>("add", "", this::add,
            new Option[] { idOption, nameOption, lastNameOption, telephoneNumberOption, emailAddressOption },
            new Option[]{ departmentIdOption, isActiveOption });
    private final Command<String> deleteCommand = new Command<>("delete", "", this::delete,
            new Option[]{ idOption }, null);

    private final Option addTasksOption = new SwitchOption("add-tasks", "a", "");
    private final Option removeTasksOption = new SwitchOption("remove-tasks", "r", "");
    private final Option taskIdsOption = new SwitchOption("task-ids", "s", "");
    private final Command<Map<String, String>> editCommand = new Command<>("edit", "", this::edit,
            new Option[]{ idOption },
            new Option[] { nameOption, lastNameOption, telephoneNumberOption, emailAddressOption, departmentIdOption, isActiveOption,
                            taskIdsOption, addTasksOption, removeTasksOption});
    private final Command<Map<String, String>> searchCommand = new Command<>("search", "", this::search,
            null, new Option[] { departmentIdOption, idOption });

    private final List<Command> commands = List.of(addCommand, deleteCommand, editCommand, helpCommand, searchCommand);
    // TODO: Could delete this and create on demand on every usage of it, just the TokenResolver is doing it for now
    private final Map<String, Command> commandsMap = TokenMapGenerator.generateMap(commands);

    @Inject
    public CollaboratorController(DataStore dataStore, CollaboratorFormatter collaboratorFormatter, EntryController entryController, TokenResolver tokenResolver) {
        super(Collaborator.class, dataStore, collaboratorFormatter, entryController, tokenResolver);
    }

//    public String add(Map<String, Object> argsByOptionName) {
//        Class<Collaborator> collaboratorClass = Collaborator.class;
//        Result<Map<String, Object>, Tuple<String, String[]>> result = tokenResolver.mapCommandArgsToModelFields(addCommand, collaboratorClass, argsByOptionName, null);
//
//        return result.mapOrElse(fieldMappings -> {
//            try {
//                Collaborator collaborator = EntityCreator.newInstance(collaboratorClass, fieldMappings).unwrap();
//                return dataStore.add(collaborator).mapOrElse(c -> {
//                    Result<Integer, Exception> commitResult = dataStore.commitChanges();
//                    return commitResult.mapOrElse(
//                            i -> "Operation completed successfully.",
//                            e -> "An error occurred. Please contact the developers.");
//                }, e -> String.format("A collaborator with ID: %s already exists.", collaborator.id));
//                // DataStore.ENTITY_ALREADY_EXISTS is the only type of error result that can be received here, for now.
//
//            } catch (Exception ex) {
//                return String.format("An error occurred: %s", ex);
//            }
//        }, e -> {
//            String errorType = e.x();
//            String[] errorArguments = e.y();
//            switch (errorType) {
//                case TokenResolver.REQUIRED_OPTION_NOT_PRESENT:
//                    return String.format("Option %s is required for add command", errorArguments[0]);
//                case TokenResolver.ID_DOES_NOT_EXIST:
//                    return String.format("There is no %s with ID: %s", errorArguments[0], errorArguments[1]);
//                case TokenResolver.SOME_IDS_DO_NOT_EXIST:
//                    return String.format("There are no %s with IDs: %s", errorArguments[0], errorArguments[1]);
//            }
//
//            return "";
//        });
//
//
////        Constructor<Collaborator>[] modelConstructors = (Constructor<Collaborator>[]) Collaborator.class.getConstructors();
////        Constructor<Collaborator> collaboratorConstructor = modelConstructors[1];
////        Result<Object[], String> result = tokenResolver.mapCommandArgsToConstructor(addCommand, collaboratorConstructor, argsByOptionName, null);
////        return (String) result.map(constructorArgs -> {
////            try {
////                Collaborator collaborator = collaboratorConstructor.newInstance(constructorArgs);
////                return dataStore.add(collaborator).mapOrElse(c -> {
////                    Result<Integer, Exception> commitResult = dataStore.commitChanges();
////                    return commitResult.mapOrElse(i -> "Operation completed successfully.",
////                            e -> "An error occurred. Please contact the developers.");
////                }, e -> String.format("A collaborator with ID: %s already exists.", constructorArgs[0]));
////                // DataStore.ENTITY_ALREADY_EXISTS is the only type of error result that can be received here, for now.
////
////            } catch (Exception ex) {
////                return String.format("An error occurred: %s", ex);
////            }
////        }).unwrapSafe();
//    }

    public String edit(Map<String, String> argsByOptionName) {
        String collaboratorId = argsByOptionName.get(idOption.name);
        Optional<Collaborator> possibleExistingInstance = dataStore.get(Collaborator.class, collaboratorId).unwrap();

        if (possibleExistingInstance.isPresent()) {
            Collaborator existingInstance = possibleExistingInstance.get();
            Result<Map<String, Object>, Tuple<String, String[]>> result = tokenResolver.mapCommandArgsToModelFields(editCommand, Collaborator.class, argsByOptionName, existingInstance);
            return (String) result.map(fieldMappings -> {
                List<Task> selectedTasks;
                if ((selectedTasks = (List<Task>) fieldMappings.get(taskIdsOption.name)) != null) {
                    List<Task> currentTasks = existingInstance.tasks;
                    List<Task> newTasks;
                    boolean addTasks = argsByOptionName.containsKey(addTasksOption.name),
                            removeTasks = argsByOptionName.containsKey(removeTasksOption.name);

                    if (addTasks && removeTasks) {
                        return "add-tasks and remove-tasks options cannot be provided both at the same time.";
                    } else if (!addTasks && !removeTasks) {
                        fieldMappings.replace(taskIdsOption.name, selectedTasks);
                    } else {
                        if (addTasks) {
                            newTasks = Stream
                                    .concat(currentTasks.stream(), selectedTasks.stream())
                                    .distinct()
                                    .toList();
                        } else {
                            newTasks = new ArrayList<>(currentTasks);

                            for (Task task : selectedTasks) {
                                if (!newTasks.remove(task)) {
                                    return String.format("Task with ID '%s' is not assigned to this collaborator",
                                            task.id);
                                }
                            }
                        }

                        fieldMappings.replace(taskIdsOption.name, newTasks);
                    }
                }

                Collaborator newInstance = EntityCreator.newInstance(Collaborator.class, fieldMappings).unwrap();
                return dataStore
                        .update(newInstance, false)
                        .map(c -> {
                            Result<Integer, Exception> commitResult = dataStore.commitChanges();
                            return commitResult.mapOrElse(i -> "Operation completed successfully.",
                                    e -> "An error occurred. Please contact the developers.");
                        }).unwrapSafe();
            }).unwrapSafe();
        }

        return String.format("A collaborator with ID '%s' does not exist.", collaboratorId);
    }

    @Override
    public String getHelp(String tokenName) {
        return "Collaborator management";
    }

    @Override
    public Command getAddCommand() {
        return addCommand;
    }

    @Override
    public Command<String> getHelpCommand() {
        return helpCommand;
    }

    @Override
    public List<Command> getCommands() {
        return commands;
    }

    //    @Override
//    public String resolveArgs(String[] args) {
//        if (args.length > 0) {
//
//            Result<Tuple<Command, Map<String, Object>>, String> result = tokenResolver.extractCommandAndArgs(args, commandsMap, helpCommand);
////            var argsForConstructor = result.andThen(t -> {
////                Constructor<Collaborator> collaboratorConstructor = (Constructor<Collaborator>) Collaborator.class.getConstructors()[0];
////                Object[] constructorArgs = TokenResolver.mapCommandArgsToConstructor(t.x(), collaboratorConstructor, t.y());
////
////                try {
////                    Collaborator collaborator = collaboratorConstructor.newInstance(constructorArgs);
////                    return Result.ok("Operation completed successfully.");
////                } catch (Exception ex) {
////                    return Result.err(String.format("An error occurred: %s", ex));
////                }
////            });
//
//            String resultMessage = (String) result.map(t -> {
//                // TODO: Command may be null
//                Command command = t.x();
//                Map<String, Object> commandArgs = t.y();
//
//                if (command.optionsCount() == 1) {
//                    if (commandArgs.size() != 1) {
//                        return String.format("Expected 1 argument for command %s, found %s.", command.name, commandArgs.size());
//                    }
//
//                    Option onlyOption = (Option) command.getOptionAt(0).unwrap();
//                    String onlyArgKey = onlyOption.name;
//                    return command.function.apply(commandArgs.get(onlyArgKey));
//                }
//
//                return command.function.apply(t.y());
//            }).unwrapSafe();
//
//            return resultMessage;
//
////            Command actionOption = null;
////            int indexToSkip = -1;
////            Map<String, Object> argsByName = new HashMap<>();
////
////            for (int i = 0; i != indexToSkip && i < args.length; ++i) {
////                String arg = args[i];
////                Tuple<Boolean, String> optionResolverResult = TokenResolver.extractOptionName(arg);
////
////                if (optionResolverResult.x()) {
////                    String optionName = optionResolverResult.y();
////
////                    if (commandsMap.containsKey(optionName)) {
////                        actionOption = commandsMap.unwrap(optionName);
////                        indexToSkip = i;
////                        i = 0;
////                    } else if (actionOption != null && actionOption.args.containsKey(optionName)) {
////                        Option argumentOption = actionOption.getOption(optionName);
////                        Object valueForArg;
////
////                        if (argumentOption instanceof SwitchOption) {
////                            valueForArg = true;
////                        } else if (i + 1 < args.length && !TokenResolver.extractOptionName(args[i + 1]).x()) {
////                            valueForArg = args[i++]; // Value found, go to next argument option now
////                        } else {
////                            // Non-switch argument option must be followed by a value, there wasn't enough arguments or
////                            // the argument that followed the current one was an option, warn the user in this case.
////                            return String.format("Missing value for option %s.", arg);
////                        }
////
////                        argsByName.put(argumentOption.name, valueForArg);
////                    } else {
////                        return String.format("Invalid option: %s", arg);
////                    }
////                }
////            }
////
////            if (actionOption != null) {
////                actionOption.function.apply(argsByName);
////
////                // TODO: Move this to TokenResolver? This should be done by the "add" method
////                @SuppressWarnings("unchecked") // Not going to change the array, see the API note for Class<T>.getConstructors()
////                Constructor<Collaborator> controllerConstructor = (Constructor<Collaborator>) Collaborator.class.getConstructors()[0];
////                Stream<Parameter> constructorParameters = Arrays.stream(controllerConstructor.getParameters());
////
////                // Must be "effectively" final to be captured in closure passed to "map" method
////                Command finalActionOption = actionOption;
////                Object[] argsForAction = constructorParameters.map(p -> {
////                    String constructorParameterName = p.getName();
////                    StringBuilder builder = new StringBuilder(constructorParameterName);
////
////                    for (int i = 0; i < builder.length(); ++i) {
////                        char currentChar = builder.charAt(i);
////                        if (Character.isUpperCase(currentChar)) {
////                            builder.replace(i, i + 1, String.valueOf(Character.toLowerCase(currentChar)));
////                            builder.insert(i, '-');
////                        }
////                    }
////
////                    if (Model.class.isAssignableFrom(p.getType())) {
////                        builder.append("-id");
////                    }
////
////                    String argumentOptionName = builder.toString();
////                    Option argumentOption = finalActionOption.getOption(argumentOptionName);
////                    Object valueForArg = argsByName.unwrap(argumentOptionName);
////
////                    if (valueForArg == null) {
////                        // NOTE: Option for constructor parameter was not provided, so take default value
////                        return argumentOption.defaultValue;
////                    }
////
////                    return valueForArg;
////                }).toArray(Object[]::new);
////                actionOption.function.apply(argsForAction);
////            }
//
////            String selectedOptionName = args[0];
////
////            if (!commandsMap.containsKey(selectedOptionName)) {
////                return "Invalid option: " + args[0];
////            }
////
////            if (args.length == 1) {
////                Token option = commandsMap.unwrap(selectedOptionName);
////
////                if (option instanceof Command argsCapableOption) {
////                    // TODO: Use this to check a valid cast, maybe... What's the return value for an invalid cast?
////                    String argsCapableOption1 = CastUtils.cast(option);
////                    // TODO: Unchecked cast
////                    return (String) argsCapableOption.function.apply("");
////                }
////            } else {
////                for (int i = 0; i < args.length; ++i) {
////                    if (commandsMap.containsKey(TokenResolver.extractOptionName(args[0]).getValue())) {
////
////                    }
////                }
////            }
//        }
//
//        return "";
//    }

    // TODO: Should return an Iterable for large amounts of text
    public String search(Map<String, String> argsByOptionName) {
        super.search(argsByOptionName);

        String id = argsByOptionName.get(idOption.name);
        if (argsByOptionName.containsKey(idOption.name)) {
            return dataStore
                    .get(Collaborator.class, id)
                    .unwrap()
                    .map(this::formatEntity)
                    .orElse(String.format("There is no collaborator with ID '%s'.", id));
        } else {
            String output = dataStore
                    .get(Department.class, id)
                    .unwrap()
                    .map(d -> formatEntities(d.collaborators))
                    .orElse(String.format("There are no departments with ID '%s'.", id));
            return output;
        }
    }

    @Override
    public String getCommandInfo(String command) {
        return "";
    }

    @Override
    public void selectOption(int optionIndex) {

    }
}
