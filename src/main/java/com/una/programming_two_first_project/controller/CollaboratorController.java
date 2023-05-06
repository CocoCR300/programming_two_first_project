package com.una.programming_two_first_project.controller;


import com.google.inject.Inject;
import com.una.programming_two_first_project.model.*;
import com.una.programming_two_first_project.service.DataStore;
import com.una.programming_two_first_project.util.TokenMapGenerator;
import com.una.programming_two_first_project.util.TokenResolver;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CollaboratorController implements ArgsCapableController
{
    private final Option departmentIdOption = new Option("department-id", "d", "", false);
    private final Option emailAddressOption = new Option("email-address", "e", "", false);
    private final Option idOption = new Option("id", "i", "", true);
    private final Option isActiveOption = new SwitchOption("is-active", "j", "");
    private final Option nameOption = new Option("name", "n", "", true);
    private final Option lastNameOption = new Option("last-name", "l", "", true);
    private final Option telephoneNumberOption = new Option("telephone-number", "t", "", true);

    private final Command<Map<String, Object>> addCommand = new Command<>("add", "", this::add,
            TokenMapGenerator.generateMap(idOption, nameOption, lastNameOption, telephoneNumberOption, emailAddressOption, departmentIdOption, isActiveOption));
    private final Command<String> deleteCommand = new Command<>("delete", "", this::delete,
            TokenMapGenerator.generateMap(idOption));
    private final Command<String> helpCommand = new Command<>("help", "", this::getHelp,
            TokenMapGenerator.generateMap(nameOption));
    private final Command<Map<String, String>> searchCommand = new Command<>("search", "", this::search,
            TokenMapGenerator.generateMap(departmentIdOption, idOption));

    private final DataStore dataStore;
    private final TokenResolver tokenResolver;
    private final List<Command> commands = List.of(addCommand, deleteCommand, helpCommand, searchCommand);
    private final Map<String, Command> commandsMap = TokenMapGenerator.generateMap(commands);

    @Inject
    public CollaboratorController(DataStore dataStore, TokenResolver tokenResolver) {
        this.dataStore = dataStore;
        this.tokenResolver = tokenResolver;
    }

    public String add(Map<String, Object> argsByOptionName) {
        Constructor<Collaborator>[] modelConstructors = (Constructor<Collaborator>[]) Collaborator.class.getConstructors();
        Constructor<Collaborator> collaboratorConstructor = modelConstructors[1];
        Result<Object[], String> result = tokenResolver.mapCommandArgsToConstructor(addCommand, collaboratorConstructor, argsByOptionName);
        return (String) result.map(constructorArgs -> {
            try {
                Collaborator collaborator = collaboratorConstructor.newInstance(constructorArgs);
                // DataStore.ENTITY_ALREADY_EXISTS is the only type of error that can occur here, for now.
                return dataStore.add(collaborator).mapOrElse(c -> {
                    dataStore.commitChanges();
                    return "Operation completed successfully.";
                }, e -> String.format("A collaborator with ID: %s already exists.", constructorArgs[0]));
            } catch (Exception ex) {
                return String.format("An error occurred: %s", ex);
            }
        }).unwrapSafe();
    }

    public String delete(String id) {
        Result<Collaborator, String> result = dataStore.delete(Collaborator.class, id);
        return result.mapOrElse(c -> {
            dataStore.commitChanges();
            return "Operation completed successfully.";
        }, e -> String.format("A collaborator with ID: %s does not exist.", id));
    }

    @Override
    public String getHelp(String tokenName) {
        return "Collaborator management";
    }

    public String formatEntity(@NotNull Collaborator collaborator) {
        String departmentInfo;

        if (collaborator.department != null) {
            departmentInfo = "Department name:   " + collaborator.department.name;
        } else {
            departmentInfo = "This collaborator is not in any department.";
        }

        return String.format("""
                [Collaborator ID: %s]
                 Name:              %s
                 Last name:         %s
                 Telephone number:  %s
                 Email address:     %s
                 %s
                 %s""",
                collaborator.id, collaborator.name,
                collaborator.lastName, collaborator.telephoneNumber,
                collaborator.emailAddress, departmentInfo,
                String.format("This collaborator is %s", collaborator.isActive ? "active" : "inactive"));
    }

    public String formatEntities(Collection<Collaborator> collaborators) {
        if (collaborators.size() == 0) {
            return "There are no collaborators.";
        }

        StringBuilder builder = new StringBuilder();
        for (Collaborator c : collaborators) {
            builder.append(formatEntity(c));
            builder.append('\n');
        }

        return builder.toString();
    }

    @Override
    public String resolveArgs(String[] args) {
        if (args.length > 0) {

            Result<Tuple<Command, Map<String, Object>>, String> result = tokenResolver.extractCommandAndArgs(args, commandsMap);
//            var argsForConstructor = result.andThen(t -> {
//                Constructor<Collaborator> collaboratorConstructor = (Constructor<Collaborator>) Collaborator.class.getConstructors()[0];
//                Object[] constructorArgs = TokenResolver.mapCommandArgsToConstructor(t.x(), collaboratorConstructor, t.y());
//
//                try {
//                    Collaborator collaborator = collaboratorConstructor.newInstance(constructorArgs);
//                    return Result.ok("Operation completed successfully.");
//                } catch (Exception ex) {
//                    return Result.err(String.format("An error occurred: %s", ex));
//                }
//            });

            String resultMessage = (String) result.map(t -> {
                // TODO: Command may be null
                Command command = t.x();
                Map<String, Object> commandArgs = t.y();

                if (command.args.size() == 2) {
                    if (commandArgs.size() != 1) {
                        return String.format("Expected 1 argument for command %s, found %s.", command.name, commandArgs.size());
                    }

                    Option onlyOption = (Option) command.args.values().stream().findFirst().get();
                    String onlyArgKey = (String) onlyOption.name;
                    return command.function.apply(commandArgs.get(onlyArgKey));
                }

                return command.function.apply(t.y());
            }).unwrapSafe();

            return resultMessage;

//            Command actionOption = null;
//            int indexToSkip = -1;
//            Map<String, Object> argsByName = new HashMap<>();
//
//            for (int i = 0; i != indexToSkip && i < args.length; ++i) {
//                String arg = args[i];
//                Tuple<Boolean, String> optionResolverResult = TokenResolver.extractOptionName(arg);
//
//                if (optionResolverResult.x()) {
//                    String optionName = optionResolverResult.y();
//
//                    if (commandsMap.containsKey(optionName)) {
//                        actionOption = commandsMap.unwrap(optionName);
//                        indexToSkip = i;
//                        i = 0;
//                    } else if (actionOption != null && actionOption.args.containsKey(optionName)) {
//                        Option argumentOption = actionOption.getArgument(optionName);
//                        Object valueForArg;
//
//                        if (argumentOption instanceof SwitchOption) {
//                            valueForArg = true;
//                        } else if (i + 1 < args.length && !TokenResolver.extractOptionName(args[i + 1]).x()) {
//                            valueForArg = args[i++]; // Value found, go to next argument option now
//                        } else {
//                            // Non-switch argument option must be followed by a value, there wasn't enough arguments or
//                            // the argument that followed the current one was an option, warn the user in this case.
//                            return String.format("Missing value for option %s.", arg);
//                        }
//
//                        argsByName.put(argumentOption.name, valueForArg);
//                    } else {
//                        return String.format("Invalid option: %s", arg);
//                    }
//                }
//            }
//
//            if (actionOption != null) {
//                actionOption.function.apply(argsByName);
//
//                // TODO: Move this to TokenResolver? This should be done by the "add" method
//                @SuppressWarnings("unchecked") // Not going to change the array, see the API note for Class<T>.getConstructors()
//                Constructor<Collaborator> controllerConstructor = (Constructor<Collaborator>) Collaborator.class.getConstructors()[0];
//                Stream<Parameter> constructorParameters = Arrays.stream(controllerConstructor.getParameters());
//
//                // Must be "effectively" final to be captured in closure passed to "map" method
//                Command finalActionOption = actionOption;
//                Object[] argsForAction = constructorParameters.map(p -> {
//                    String constructorParameterName = p.getName();
//                    StringBuilder builder = new StringBuilder(constructorParameterName);
//
//                    for (int i = 0; i < builder.length(); ++i) {
//                        char currentChar = builder.charAt(i);
//                        if (Character.isUpperCase(currentChar)) {
//                            builder.replace(i, i + 1, String.valueOf(Character.toLowerCase(currentChar)));
//                            builder.insert(i, '-');
//                        }
//                    }
//
//                    if (Model.class.isAssignableFrom(p.getType())) {
//                        builder.append("-id");
//                    }
//
//                    String argumentOptionName = builder.toString();
//                    Option argumentOption = finalActionOption.getArgument(argumentOptionName);
//                    Object valueForArg = argsByName.unwrap(argumentOptionName);
//
//                    if (valueForArg == null) {
//                        // NOTE: Option for constructor parameter was not provided, so take default value
//                        return argumentOption.defaultValue;
//                    }
//
//                    return valueForArg;
//                }).toArray(Object[]::new);
//                actionOption.function.apply(argsForAction);
//            }

//            String selectedOptionName = args[0];
//
//            if (!commandsMap.containsKey(selectedOptionName)) {
//                return "Invalid option: " + args[0];
//            }
//
//            if (args.length == 1) {
//                Token option = commandsMap.unwrap(selectedOptionName);
//
//                if (option instanceof Command argsCapableOption) {
//                    // TODO: Use this to check a valid cast, maybe... What's the return value for an invalid cast?
//                    String argsCapableOption1 = CastUtils.cast(option);
//                    // TODO: Unchecked cast
//                    return (String) argsCapableOption.function.apply("");
//                }
//            } else {
//                for (int i = 0; i < args.length; ++i) {
//                    if (commandsMap.containsKey(TokenResolver.extractOptionName(args[0]).getValue())) {
//
//                    }
//                }
//            }
        }

        return "";
    }

    // TODO: Should return an Iterable for large amounts of text
    public String search(Map<String, String> argsByOptionName) {
        if (argsByOptionName.size() == 0) {
            Result<String, String> result = dataStore
                    .getAll(Collaborator.class)
                    .map(m -> formatEntities(m.values()));

            return result.unwrap();
        }

        String id = argsByOptionName.values().stream().findFirst().get();
        if (argsByOptionName.containsKey(idOption.name)) {
            Result<Optional<Collaborator>, String> result = dataStore.get(Collaborator.class, id);
            return result
                    .map(o -> o.map(this::formatEntity).orElse(String.format("There are no collaborators with ID: %s", id)))
                    .unwrap();
        } else {
            Result<String, String> result = dataStore
                    .get(Department.class, id)
                    .map(o -> o.map(d -> formatEntities(d.collaborators))
                            .orElse(String.format("There are no departments with ID: %s", id)));
            return result.unwrap();
        }
    }

    @Override
    public void selectOption(int optionIndex) {

    }

    @Override
    public String getCommandInfo(String command) {
        return "";
    }
}
