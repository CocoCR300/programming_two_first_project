package com.una.programming_two_first_project.controller;

import com.google.inject.Inject;
import com.una.programming_two_first_project.formatter.SprintFormatter;
import com.una.programming_two_first_project.formatter.TaskFormatter;
import com.una.programming_two_first_project.model.*;
import com.una.programming_two_first_project.data_store.DataStore;
import com.una.programming_two_first_project.formatter.Formatter;
import com.una.programming_two_first_project.util.ArgsValidator;
import com.una.programming_two_first_project.util.ControllerBoilerplateHelper;
import com.una.programming_two_first_project.util.EntityCreator;
import com.una.programming_two_first_project.util.TokenResolver;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.*;

public class SprintController extends BaseModelController<Sprint>
{
    private final Option endDateOption = new ConvertibleArgumentOption<LocalDate>("end-date", "e",
            "Sprint end date. Must be before or the same day as its project's end date.",
            arg -> ArgsValidator.isNotBlank(arg).andThen(ArgsValidator::isDate));
    private final Option idOption = new ConvertibleArgumentOption<String>("id", "i",
            "Sprint ID, used when deleting a sprint, editing its information or searching for them.",
            arg -> ArgsValidator.isNotBlank(arg).map(String::toUpperCase));
    private final Option projectIdOption = new ConvertibleArgumentOption<String>("project-id", "p",
            "The ID of the project in which this sprint is, or in which to search for sprints.",
            arg -> ArgsValidator.isNotBlank(arg).map(String::toUpperCase));
    private final Option startDateOption = new ConvertibleArgumentOption<LocalDate>("start-date", "s",
            "Sprint start date. Must be after or the same day as its project's start date.",
            arg -> ArgsValidator.isNotBlank(arg).andThen(ArgsValidator::isDate));
    private final Option taskIdsOption = new ConvertibleArgumentOption<String>("task-ids", "t",
            "IDs of the tasks to assign to, add to (using 'add-tasks') or remove from (using 'remove-tasks') the sprint.",
            arg -> ArgsValidator.isNotBlank(arg).map(String::toUpperCase));
    private final Command<Map<String, String>> addCommand = new Command<>("add", "", this::add,
            new Option[] { startDateOption, endDateOption },
            new Option[]{ projectIdOption, taskIdsOption });
    private final Command<String> deleteCommand = new Command<>("delete", "", this::delete,
            new Option[]{ idOption }, null);

    private final Option addTasksOption = new SwitchOption("add-tasks", "a",
            "Add the tasks corresponding to the IDs passed through 'task-ids' to this sprint instead of overwriting its current tasks. Does not require a value.");
    private final Option removeTasksOption = new SwitchOption("remove-tasks", "r",
            "Remove the tasks corresponding to the IDs passed through 'task-ids' from this sprint instead of overwriting its current tasks. Does not require a value.");
    private final Command<Map<String, String>> editCommand = new Command<>("edit", "", this::edit,
            new Option[]{ idOption },
            new Option[] { startDateOption, endDateOption, projectIdOption, taskIdsOption, addTasksOption, removeTasksOption });

    private final Option codeOption = new ConvertibleArgumentOption<String>("code", "c",
            "The code of the sprint to search for.",
            ArgsValidator::isNotBlank);
    private final Command<Map<String, String>> searchCommand = new Command<>("search", "", this::search,
            null, new Option[] { codeOption, idOption, projectIdOption });

    private final TaskFormatter taskFormatter;

    @Inject
    public SprintController(@NotNull DataStore dataStore, @NotNull SprintFormatter formatter, TaskFormatter taskFormatter, @NotNull EntryController entryController, @NotNull TokenResolver tokenResolver) {
        super(Sprint.class, dataStore, formatter, entryController, tokenResolver);

        this.taskFormatter = taskFormatter;
        commands.put(addCommand.name, addCommand);
        commands.put(deleteCommand.name, deleteCommand);
        commands.put(editCommand.name, editCommand);
        commands.put(searchCommand.name, searchCommand);
    }

