package com.una.programming_two_first_project.controller;

import com.google.inject.Inject;
import com.una.programming_two_first_project.model.*;
import com.una.programming_two_first_project.formatter.CollaboratorFormatter;
import com.una.programming_two_first_project.data_store.DataStore;
import com.una.programming_two_first_project.formatter.DepartmentFormatter;
import com.una.programming_two_first_project.formatter.Formatter;
import com.una.programming_two_first_project.util.ArgsValidator;
import com.una.programming_two_first_project.util.ControllerBoilerplateHelper;
import com.una.programming_two_first_project.util.EntityCreator;
import com.una.programming_two_first_project.util.TokenResolver;

import java.util.*;

public class DepartmentController extends BaseModelController<Department>
{
    private final CollaboratorFormatter collaboratorFormatter;

    private final Option collaboratorIdsOption = new ConvertibleArgumentOption<String>("collaborator-ids",
            "c", "IDs of the collaborators to assign to, add to (using 'add-collaborators') or remove from (using 'remove-collaborators') the department.",
            arg -> ArgsValidator.isNotBlank(arg).map(String::toUpperCase));
    private final Option idOption = new ConvertibleArgumentOption<String>("id", "i",
            "Department ID, used when deleting a department, editing its information or searching for them.",
            arg -> ArgsValidator.isNotBlank(arg).map(String::toUpperCase));
    private final Option nameOption = new ConvertibleArgumentOption<String>("name", "n",
            "Department name.", ArgsValidator::isNotBlank);
    private final Command<Map<String, String>> addCommand = new Command<>("add", "", this::add,
            new Option[] { nameOption }, new Option[] { collaboratorIdsOption });

    private final Command<String> deleteCommand = new Command<>("delete", "", this::delete,
            new Option[]{ idOption }, null);

    private final Option addCollaboratorsOption = new SwitchOption("add-collaborators", "a",
            "Add the collaborators corresponding to the IDs passed through 'collaborator-ids' to the department. Does not require a value.");
    private final Option removeCollaboratorsOption = new SwitchOption("remove-collaborators", "r",
            "Remove the collaborators corresponding to the IDs passed through 'collaborator-ids' from the department. Does not require a value.");
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
                    .mapErr(e -> handleFieldMappingError(editCommand, e));
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
        List<Collaborator> collaborators = (List<Collaborator>) fieldMappings.get("collaborators");
        List<Collaborator> conflictingCollaborators = ControllerBoilerplateHelper.checkAlreadyAssignedEntities(collaborators, c -> c.department);
        if (!conflictingCollaborators.isEmpty()) {
            String conflictingTasksInfo = collaboratorFormatter.formatMany(conflictingCollaborators, Formatter.FORMAT_MINIMUM, 2);

            String confirmation = askForInputLoop(String.format("""
                                The following collaborators are already in a department:
                                %s
                                You can type 'Y' to overwrite this information and proceed with the action, or 'N' to stop it.
                                Choose an option:\s""",
                            conflictingTasksInfo),
                    "Option was invalid.\nTry again: ",
                    ControllerBoilerplateHelper::validateYesOrNoInput);

            if (confirmation.equals("Y")) {
                List<Collaborator> newCollaborators = collaborators
                        .stream()
                        .map(c -> c.department == null ? c : new Collaborator(c.id, c.name, c.lastName, c.telephoneNumber, c.emailAddress, null, c.isActive))
                        .toList();
                dataStore.updateAll(Collaborator.class, newCollaborators, false);
                fieldMappings.put("collaborators", newCollaborators);
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
            String message;

            if (departmentsToChoose.isEmpty()) {
                message = "You can type 'Y' to proceed with the deletion, or 'N' to stop this action.\n";
            } else {
                String departmentsInfo = formatter.formatMany(departmentsToChoose, Formatter.FORMAT_MINIMUM, 4);
                message = String.format("""
                        You can type 'Y' to proceed with the deletion, 'N' to stop this action or provide a new
                        department ID for the affected collaborators from the following list:
                        %s""", departmentsInfo);
            }

            String confirmation = askForInputLoop(String.format("""
                        Deleting this project will affect the following sprints:
                        %s
                        %s
                        Choose an option:\s""", collaboratorsInfo, message),
                    "Option was invalid or there was no department with the entered ID.\nTry again: ",
                    arg -> arg.equalsIgnoreCase("Y") || arg.equalsIgnoreCase("N") ||
                            (allDepartments.containsKey(arg.toUpperCase()) && !arg.equalsIgnoreCase(department.id)));
            do {
                String selectedOptionUppercase = confirmation.toUpperCase();
                if (selectedOptionUppercase.equals("Y")) {
                    return super.delete(id);
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
}
