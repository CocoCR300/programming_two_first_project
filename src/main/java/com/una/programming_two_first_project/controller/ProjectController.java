package com.una.programming_two_first_project.controller;

import com.google.inject.Inject;
import com.una.programming_two_first_project.data_store.DataStore;
import com.una.programming_two_first_project.formatter.Formatter;
import com.una.programming_two_first_project.formatter.ProjectFormatter;
import com.una.programming_two_first_project.formatter.SprintFormatter;
import com.una.programming_two_first_project.model.*;
import com.una.programming_two_first_project.util.ArgsValidator;
import com.una.programming_two_first_project.util.EntityCreator;
import com.una.programming_two_first_project.util.StringUtils;
import com.una.programming_two_first_project.util.TokenResolver;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ProjectController extends BaseModelController<Project>
{
    private final Option endDateOption = new ConvertibleArgumentOption<LocalDate>("end-date", "e", "",
            arg -> ArgsValidator.isNotBlank(arg).andThen(ArgsValidator::isDate));
    private final Option nameOption = new ConvertibleArgumentOption<String>("name", "n", "",
            ArgsValidator::isNotBlank);
    private final Option startDateOption = new ConvertibleArgumentOption<LocalDate>("start-date", "s", "",
            arg -> ArgsValidator.isNotBlank(arg).andThen(ArgsValidator::isDate));
    private final Option sprintIdsOption = new ConvertibleArgumentOption<String>("sprint-ids", "t", "",
            ArgsValidator::isCommaSeparatedList);
    private final Command<Map<String, String>> addCommand = new Command<>("add", "", this::add,
            new Option[] { nameOption, startDateOption, endDateOption },
            new Option[] { sprintIdsOption });

    private final Option codeOption = new ConvertibleArgumentOption<String>("code", "c", "",
            ArgsValidator::isNotBlank);
    private final Command<String> deleteCommand = new Command<>("delete", "", this::delete,
            new Option[]{ codeOption }, null);

    private final Option addSprintsOption = new SwitchOption("add-sprints", "a", "");
    private final Option removeSprintsOption = new SwitchOption("remove-sprints", "r", "");
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
            String projectsInfo = formatter.formatMany(projectsToChoose, Formatter.FORMAT_MINIMUM, 4);

            String confirmation = askForInput(String.format("""
                        Deleting this project will affect the following sprints:
                        %s
                        You can type 'Y' to proceed with the deletion, 'N' to stop this action or provide a new
                        project ID for the affected sprints from the following list:
                        %s
                        Choose an option:\s""", sprintsInfo, projectsInfo));
            do {
                String selectedOptionUppercase = confirmation.toUpperCase();
                if (selectedOptionUppercase.equals("Y")) {
                    return super.delete(id);
//                        Result<Integer, Exception> commitResult = dataStore.commitChanges();
//                        return commitResult.mapOrElse(i -> "Operation completed successfully.",
//                                e -> "An error occurred. Please contact the developers.");
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
                    .mapErr(e -> {
                        // TODO
                        return "";
                    });
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

        List<Sprint> projectSprints = (List<Sprint>) fieldMappings.get("sprints");
        List<Sprint> conflictingSprints = new ArrayList<>();
        StringBuilder conflictingSprintsInfoBuilder = new StringBuilder();
        for (Sprint sprint : projectSprints) {
            if (sprint.startDate.isBefore(projectStartDate) || sprint.endDate.isAfter(projectEndDate)) {
                conflictingSprints.add(sprint);
                conflictingSprintsInfoBuilder.append(sprintFormatter.formatMinimum(sprint, 2));
                conflictingSprintsInfoBuilder.append('\n');
            }
        }

        if (!conflictingSprints.isEmpty()) {
            String confirmation = askForInput(String.format("""
                    The following sprints' start or end dates conflict with the project's start or end date:
                    %s
                    
                    Project start date: %s
                    Project end date: %s
                    
                    You can type 'Y' to adapt all the sprints' conflicting start and end dates to the project start
                    and end dates to proceed, or 'N' to stop this action
                    Choose an option:\s""", conflictingSprintsInfoBuilder,
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

    @Override
    public String getCommandInfo(String command) {
        return null;
    }
}
