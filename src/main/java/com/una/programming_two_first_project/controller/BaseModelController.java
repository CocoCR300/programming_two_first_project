package com.una.programming_two_first_project.controller;

import com.una.programming_two_first_project.model.*;
import com.una.programming_two_first_project.service.DataStore;
import com.una.programming_two_first_project.util.EntityCreator;
import com.una.programming_two_first_project.util.TokenResolver;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public abstract class BaseModelController<T extends Model> implements ModelController
{
    protected final Class<T> modelClass;
    protected final DataStore dataStore;
    private final String modelNameLowercase;
    protected final TokenResolver tokenResolver;


    public BaseModelController(@NotNull Class<T> modelClass, @NotNull DataStore dataStore, @NotNull TokenResolver tokenResolver) {
        this.modelClass = modelClass;
        this.dataStore = dataStore;
        this.tokenResolver = tokenResolver;

        modelNameLowercase = modelClass.getSimpleName().toLowerCase();
    }

    protected abstract Command getAddCommand();
    protected abstract Command getHelpCommand();

    protected String add(Map<String, Object> argsByOptionName) {
        Result<Map<String, Object>, Tuple<String, String[]>> result = tokenResolver.mapCommandArgsToModelFields(getAddCommand(), modelClass, argsByOptionName, null);

        return result.mapOrElse(fieldMappings -> {
            try {
                Model entity = EntityCreator.newInstance(modelClass, fieldMappings).unwrap();
                return dataStore.add(entity).mapOrElse(c -> {
                    Result<Integer, Exception> commitResult = dataStore.commitChanges();
                    return commitResult.mapOrElse(
                            i -> "Operation completed successfully.",
                            e -> "An error occurred. Please contact the developers.");
                }, e -> {
                    if (e.equals(DataStore.ENTITY_ALREADY_EXISTS)) {
                        return String.format("A entity with ID: %s already exists.", entity.getId());
                    } else if (e.equals(DataStore.ENTITY_ALREADY_RELATED)) {
                        return String.format("A collaborator in the ID list already has a entity assigned.");
                    }

                    return "";
                });
                // DataStore.ENTITY_ALREADY_EXISTS is the only type of error result that can be received here, for now.

            } catch (Exception ex) {
                return String.format("An error occurred: %s", ex);
            }
        }, e -> {
            String errorType = e.x();
            String[] errorArguments = e.y();
            switch (errorType) {
                case TokenResolver.REQUIRED_OPTION_NOT_PRESENT:
                    return String.format("'%s' option is required for add command", errorArguments[0]);
                case TokenResolver.ID_DOES_NOT_EXIST:
                    return String.format("There is no %s with ID: %s", errorArguments[0], errorArguments[1]);
                case TokenResolver.SOME_IDS_DO_NOT_EXIST:
                    return String.format("There are no %s with IDs: %s", errorArguments[0], errorArguments[1]);
            }

            return "";
        });
    }

    protected String delete(String id) {
        Result<T, String> result = dataStore.delete(modelClass, id);
        return result.mapOrElse(c -> {
            Result<Integer, Exception> commitResult = dataStore.commitChanges();
            return commitResult.mapOrElse(i -> "Operation completed successfully.",
                    e -> "An error occurred. Please contact the developers.");
        }, e -> String.format("A %s with ID '%s' does not exist.", modelNameLowercase, id));
    }

    @Override
    public String resolveArgs(String[] args) {
        if (args.length > 0) {

            Result<Tuple<Command, Map<String, Object>>, String> result = tokenResolver.extractCommandAndArgs(args, getCommands(), getHelpCommand());

            String resultMessage = (String) result.map(t -> {
                // TODO: Command may be null
                Command command = t.x();
                Map<String, Object> commandArgs = t.y();

                if (command.optionsCount() == 1) {
                    if (commandArgs.size() != 1) {
                        return String.format("Expected 1 argument for command %s, found %s.", command.name, commandArgs.size());
                    }

                    Option onlyOption = (Option) command.getOptionAt(0).unwrap();
                    String onlyArgKey = onlyOption.name;
                    return command.function.apply(commandArgs.get(onlyArgKey));
                }

                return command.function.apply(t.y());
            }).unwrapSafe();

            return resultMessage;
        }

        return "";
    }
}
