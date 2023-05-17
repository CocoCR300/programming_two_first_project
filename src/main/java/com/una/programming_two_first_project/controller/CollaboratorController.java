package com.una.programming_two_first_project.controller;


import com.google.inject.Inject;
import com.una.programming_two_first_project.formatter.CollaboratorFormatter;
import com.una.programming_two_first_project.formatter.Formatter;
import com.una.programming_two_first_project.formatter.TaskFormatter;
import com.una.programming_two_first_project.model.*;
import com.una.programming_two_first_project.data_store.DataStore;
import com.una.programming_two_first_project.util.ArgsValidator;
import com.una.programming_two_first_project.util.EntityCreator;
import com.una.programming_two_first_project.util.TokenResolver;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CollaboratorController extends BaseModelController<Collaborator>
{
    private final Option departmentIdOption = new ConvertibleArgumentOption<String>("department-id", "d", "",
            ArgsValidator::isNotBlank);
    private final Option emailAddressOption = new ConvertibleArgumentOption<String>("email-address", "e", "",
            ArgsValidator::isNotBlank);
    private final Option idOption = new ConvertibleArgumentOption<String>("id", "i", "",
            ArgsValidator::isNotBlank);
    private final Option isActiveOption = new SwitchOption("is-active", "j", "");
    private final Option nameOption = new ConvertibleArgumentOption<String>("name", "n", "",
            ArgsValidator::isNotBlank);
    private final Option lastNameOption = new ConvertibleArgumentOption<String>("last-name", "l", "",
            ArgsValidator::isNotBlank);
    private final Option telephoneNumberOption = new ConvertibleArgumentOption<String>("telephone-number", "t", "",
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

    private final TaskFormatter taskFormatter;

    @Inject
    public CollaboratorController(DataStore dataStore, CollaboratorFormatter collaboratorFormatter, TaskFormatter taskFormatter, EntryController entryController, TokenResolver tokenResolver) {
        super(Collaborator.class, dataStore, collaboratorFormatter, entryController, tokenResolver);

        this.taskFormatter = taskFormatter;

        commands.put(addCommand.name, addCommand);
        commands.put(deleteCommand.name, deleteCommand);
        commands.put(editCommand.name, editCommand);
        commands.put(searchCommand.name, searchCommand);
    }

    @Override
    protected String delete(String id) {
        Optional<Collaborator> possibleCollaborator = dataStore.get(Collaborator.class, id).unwrap();
        if (possibleCollaborator.isEmpty()) {
            return String.format("A collaborator with ID '%s' does not exist.", id);
        }
        Collaborator collaborator = possibleCollaborator.get();

        if (collaborator.tasks.isEmpty()) {
            return super.delete(id);
        } else {
            Map<String, Collaborator> allCollaborators = dataStore.getAll(Collaborator.class).unwrap();
            List<Collaborator> sprintsToChoose = allCollaborators.values().stream().filter(c -> c != collaborator).toList();

            String tasksInfo = taskFormatter.formatMany(collaborator.tasks, Formatter.FORMAT_MINIMUM, 4);
            String sprintsInfo = formatter.formatMany(sprintsToChoose, Formatter.FORMAT_MINIMUM, 4);

            String confirmation = askForInput(String.format("""
                        Deleting this collaborator will affect the following tasks:
                        %s
                        You can type 'Y' to proceed with the deletion, 'N' to stop this action or provide a new
                        collaborator ID for the affected tasks from the following list:
                        %s
                        Choose an option:\s""", tasksInfo, sprintsInfo));
            do {
                String selectedOptionUppercase = confirmation.toUpperCase();
                if (selectedOptionUppercase.equals("Y")) {
                    return super.delete(id);
//                        Result<Integer, Exception> commitResult = dataStore.commitChanges();
//                        return commitResult.mapOrElse(i -> "Operation completed successfully.",
//                                e -> "An error occurred. Please contact the developers.");
                } else if (selectedOptionUppercase.equals("N")) {
                    return "Operation was cancelled.";
                } else if (allCollaborators.containsKey(confirmation)) {
                    Collaborator newCollaborator = allCollaborators.get(confirmation);
                    List<Task> newTasks = collaborator.tasks
                            .stream()
                            .map(t -> new Task(t.id, t.name, t.description, newCollaborator, t.sprint, t.neededResources))
                            .toList();
                    Result<List<Task>, String> result = dataStore.updateAll(Task.class, newTasks, true);
                    return result.mapOrElse(
                            l -> super.delete(id),
                            e -> "An error occurred. Please contact the developers.");
                } else {
                    confirmation = askForInput("Option was invalid or there was no collaborator with the entered ID.\nTry again: ");
                }
            } while (true);
        }
    }

    @Override
    protected Result<Map<String, Object>, String> verifyFieldMappings(Map<String, Object> fieldMappings) {
        return Result.ok(fieldMappings);
    }

    @Override
    protected String getExampleCommand(String commandName) {
        if (commandName.equals(addCommand.name)) {
            return """
        add collaborator --id "112096743" --name Robert -l "Washington Perez" --telephone-number "+3 80032" --e "robert.wp@gmail.com" -d 10034 --is-active
        add collaborator 389472201 Jessica "Rodriguez Altamirano" 86429700 jessica.roal@hotmail.com 10034""";
        } else if (commandName.equals(deleteCommand.name)) {
            return """
        delete collaborator --id 112096743
        delete collaborator 389472201""";
        } else if (commandName.equals(editCommand.name)) {
            return """
        edit collaborator --id "112096743" -l "Washington Ford" --e "robert.ford@yahoo.com" --is-active
        edit collaborator 389472201 --name Veronica -e veronica.roal@hotmail.com""";
        } else if (commandName.equals(editCommand.name)) {
            return """
        search collaborator --id "112096743"
        search collaborator --department-id 1234""";
        }

        return "";
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
            Result<Map<String, Object>, String> result = tokenResolver
                    .mapCommandArgsToModelFields(editCommand, Collaborator.class, argsByOptionName, existingInstance)
                    .mapErr(e -> {
                        // TODO
                        return "";
                    });
            return (String) result.andThen(fieldMappings ->
                    tokenResolver.checkIdListOption(fieldMappings, argsByOptionName, taskIdsOption, addTasksOption,
                            removeTasksOption, existingInstance.tasks).mapErr(e -> {
                        if (e.x().equals(TokenResolver.INVALID_ID_LIST_OPTION_COMBINATION)) {
                            return "add-tasks and remove-tasks options cannot be provided both at the same time.";
                        } else if (e.x().equals(TokenResolver.ID_IN_OPTION_LIST_NOT_RELATED)) {
                            return String.format("Task with ID '%s' is not assigned to this collaborator.",
                            e.y()[0]);
                        }

                        return "";
                    })).map(fieldMappings -> {
                        Collaborator newInstance = EntityCreator.newInstance(Collaborator.class, fieldMappings).unwrap();
                        // TODO: Unwrapping possible raw error message with unwrapSafe()
                        return Result.unwrapSafe(dataStore
                                .update(newInstance, false)
                                .map(c -> {
                                    Result<Integer, Exception> commitResult = dataStore.commitChanges();
                                    return commitResult.mapOrElse(
                                            i -> "Operation completed successfully.",
                                            e -> "An error occurred. Please contact the developers.");
                                }));
                    }).unwrapSafe();
        }

        return String.format("A collaborator with ID '%s' does not exist.", collaboratorId);
    }

    @Override
    public Command getAddCommand() {
        return addCommand;
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
        String id;
        if (argsByOptionName.containsKey(idOption.name)) {
            id = argsByOptionName.get(idOption.name);
            return dataStore
                    .get(Collaborator.class, id)
                    .unwrap()
                    .map(this::formatEntity)
                    .orElse(String.format("There is no collaborator with ID '%s'.", id));
        } else if (argsByOptionName.containsKey(departmentIdOption.name)) {
            id = argsByOptionName.get(departmentIdOption.name);
            String output = dataStore
                    .get(Department.class, id)
                    .unwrap()
                    .map(d -> formatEntities(d.collaborators))
                    .orElse(String.format("There are no departments with ID '%s'.", id));
            return output;
        }

        return "";
    }

    @Override
    public String getCommandInfo(String command) {
        return "";
    }
}
