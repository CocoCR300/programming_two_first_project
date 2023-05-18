package com.una.programming_two_first_project.controller;


import com.google.inject.Inject;
import com.una.programming_two_first_project.formatter.CollaboratorFormatter;
import com.una.programming_two_first_project.formatter.Formatter;
import com.una.programming_two_first_project.formatter.TaskFormatter;
import com.una.programming_two_first_project.model.*;
import com.una.programming_two_first_project.data_store.DataStore;
import com.una.programming_two_first_project.util.ArgsValidator;
import com.una.programming_two_first_project.util.ControllerBoilerplateHelper;
import com.una.programming_two_first_project.util.EntityCreator;
import com.una.programming_two_first_project.util.TokenResolver;

import java.util.*;

// TODO: How to mark a collaborator as inactive and check collaborator state when assigning tasks to them
public class CollaboratorController extends BaseModelController<Collaborator>
{
    private final Option departmentIdOption = new ConvertibleArgumentOption<String>("department-id", "d",
            "ID of the department in which a collaborator works, or in which to search for collaborators.",
            arg -> ArgsValidator.isNotBlank(arg).map(String::toUpperCase));
    private final Option emailAddressOption = new ConvertibleArgumentOption<String>("email-address", "e",
            "Collaborator email address.",
            ArgsValidator::isNotBlank);
    private final Option idOption = new ConvertibleArgumentOption<String>("id", "i",
            "Collaborator ID, used when adding a collaborator, editing their information or searching for them.",
            arg -> ArgsValidator.isNotBlank(arg).map(String::toUpperCase));
    private final Option isActiveOption = new SwitchOption("is-active", "j",
            "Mark a collaborator as active. Does not require a value.");
    private final Option isInactiveOption = new SwitchOption("is-inactive", "j",
            "Mark a collaborator as inactive. Does not require a value.");
    private final Option nameOption = new ConvertibleArgumentOption<String>("name", "n",
            "Collaborator name.",
            ArgsValidator::isNotBlank);
    private final Option lastNameOption = new ConvertibleArgumentOption<String>("last-name", "l",
            "Collaborator last name.",
            ArgsValidator::isNotBlank);
    private final Option telephoneNumberOption = new ConvertibleArgumentOption<String>("telephone-number", "t",
            "Collaborator telephone number.",
            ArgsValidator::isNotBlank);
    private final Option taskIdsOption = new ConvertibleArgumentOption<String>("task-ids", "s",
            "IDs of the tasks to assign to, add to (using 'add-tasks') or remove from (using 'remove-tasks') the collaborator's tasks.",
            arg -> ArgsValidator.isNotBlank(arg).map(String::toUpperCase));
    private final Command<Map<String, String>> addCommand = new Command<>("add", "", this::add,
            new Option[] { idOption, nameOption, lastNameOption, telephoneNumberOption, emailAddressOption },
            new Option[]{ departmentIdOption, taskIdsOption, isActiveOption });
    private final Command<String> deleteCommand = new Command<>("delete", "", this::delete,
            new Option[]{ idOption }, null);

    private final Option addTasksOption = new SwitchOption("add-tasks", "a",
            "Add the tasks corresponding to the IDs passed through 'task-ids' to the collaborator's tasks, instead of overwriting their current tasks. Does not require a value.");
    private final Option removeTasksOption = new SwitchOption("remove-tasks", "r",
            "Remove the tasks corresponding to the IDs passed through 'task-ids' from the collaborator's tasks, instead of overwriting their current tasks. Does not require a value.");
    private final Command<Map<String, String>> editCommand = new Command<>("edit", "", this::edit,
            new Option[]{ idOption },
            new Option[] { nameOption, lastNameOption, telephoneNumberOption, emailAddressOption, departmentIdOption,
                    isActiveOption, isInactiveOption, taskIdsOption, addTasksOption, removeTasksOption });
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
            List<Collaborator> collaboratorsToChoose = allCollaborators.values().stream().filter(c -> c != collaborator).toList();
            String tasksInfo = taskFormatter.formatMany(collaborator.tasks, Formatter.FORMAT_MINIMUM, 4);
            String message;

            if (collaboratorsToChoose.isEmpty()) {
                message = "You can type 'Y' to proceed with the deletion, or 'N' to stop this action.\n";
            } else {
                String collaboratorsInfo = formatter.formatMany(collaboratorsToChoose, Formatter.FORMAT_MINIMUM, 4);
                message = String.format("""
                        You can type 'Y' to proceed with the deletion, 'N' to stop this action or provide a new
                        collaborator ID for the affected tasks from the following list:
                        %s""", collaboratorsInfo);
            }

