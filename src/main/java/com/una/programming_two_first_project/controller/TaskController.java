package com.una.programming_two_first_project.controller;

import com.google.inject.Inject;
import com.una.programming_two_first_project.data_store.DataStore;
import com.una.programming_two_first_project.formatter.TaskFormatter;
import com.una.programming_two_first_project.model.*;
import com.una.programming_two_first_project.util.ArgsValidator;
import com.una.programming_two_first_project.util.EntityCreator;
import com.una.programming_two_first_project.util.TokenResolver;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

public class TaskController extends BaseModelController<Task>
{
    private final Option collaboratorIdOption = new ConvertibleArgumentOption<String>("collaborator-id", "c", "",
            ArgsValidator::isNotBlank);
    private final Option descriptionOption = new ConvertibleArgumentOption<String>("description", "d", "",
            ArgsValidator::isNotBlank);
    private final Option nameOption = new ConvertibleArgumentOption<String>("name", "n", "",
            ArgsValidator::isNotBlank);
    private final Option sprintIdOption = new ConvertibleArgumentOption<String>("sprint-id", "s", "",
            ArgsValidator::isNotBlank);
    private final Option neededResourcesOption = new ConvertibleArgumentOption<String>("needed-resources", "m", "",
            ArgsValidator::isCommaSeparatedList);
    private final Command<Map<String, String>> addCommand = new Command<>("add", "", this::add,
            new Option[] { nameOption, descriptionOption, neededResourcesOption },
            new Option[]{ collaboratorIdOption, sprintIdOption });

    private final Option idOption = new ConvertibleArgumentOption<String>("id", "i", "",
            ArgsValidator::isNotBlank);
    private final Command<String> deleteCommand = new Command<>("delete", "", this::delete,
            new Option[]{ idOption }, null);

    private final Command<Map<String, String>> editCommand = new Command<>("edit", "", this::edit,
            new Option[]{ idOption },
            new Option[] { nameOption, descriptionOption, neededResourcesOption, sprintIdOption, collaboratorIdOption });
    private final Command<Map<String, String>> searchCommand = new Command<>("search", "", this::search,
            null, new Option[] { collaboratorIdOption, idOption, sprintIdOption });

    @Inject
    public TaskController(@NotNull Class<Task> modelClass, @NotNull DataStore dataStore, @NotNull TaskFormatter formatter,
                          @NotNull EntryController entryController, @NotNull TokenResolver tokenResolver) {
        super(modelClass, dataStore, formatter, entryController, tokenResolver);
        commands.put(addCommand.name, addCommand);
        commands.put(deleteCommand.name, deleteCommand);
        commands.put(editCommand.name, editCommand);
        commands.put(searchCommand.name, searchCommand);
    }

    private String edit(Map<String, String> argsByOptionName) {
        String taskId = argsByOptionName.get(idOption.name);
        Optional<Task> possibleExistingInstance = dataStore.get(Task.class, taskId).unwrap();

        if (possibleExistingInstance.isPresent()) {
            Task existingInstance = possibleExistingInstance.get();
            String output = tokenResolver
                    .mapCommandArgsToModelFields(editCommand, Task.class, argsByOptionName, existingInstance)
                    .mapOrElse(fieldMappings -> {
                        Task newInstance = EntityCreator.newInstance(Task.class, fieldMappings).unwrap();
                        // TODO: Unwrapping possible raw error message with unwrapSafe()
                        return Result.unwrapSafe(dataStore
                                .update(newInstance, false)
                                .map(c -> {
                                    Result<Integer, Exception> commitResult = dataStore.commitChanges();
                                    return commitResult.mapOrElse(
                                            i -> "Operation completed successfully.",
                                            e -> "An error occurred. Please contact the developers.");
                                }));
                    }, e -> {
                        // TODO
                        return "";
                    });

            return output;
        }

        return String.format("A task with ID '%s' does not exist.", taskId);
    }

    private String search(Map<String, String> argsByOptionName) {
        String id;
        if (argsByOptionName.containsKey(idOption.name)) {
            id = argsByOptionName.get(idOption.name);
            return dataStore
                    .get(Task.class, id)
                    .unwrap()
                    .map(this::formatEntity)
                    .orElse(String.format("There is no task with ID '%s'.", id));
        } else if (argsByOptionName.containsKey(collaboratorIdOption.name)) {
            id = argsByOptionName.get(sprintIdOption.name);
            return dataStore
                    .get(Collaborator.class, id)
                    .unwrap()
                    .map(s -> formatEntities(s.tasks))
                    .orElse(String.format("There is no collaborator with ID '%s'.", id));
        } else if (argsByOptionName.containsKey(sprintIdOption.name)) {
            id = argsByOptionName.get(sprintIdOption.name);
            return dataStore
                    .get(Sprint.class, id)
                    .unwrap()
                    .map(s -> formatEntities(s.tasks))
                    .orElse(String.format("There is no sprint with ID '%s'.", id));
        }

        return "";
    }

    @Override
    protected Command getAddCommand() {
        return addCommand;
    }

    @Override
    protected Result<Map<String, Object>, String> verifyFieldMappings(Map<String, Object> fieldMappings) {
        if (fieldMappings.containsKey("collaborator")) {
            Collaborator collaborator = (Collaborator) fieldMappings.get("collaborator");
            if (!collaborator.isActive) {
                String confirmation = askForInput("""
                        The collaborator that you wish to assign this task to is inactive.
                        You can type 'Y' to mark the collaborator as active and proceed, or 'N' to stop this action.
                        Choose an option:\s""");
                do {
                    String selectedOptionUppercase = confirmation.toUpperCase();
                    if (selectedOptionUppercase.equals("Y")) {
                        Collaborator newCollaborator = new Collaborator(collaborator.id, collaborator.name,
                                collaborator.lastName, collaborator.telephoneNumber, collaborator.emailAddress,
                                collaborator.department, true);
                        dataStore.update(newCollaborator, false);
                        fieldMappings.replace("collaborator", newCollaborator);
                        break;
                    } else if (selectedOptionUppercase.equals("N")) {
                        return Result.err("Operation was cancelled.");
                    } else {
                        confirmation = askForInput("Option was invalid.\nTry again: ");
                    }
                } while (true);
            }
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
