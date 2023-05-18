package com.una.programming_two_first_project.controller;

import com.google.inject.Inject;
import com.una.programming_two_first_project.data_store.DataStore;
import com.una.programming_two_first_project.formatter.Formatter;
import com.una.programming_two_first_project.formatter.ProjectFormatter;
import com.una.programming_two_first_project.formatter.SprintFormatter;
import com.una.programming_two_first_project.model.*;
import com.una.programming_two_first_project.util.*;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.*;

public class ProjectController extends BaseModelController<Project>
{
    private final Option endDateOption = new ConvertibleArgumentOption<LocalDate>("end-date", "e",
            "Project end date.",
            arg -> ArgsValidator.isNotBlank(arg).andThen(ArgsValidator::isDate));
    private final Option nameOption = new ConvertibleArgumentOption<String>("name", "n",
            "Project name.",
            ArgsValidator::isNotBlank);
    private final Option startDateOption = new ConvertibleArgumentOption<LocalDate>("start-date", "s",
            "Project start date.",
            arg -> ArgsValidator.isNotBlank(arg).andThen(ArgsValidator::isDate));
    private final Option sprintIdsOption = new ConvertibleArgumentOption<String>("sprint-ids", "t",
            "IDs of the sprints to assign to, add to (using 'add-sprints') or remove from (using 'remove-sprints') the project.",
            arg -> ArgsValidator.isNotBlank(arg).map(String::toUpperCase));
    private final Command<Map<String, String>> addCommand = new Command<>("add", "", this::add,
            new Option[] { nameOption, startDateOption, endDateOption },
            new Option[] { sprintIdsOption });

    private final Option codeOption = new ConvertibleArgumentOption<String>("code", "c",
            "Project code, used when deleting a project, editing its information or searching for them.",
            ArgsValidator::isNotBlank);
    private final Command<String> deleteCommand = new Command<>("delete", "", this::delete,
            new Option[]{ codeOption }, null);

    private final Option addSprintsOption = new SwitchOption("add-sprints", "a",
            "Add the sprints corresponding to the IDs passed through 'sprint-ids' to this sprint, instead of overwriting its current sprints. Does not require a value.");
    private final Option removeSprintsOption = new SwitchOption("remove-sprints", "r",
            "Remove the sprints corresponding to the IDs passed through 'sprint-ids' from this sprint, instead of overwriting its current sprints. Does not require a value.");
    private final Command<Map<String, String>> editCommand = new Command<>("edit", "", this::edit,
            new Option[]{ codeOption },
            new Option[] { nameOption, startDateOption, endDateOption, sprintIdsOption, addSprintsOption, removeSprintsOption });
    private final Command<Map<String, String>> searchCommand = new Command<>("search", "", this::search,
            new Option[] { codeOption }, null);
    private final SprintFormatter sprintFormatter;

    @Inject
    public ProjectController(@NotNull DataStore dataStore, @NotNull ProjectFormatter formatter,
                             @NotNull SprintFormatter sprintFormatter, @NotNull EntryController entryController,
                             @NotNull TokenResolver tokenResolver) {
        super(Project.class, dataStore, formatter, entryController, tokenResolver);
        this.sprintFormatter = sprintFormatter;
        commands.put(addCommand.name, addCommand);
        commands.put(deleteCommand.name, deleteCommand);
        commands.put(editCommand.name, editCommand);
        commands.put(searchCommand.name, searchCommand);
    }