    private String edit(Map<String, String> argsByOptionName) {
        String sprintId = argsByOptionName.get(idOption.name);
        Optional<Sprint> possibleExistingInstance = dataStore.get(Sprint.class, sprintId).unwrap();

        if (possibleExistingInstance.isPresent()) {
            Sprint existingInstance = possibleExistingInstance.get();

            Result<Map<String, Object>, String> result = tokenResolver
                    .mapCommandArgsToModelFields(editCommand, Sprint.class, argsByOptionName, existingInstance)
                    .mapErr(e -> handleFieldMappingError(editCommand, e));
            return (String) result.andThen(fieldMappings ->
                    tokenResolver.checkIdListOption(fieldMappings, argsByOptionName, taskIdsOption, addTasksOption,
                            removeTasksOption, existingInstance.tasks).mapErr(e -> {
                        String errorType = e.x();

                        if (errorType.equals(TokenResolver.INVALID_ID_LIST_OPTION_COMBINATION)) {
                            return "add-tasks and remove-tasks options cannot be provided both at the same time.";
                        } else if (errorType.equals(TokenResolver.ID_IN_OPTION_LIST_NOT_RELATED)) {
                            return String.format("Task with ID '%s' is not in this sprint.", e.y()[0]);
                        }

                        return "";
                    })).map(fieldMappings -> {
                Sprint newInstance = EntityCreator.newInstance(Sprint.class, fieldMappings).unwrap();
                // TODO: Unwrapping possible raw error message with unwrapSafe()
                return Result.unwrapSafe(dataStore
                        .update(newInstance, false)
                        .map(c -> {
                            Result<Integer, Exception> commitResult = dataStore.commitChanges();
                            return commitResult.mapOrElse(i -> "Operation completed successfully.",
                                    e -> "An error occurred. Please contact the developers.");
                        }));
            }).unwrapSafe();
        }

        return String.format("A sprint with ID '%s' does not exist.", sprintId);
    }

    private String search(Map<String, String> argsByOptionName) {
        if (argsByOptionName.containsKey(idOption.name)) {
            String id = argsByOptionName.get(idOption.name);
            return dataStore
                    .get(Sprint.class, id)
                    .unwrap()
                    .map(this::formatEntity)
                    .orElse(String.format("There is no sprint with ID '%s'.", id));
        } else if (argsByOptionName.containsKey(codeOption.name)) {
            String code = argsByOptionName.get(codeOption.name);
            Collection<Sprint> sprints = dataStore
                    .getAll(Sprint.class)
                    .unwrap()
                    .values();
            Optional<Sprint> possibleSprint = sprints.stream().filter(s -> s.getCode().equals(code)).findAny();
            return possibleSprint
                    .map(this::formatEntity)
                    .orElse(String.format("There is no sprint with code '%s'.", code));
        } else if (argsByOptionName.containsKey(projectIdOption.name)) {
            String projectId = argsByOptionName.get(projectIdOption.name);
            return dataStore
                    .get(Project.class, projectId)
                    .unwrap()
                    .map(p -> formatEntities(p.sprints))
                    .orElse(String.format("There is no project with ID '%s'.", projectId));
        }

        return "";
    }