            String confirmation = askForInputLoop(String.format("""
                        Deleting this collaborator will affect the following sprints:
                        %s
                        %s
                        Choose an option:\s""", tasksInfo, message),
                    "Option was invalid or there was no collaborator with the entered ID.\nTry again: ",
                    arg -> arg.equalsIgnoreCase("Y") || arg.equalsIgnoreCase("N") ||
                            (allCollaborators.containsKey(arg.toUpperCase()) && !arg.equalsIgnoreCase(collaborator.id)));
            if (confirmation.equals("Y")) {
                return super.delete(id);
            } else if (confirmation.equals("N")) {
                return "Operation was cancelled.";
            } else {
                Collaborator newCollaborator = allCollaborators.get(confirmation);
                List<Task> newTasks = collaborator.tasks
                        .stream()
                        .map(t -> new Task(t.id, t.name, t.description, newCollaborator, t.sprint, t.neededResources))
                        .toList();
                Result<List<Task>, String> result = dataStore.updateAll(Task.class, newTasks, true);
                return result.mapOrElse(
                        l -> super.delete(id),
                        e -> "An error occurred. Please contact the developers.");
            }
        }
    }

    @Override
    protected Result<Map<String, Object>, String> verifyFieldMappings(Map<String, Object> fieldMappings) {
        List<Task> tasks = (List<Task>) fieldMappings.get("tasks");

        if (!tasks.isEmpty()) {
            if (!((boolean) fieldMappings.get("isActive"))) {
                String confirmation = askForInputLoop("""
                                You cannot assign tasks to an inactive collaborator.
                                You can type 'Y' to mark the collaborator as active and proceed with the action, or 'N' to stop it.
                                Choose an option:\s""",
                        "Option was invalid.\nTry again: ",
                        ControllerBoilerplateHelper::validateYesOrNoInput);

                if (confirmation.equalsIgnoreCase("Y")) {
                    fieldMappings.replace("isActive", true);
                } else {
                    return Result.err("Operation was cancelled.");
                }
            }

            List<Task> conflictingTasks = ControllerBoilerplateHelper.checkAlreadyAssignedEntities(tasks, t -> t.collaborator);

            if (!conflictingTasks.isEmpty()) {
                String conflictingTasksInfo =
                        taskFormatter.formatMany(conflictingTasks, Formatter.FORMAT_MINIMUM, 2);

                String confirmation = askForInputLoop(String.format("""
                                The following tasks are already assigned to a collaborator:
                                %s
                                You can type 'Y' to overwrite this information and proceed with the action, or 'N' to stop it.
                                Choose an option:\s""",
                                conflictingTasksInfo),
                        "Option was invalid.\nTry again: ",
                        ControllerBoilerplateHelper::validateYesOrNoInput);

                if (confirmation.equals("Y")) {
                    List<Task> newTasks = tasks
                            .stream()
                            .map(t -> t.collaborator == null ? t : new Task(t.id, t.name, t.description, null, t.sprint, t.neededResources))
                            .toList();
                    dataStore.updateAll(Task.class, newTasks, false);
                    fieldMappings.put("tasks", newTasks);
                } else {
                    return Result.err("Operation was cancelled.");
                }
            }
        }

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
        } else if (commandName.equals(searchCommand.name)) {
            return """
        search collaborator --id "112096743"
        search collaborator --department-id 1234""";
        }

        return "";
    }

    public String edit(Map<String, String> argsByOptionName) {
        String collaboratorId = argsByOptionName.get(idOption.name);
        Optional<Collaborator> possibleExistingInstance = getEntity(collaboratorId);

        if (possibleExistingInstance.isPresent()) {
            Collaborator existingInstance = possibleExistingInstance.get();
            Result<Map<String, Object>, String> result = tokenResolver
                    .mapCommandArgsToModelFields(editCommand, Collaborator.class, argsByOptionName, existingInstance)
                    .mapErr(e -> handleFieldMappingError(editCommand, e));

            return Result.unwrapSafe(result.andThen(fieldMappings -> {
                if (argsByOptionName.get(isInactiveOption.name) != null) {
                    if (argsByOptionName.get(isActiveOption.name) != null) {
                        return Result.err("is-active and is-inactive options cannot be provided both at the same time.");
                    }

                    fieldMappings.replace("isActive", false);
                }

                return tokenResolver.checkIdListOption(fieldMappings, argsByOptionName, taskIdsOption, addTasksOption,
                        removeTasksOption, existingInstance.tasks).mapErr(e -> {
                    if (e.x().equals(TokenResolver.INVALID_ID_LIST_OPTION_COMBINATION)) {
                        return "add-tasks and remove-tasks options cannot be provided both at the same time.";
                    } else if (e.x().equals(TokenResolver.ID_IN_OPTION_LIST_NOT_RELATED)) {
                        return String.format("Task with ID '%s' is not assigned to this collaborator.",
                                e.y()[0]);
                    }

                    return "";
                });
            }).map(fieldMappings -> {
                Collaborator newInstance = EntityCreator.newInstance(Collaborator.class, fieldMappings).unwrap();
                return Result.unwrapSafe(dataStore
                        .update(newInstance, false)
                        .map(c -> {
                            Result<Integer, Exception> commitResult = dataStore.commitChanges();
                            return commitResult.mapOrElse(
                                    i -> "Operation completed successfully.",
                                    e -> "An error occurred. Please contact the developers.");
                        }));
            }));
        }

        return String.format("A collaborator with ID '%s' does not exist.", collaboratorId);
    }

    @Override
    public Command getAddCommand() {
        return addCommand;
    }

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
}