    @Override
    protected String delete(String id) {
        Optional<Project> possibleProject = dataStore.get(Project.class, id).unwrap();
        if (possibleProject.isEmpty()) {
            return String.format("A project with code '%s' does not exist.", id);
        }
        Project project = possibleProject.get();

        if (project.sprints.isEmpty()) {
            return super.delete(id);
        } else {
            Map<String, Project> allProjects = dataStore.getAll(Project.class).unwrap();
            List<Project> projectsToChoose = allProjects.values().stream().filter(p -> p != project).toList();

            String sprintsInfo = sprintFormatter.formatMany(project.sprints, Formatter.FORMAT_MINIMUM, 4);
            String message;

            if (projectsToChoose.isEmpty()) {
                message = "You can type 'Y' to proceed with the deletion, or 'N' to stop this action.\n";
            } else {
                String projectsInfo = formatter.formatMany(projectsToChoose, Formatter.FORMAT_MINIMUM, 4);
                message = String.format("""
                        You can type 'Y' to proceed with the deletion, 'N' to stop this action or provide a new
                        project ID for the affected sprints from the following list:
                        %s""", projectsInfo);
            }

            String confirmation = askForInputLoop(String.format("""
                        Deleting this project will affect the following sprints:
                        %s
                        %s
                        Choose an option:\s""", sprintsInfo, message),
                    "Option was invalid or there was no project with the entered ID.\nTry again: ",
                    arg -> arg.equalsIgnoreCase("Y") || arg.equalsIgnoreCase("N") ||
                            (allProjects.containsKey(arg.toUpperCase()) && !arg.equalsIgnoreCase(project.getId())));

            do {
                String selectedOptionUppercase = confirmation.toUpperCase();
                if (selectedOptionUppercase.equals("Y")) {
                    return super.delete(id);
                } else if (selectedOptionUppercase.equals("N")) {
                    return "Operation was cancelled.";
                } else if (allProjects.containsKey(confirmation)) {
                    Project newProject = allProjects.get(confirmation);
                    List<Sprint> newSprints = project.sprints
                            .stream()
                            .map(s -> new Sprint(s.id, newProject, s.number, s.startDate, s.endDate))
                            .toList();
                    Result<List<Sprint>, String> result = dataStore.updateAll(Sprint.class, newSprints, true);
                    return result.mapOrElse(
                            l -> super.delete(id),
                            e -> "An error occurred. Please contact the developers.");
                } else {
                    confirmation = askForInput("Option was invalid or there was no project with the entered ID.\nTry again: ");
                }
            } while (true);
        }
    }