    @Override
    protected String delete(String id) {
        Optional<Sprint> possibleSprint = dataStore.get(Sprint.class, id).unwrap();
        if (possibleSprint.isEmpty()) {
            return String.format("A sprint with ID '%s' does not exist.", id);
        }
        Sprint sprint = possibleSprint.get();

        if (sprint.tasks.isEmpty()) {
            return super.delete(id);
        } else {
            Map<String, Sprint> allSprints = dataStore.getAll(Sprint.class).unwrap();
            List<Sprint> sprintsToChoose = allSprints.values().stream().filter(s -> s != sprint).toList();
            String tasksInfo = taskFormatter.formatMany(sprint.tasks, Formatter.FORMAT_MINIMUM, 4);
            String message;

            if (sprintsToChoose.isEmpty()) {
                message = "You can type 'Y' to proceed with the deletion, or 'N' to stop this action.\n";
            } else {
                String sprintsInfo = formatter.formatMany(sprintsToChoose, Formatter.FORMAT_MINIMUM, 4);
                message = String.format("""
                        You can type 'Y' to proceed with the deletion, 'N' to stop this action or provide a new
                        sprint ID for the affected tasks from the following list:
                        %s""", sprintsInfo);
            }

            String confirmation = askForInputLoop(String.format("""
                        Deleting this sprint will affect the following tasks:
                        %s
                        %s
                        Choose an option:\s""", tasksInfo, message),
                    "Option was invalid or there was no sprint with the entered ID.\nTry again: ",
                    arg -> {
                        return arg.equalsIgnoreCase("Y") || arg.equalsIgnoreCase("N") ||
                                (allSprints.containsKey(arg.toUpperCase()) && !arg.equalsIgnoreCase(sprint.id));
                    });
            do {
                String selectedOptionUppercase = confirmation.toUpperCase();
                if (selectedOptionUppercase.equals("Y")) {
                    return super.delete(id);
                } else if (selectedOptionUppercase.equals("N")) {
                    return "Operation was cancelled.";
                } else if (allSprints.containsKey(confirmation)) {
                    Sprint newSprint = allSprints.get(confirmation);
                    List<Task> newTasks = sprint.tasks
                            .stream()
                            .map(t -> new Task(t.id, t.name, t.description, t.collaborator, newSprint, t.neededResources))
                            .toList();
                    Result<List<Task>, String> result = dataStore.updateAll(Task.class, newTasks, true);
                    return result.mapOrElse(
                            l -> super.delete(id),
                            e -> "An error occurred. Please contact the developers.");
                } else {
                    confirmation = askForInput("Option was invalid or there was no sprint with the entered ID.\nTry again: ");
                }
            } while (true);
        }
    }

    @Override
    protected Command getAddCommand() {
        return addCommand;
    }

    @Override
    protected Result<Map<String, Object>, String> verifyFieldMappings(Map<String, Object> fieldMappings) {
        Optional<Sprint> highestNumberSprint = dataStore
                .getAll(Sprint.class)
                .unwrap()
                .values()
                .stream()
                .max(Comparator.comparingInt(s -> s.number));
        highestNumberSprint.ifPresent(s -> fieldMappings.replace("number", (short) (s.number + 1)));

        LocalDate endDate = (LocalDate) fieldMappings.get("endDate");
        LocalDate startDate = (LocalDate) fieldMappings.get("startDate");

        if (startDate.isAfter(endDate)) {
            return Result.err("Sprint's start date cannot be after the end date.");
        }

        Project project;
        if ((project = (Project) fieldMappings.get("project")) != null) {
            if (endDate.isAfter(project.endDate)) {
                return Result.err("Sprint's end date cannot be after the project's end date.");
            }

            if (startDate.isBefore(project.startDate)) {
                return Result.err("Sprint's start date cannot be before the project's start date.");
            }
        }

        List<Task> tasks = (List<Task>) fieldMappings.get("tasks");
        List<Task> conflictingTasks = ControllerBoilerplateHelper.checkAlreadyAssignedEntities(tasks, t -> t.sprint);
        if (!conflictingTasks.isEmpty()) {
            String conflictingTasksInfo = taskFormatter.formatMany(conflictingTasks, Formatter.FORMAT_MINIMUM, 2);

            String confirmation = askForInputLoop(String.format("""
                                The following tasks are already in a sprint:
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

        return Result.ok(fieldMappings);
    }

    @Override
    protected String getExampleCommand(String commandName) {
        // TODO
        return "";
    }
}
