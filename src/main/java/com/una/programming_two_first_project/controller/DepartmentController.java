package com.una.programming_two_first_project.controller;

import com.google.inject.Inject;
import com.una.programming_two_first_project.model.*;
import com.una.programming_two_first_project.formatter.CollaboratorFormatter;
import com.una.programming_two_first_project.data_store.DataStore;
import com.una.programming_two_first_project.formatter.DepartmentFormatter;
import com.una.programming_two_first_project.formatter.Formatter;
import com.una.programming_two_first_project.util.ArgsValidator;
import com.una.programming_two_first_project.util.EntityCreator;
import com.una.programming_two_first_project.util.TokenResolver;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DepartmentController extends BaseModelController<Department>
{
    private final CollaboratorFormatter collaboratorFormatter;

    private final Option collaboratorIdsOption = new ConvertibleArgumentOption<String>("collaborator-ids",
            "c", "", ArgsValidator::isCommaSeparatedList);
    private final Option idOption = new ConvertibleArgumentOption<String>("id", "i", "",
            ArgsValidator::isNotBlank);
    private final Option nameOption = new ConvertibleArgumentOption<String>("name", "n", "",
            ArgsValidator::isNotBlank);
    private final Command<Map<String, String>> addCommand = new Command<>("add", "", this::add,
            new Option[] { nameOption },
            new Option[] {collaboratorIdsOption });

    private final Command<String> deleteCommand = new Command<>("delete", "", this::delete,
            new Option[]{ idOption }, null);

    private final Option addCollaboratorsOption = new SwitchOption("add-collaborators", "a", "");
    private final Option removeCollaboratorsOption = new SwitchOption("remove-collaborators", "r", "");
    private final Command<Map<String, String>> editCommand = new Command<>("edit", "", this::edit,
            new Option[]{ idOption },
            new Option[] { nameOption, collaboratorIdsOption, addCollaboratorsOption, removeCollaboratorsOption });
    private final Command<String> searchCommand = new Command<>("search", "", this::search,
            new Option[] { idOption }, null);

    @Inject
    public DepartmentController(DataStore dataStore, DepartmentFormatter departmentFormatter,
                                CollaboratorFormatter collaboratorFormatter, EntryController entryController,
                                TokenResolver tokenResolver) {
        super(Department.class, dataStore, departmentFormatter, entryController, tokenResolver);
        this.collaboratorFormatter = collaboratorFormatter;

        commands.put(addCommand.name, addCommand);
        commands.put(deleteCommand.name, deleteCommand);
        commands.put(editCommand.name, editCommand);
        commands.put(searchCommand.name, searchCommand);
    }

    private String edit(Map<String, String> argsByOptionName) {
        String departmentId = argsByOptionName.get(idOption.name);
        Optional<Department> possibleExistingInstance = dataStore.get(Department.class, departmentId).unwrap();

        if (possibleExistingInstance.isPresent()) {
            Department existingInstance = possibleExistingInstance.get();

            Result<Map<String, Object>, String> result = tokenResolver
                    .mapCommandArgsToModelFields(editCommand, Department.class, argsByOptionName, existingInstance)
                    .mapErr(e -> {
                        // TODO
                        return "";
                    });
            return (String) result.andThen(fieldMappings ->
                    tokenResolver.checkIdListOption(fieldMappings, argsByOptionName, collaboratorIdsOption, addCollaboratorsOption,
                            removeCollaboratorsOption, existingInstance.collaborators).mapErr(e -> {
                                String errorType = e.x();

                                if (errorType.equals(TokenResolver.INVALID_ID_LIST_OPTION_COMBINATION)) {
                                    return "add-collaborators and remove-collaborators options cannot be provided both at the same time.";
                                } else if (errorType.equals(TokenResolver.ID_IN_OPTION_LIST_NOT_RELATED)) {
                                    return String.format("Collaborator with ID '%s' is not in this department",
                                            e.y()[0]);
                                }

                                return "";
                    })).map(fieldMappings -> {
                Department newInstance = EntityCreator.newInstance(Department.class, fieldMappings).unwrap();
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

        return String.format("A department with ID '%s' does not exist.", departmentId);
    }

    private String search(String id) {
        return dataStore
                .get(Department.class, id)
                .unwrap()
                .map(this::formatEntity)
                .orElse(String.format("There is no department with ID '%s'.", id));
    }

    @Override
    protected Result<Map<String, Object>, String> verifyFieldMappings(Map<String, Object> fieldMappings) {
        return Result.ok(fieldMappings);
    }

    @Override
    protected String getExampleCommand(String commandName) {
        // TODO
        return "";
    }

    @Override
    protected String delete(String id) {
        Optional<Department> possibleDepartment = dataStore.get(Department.class, id).unwrap();
        if (possibleDepartment.isEmpty()) {
            return String.format("A department with ID '%s' does not exist.", id);
        }
        Department department = possibleDepartment.get();

        if (department.collaborators.isEmpty()) {
            return super.delete(id);
        } else {
            Map<String, Department> allDepartments = dataStore.getAll(Department.class).unwrap();
            List<Department> departmentsToChoose = allDepartments.values().stream().filter(d -> d != department).toList();

            String collaboratorsInfo = collaboratorFormatter.formatMany(department.collaborators, Formatter.FORMAT_MINIMUM, 4);
            String departmentsInfo = formatter.formatMany(departmentsToChoose, Formatter.FORMAT_MINIMUM, 4);

            String confirmation = askForInput(String.format("""
                        Deleting this department will affect the following collaborators:
                        %s
                        You can type 'Y' to proceed with the deletion, 'N' to stop this action or provide a new
                        department ID for the affected collaborators from the following list:
                        %s
                        Choose an option:\s""", collaboratorsInfo, departmentsInfo));
            do {
                String selectedOptionUppercase = confirmation.toUpperCase();
                if (selectedOptionUppercase.equals("Y")) {
                    return super.delete(id);
//                        Result<Integer, Exception> commitResult = dataStore.commitChanges();
//                        return commitResult.mapOrElse(i -> "Operation completed successfully.",
//                                e -> "An error occurred. Please contact the developers.");
                } else if (selectedOptionUppercase.equals("N")) {
                    return "Operation was cancelled.";
                } else if (allDepartments.containsKey(confirmation)) {
                    Department newDepartment = allDepartments.get(confirmation);
                    List<Collaborator> newCollaborators = department.collaborators
                            .stream()
                            .map(c -> new Collaborator(c.id, c.name, c.lastName, c.telephoneNumber, c.emailAddress,
                                    newDepartment, c.isActive))
                            .toList();
                    Result<List<Collaborator>, String> result = dataStore.updateAll(Collaborator.class, newCollaborators, true);
                    return result.mapOrElse(
                            l -> super.delete(id),
                            e -> "An error occurred. Please contact the developers.");
                } else {
                    confirmation = askForInput("Option was invalid or there was no department with the entered ID.\nTry again: ");
                }
            } while (true);
        }
    }

    @Override
    public Command getAddCommand() {
        return addCommand;
    }

    @Override
    public String getCommandInfo(String command) {
        return null;
    }
}
