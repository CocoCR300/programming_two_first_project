package com.una.programming_two_first_project.controller;

import com.una.programming_two_first_project.data_store.Model;
import com.una.programming_two_first_project.model.*;
import com.una.programming_two_first_project.data_store.DataStore;
import com.una.programming_two_first_project.formatter.Formatter;
import com.una.programming_two_first_project.util.EntityCreator;
import com.una.programming_two_first_project.util.StringUtils;
import com.una.programming_two_first_project.util.TokenResolver;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public abstract class BaseModelController<T extends Model> implements ModelController
{
    protected final Class<T> modelClass;

    protected final Option commandNameOption = new Option("command-name", "n", "");
    protected final Command<String> helpCommand = new Command<>("help", "", this::getHelp,
            new Option[] { commandNameOption }, null);
    protected final Command listCommand;
    protected final DataStore dataStore;
    protected final Formatter<T> formatter;
    protected final Map<String, Command> commands;
    protected final EntryController parentEntryController;
    protected final TokenResolver tokenResolver;

    private final String modelNameLowercase;


    public BaseModelController(@NotNull Class<T> modelClass, @NotNull DataStore dataStore, @NotNull Formatter<T> formatter, @NotNull EntryController entryController, @NotNull TokenResolver tokenResolver) {
        this.modelClass = modelClass;
        this.dataStore = dataStore;
        this.formatter = formatter;
        this.parentEntryController = entryController;
        this.tokenResolver = tokenResolver;
        modelNameLowercase = modelClass.getSimpleName().toLowerCase();
        listCommand = new Command("list", String.format("List all the %ss in the system.", modelNameLowercase), this::list, null, null);

        commands = new HashMap<>();
        commands.put(helpCommand.name, helpCommand);
        commands.put(listCommand.name, listCommand);
    }

    private String list(Object dummy) {
        Result<String, String> result = dataStore
                .getAll(modelClass)
                .map(m -> formatEntities(m.values()));

        return result.unwrap();
    }

    protected abstract Command getAddCommand();
    protected abstract Result<Map<String, Object>, String> verifyFieldMappings(Map<String, Object> fieldMappings);
    protected abstract String getExampleCommand(String commandName);

    protected String add(Map<String, String> argsByOptionName) {
        Result<Map<String, Object>, String> result = tokenResolver
                .mapCommandArgsToModelFields(getAddCommand(), modelClass, argsByOptionName, null)
                .mapErr(e -> {
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

                    return "";})
                .andThen(this::verifyFieldMappings);

        return Result.unwrapSafe(result.map(fieldMappings -> {
            Model entity = EntityCreator.newInstance(modelClass, fieldMappings).unwrap();
            return dataStore.add(entity).mapOrElse(c -> {
                Result<Integer, Exception> commitResult = dataStore.commitChanges();
                return commitResult.mapOrElse(
                        i -> "Operation completed successfully.",
                        e -> "An error occurred. Please contact the developers.");
            }, e -> {
                if (e.equals(DataStore.ENTITY_ALREADY_EXISTS)) {
                    return String.format("A %s with ID '%s' already exists.", modelNameLowercase, entity.getId());
                } else if (e.equals(DataStore.ENTITY_ALREADY_RELATED)) {
                    // TODO: Wrong message
                    return String.format("A %s in the ID list already has a %s assigned.",
                            modelNameLowercase, e);
                }

                return "";
            });
        }));
    }

    protected String delete(String id) {
        Result<T, String> result = dataStore.delete(modelClass, id);
        return result.mapOrElse(c -> {
            Result<Integer, Exception> commitResult = dataStore.commitChanges();
            return commitResult.mapOrElse(
                    i -> "Operation completed successfully.",
                    e -> "An error occurred. Please contact the developers.");
        }, e -> String.format("A %s with ID '%s' does not exist.", modelNameLowercase, id));
    }

    @Override
    public Command[] getCommands() {
        return commands.values().toArray(Command[]::new);
    }

    @Override
    public String askForInput(String message) {
        return parentEntryController.askForInput(message);
    }

    public String formatEntity(T entity) {
        return formatter.formatFull(entity, 0);
    }

    public String formatEntities(Collection<T> entities) {
        return formatter.formatMany(entities, Formatter.FORMAT_FULL, 0);
    }

    @Override
    public String getHelp(String tokenName) {
        Command command = commands.get(tokenName);

        if (command != null) {
            StringBuilder helpBuilder = new StringBuilder(String.format("""
                %s command

                Usage: management.exe %s [item] [option(s)]
                """,
                    tokenName, tokenName));

            if (command.optionsCount() == 0) {
                helpBuilder.append("\nThis command has no options.");
            } else {
                StringBuilder optionalOptionsInfo = new StringBuilder();
                StringBuilder requiredOptionsInfo = new StringBuilder();

                if (command.requiredOptionsCount() > 0) {
                    requiredOptionsInfo.append("\nRequired options:\n");
                }

                if (command.optionalOptionsCount() > 0) {
                    requiredOptionsInfo.append("\nOptional options:\n");
                }

                int maxLengthName = 0;
                for (int i = 0; i < command.optionsCount(); ++i) {
                    Option option = (Option) command.getOptionAt(i).unwrap();
                    if (maxLengthName < option.name.length()) {
                        maxLengthName = option.name.length();
                    }
                }

                for (int i = 0; i < command.optionsCount(); ++i) {
                    Option option = (Option) command.getOptionAt(i).unwrap();
                    String optionInfo = String.format("  %s|%s%s\n", option.shortName, option.name,
                            option.description.indent(maxLengthName - option.name.length() + 2));

                    if ((boolean) command.isRequired(option).get()) {
                        requiredOptionsInfo.append(optionInfo);
                    } else {
                        optionalOptionsInfo.append(optionInfo);
                    }
                }

                helpBuilder.append(requiredOptionsInfo);
                helpBuilder.append(optionalOptionsInfo);
                helpBuilder.append("""
                All options (except those where explicitly stated otherwise) must be followed by a value.
                You can omit the options and write the values in the same order as the list above.
                When writing values with a space between them, you must enclose the whole value in double quotes.""");
            }

            helpBuilder.append("\n");
            helpBuilder.append(StringUtils.indent(getExampleCommand(tokenName), 2));
            return helpBuilder.toString();
        }

        return String.format("Unrecognized command '%s'", tokenName);
    }

    @Override
    public String resolveArgs(String[] args) {
        if (args.length > 0) {

            Result<Tuple<Command, Map<String, Object>>, String> result = tokenResolver.extractCommandAndArgs(args, commands, helpCommand);

            String resultMessage = Result.unwrapSafe(result.map(t -> {
                // TODO: Command may be null
                Command command = t.x();
                Map<String, Object> commandArgs = t.y();

                if (commandArgs.size() == 1) {
//                    if (commandArgs.size() != 1) {
//                        return String.format("Expected 1 argument for command %s, found %s.", command.name, commandArgs.size());
//                    }

                    Option onlyOption = (Option) command.getOptionAt(0).unwrap();
                    String onlyArgKey = onlyOption.name;
                    return (String) command.function.apply(commandArgs.get(onlyArgKey));
                }

                return (String) command.function.apply(t.y());
            }));

            return resultMessage;
        }

        return "";
    }
}