    private String edit(Map<String, String> argsByOptionName) {
        String projectCode = argsByOptionName.get(codeOption.name);
        Optional<Project> possibleExistingInstance = dataStore.get(Project.class, projectCode).unwrap();

        if (possibleExistingInstance.isPresent()) {
            Project existingInstance = possibleExistingInstance.get();

            Result<Map<String, Object>, String> result = tokenResolver
                    .mapCommandArgsToModelFields(editCommand, Project.class, argsByOptionName, existingInstance)
                    .mapErr(e -> handleFieldMappingError(editCommand, e));
            return (String) result.andThen(fieldMappings ->
                    tokenResolver.checkIdListOption(fieldMappings, argsByOptionName, sprintIdsOption, addSprintsOption,
                            removeSprintsOption, existingInstance.sprints).mapErr(e -> {
                        String errorType = e.x();

                        if (errorType.equals(TokenResolver.INVALID_ID_LIST_OPTION_COMBINATION)) {
                            return "add-collaborators and remove-collaborators options cannot be provided both at the same time.";
                        } else if (errorType.equals(TokenResolver.ID_IN_OPTION_LIST_NOT_RELATED)) {
                            return String.format("Collaborator with ID '%s' is not in this department",
                                    e.y()[0]);
                        }

                        return "";
                    })).map(fieldMappings -> {
                Project newInstance = EntityCreator.newInstance(Project.class, fieldMappings).unwrap();
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

        return String.format("A project with code '%s' does not exist.", projectCode);
    }

    private String search(Map<String, String> argsByOptionName) {
        String code = argsByOptionName.get(codeOption.name);

        return dataStore
                .get(Project.class, code)
                .unwrap()
                .map(this::formatEntity)
                .orElse(String.format("There is no project with code '%s'.", code));
    }

    @Override
    protected Command getAddCommand() {
        return addCommand;
    }

    @Override
    protected Result<Map<String, Object>, String> verifyFieldMappings(Map<String, Object> fieldMappings) {
        LocalDate projectEndDate = (LocalDate) fieldMappings.get("endDate");
        LocalDate projectStartDate = (LocalDate) fieldMappings.get("startDate");

        if (projectStartDate.isAfter(projectEndDate)) {
            return Result.err("Project's start date cannot be after the end date.");
        }

        String conflictingSprintsInfo;
        List<Sprint> sprints = (List<Sprint>) fieldMappings.get("sprints");
        List<Sprint> conflictingSprints = ControllerBoilerplateHelper.checkAlreadyAssignedEntities(sprints, s -> s.project);
        if (!conflictingSprints.isEmpty()) {
            conflictingSprintsInfo = sprintFormatter.formatMany(conflictingSprints, Formatter.FORMAT_MINIMUM, 2);

            String confirmation = askForInputLoop(String.format("""
                                The following sprints are already in a project:
                                %s
                                You can type 'Y' to overwrite this information and proceed with the action, or 'N' to stop it.
                                Choose an option:\s""",
                            conflictingSprintsInfo),
                    "Option was invalid.\nTry again: ",
                    ControllerBoilerplateHelper::validateYesOrNoInput);

            if (confirmation.equals("Y")) {
                List<Sprint> newSprints = sprints
                        .stream()
                        .map(s -> s.project == null ? s : new Sprint(s.id, null, s.number, s.startDate, s.endDate))
                        .toList();
                dataStore.updateAll(Sprint.class, newSprints, false);
                fieldMappings.put("sprints", newSprints);
            } else {
                return Result.err("Operation was cancelled.");
            }
        }

        conflictingSprints.clear();
        for (Sprint sprint : sprints) {
            if (sprint.startDate.isBefore(projectStartDate) || sprint.endDate.isAfter(projectEndDate)) {
                conflictingSprints.add(sprint);
            }
        }

        if (!conflictingSprints.isEmpty()) {
            conflictingSprintsInfo = sprintFormatter.formatMany(conflictingSprints, Formatter.FORMAT_MINIMUM, 2);
            String confirmation = askForInput(String.format("""
                    The following sprints' start or end dates conflict with the project's start or end date:
                    %s
                    
                    Project start date: %s
                    Project end date: %s
                    
                    You can type 'Y' to adapt all the sprints' conflicting start and end dates to the project start
                    and end dates to proceed, or 'N' to stop this action
                    Choose an option:\s""", conflictingSprintsInfo,
                    Formatter.defaultDateFormatter().format(projectStartDate),
                    Formatter.defaultDateFormatter().format(projectEndDate)));

            do {
                String selectedOptionUppercase = confirmation.toUpperCase();
                if (selectedOptionUppercase.equals("Y")) {
                    List<Sprint> newSprints = new ArrayList<>();
                    for (Sprint conflictingSprint : conflictingSprints) {
                        LocalDate newEndDate = conflictingSprint.endDate, newStartDate = conflictingSprint.startDate;
                        if (conflictingSprint.startDate.isBefore(projectStartDate)) {
                            newStartDate = projectStartDate;
                        }
                        if (conflictingSprint.endDate.isAfter(projectEndDate)) {
                            newEndDate = projectEndDate;
                        }

                        newSprints.add(new Sprint(conflictingSprint.id, conflictingSprint.project,
                                conflictingSprint.number, newStartDate, newEndDate));
                    }

                    // TODO: Ignoring Result
                    var result = dataStore.updateAll(Sprint.class, newSprints, false);
                    fieldMappings.replace("sprints", newSprints);
                    return Result.ok(fieldMappings);
                } else if (selectedOptionUppercase.equals("N")) {
                    return Result.err("Operation was cancelled.");
                } else {
                    confirmation = askForInput("Option was invalid.\nTry again: ");
                }
            } while (true);
        }

        return Result.ok(fieldMappings);
    }

    @Override
    protected String getExampleCommand(String commandName) {
        // TODO
        return "";
    }
}
