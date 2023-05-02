package com.una.programming_two_first_project.model;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Function;

public class Command<TArg> extends Token
{
    public final Function<TArg, String> function;
    public final Map<String, Option> args;

    public Command(@NotNull String name, @NotNull String description, @NotNull Function<TArg, String> function,
                   @NotNull Map<String, Option> args) {
        super(name, description);
        this.function = function;
        this.args = args;
    }

    public Option getArgument(String key) {
        // NOTE: Accessing args.get(key) from outside this class requires casting the return value because for some
        // reason it returns Object instead of Option
        return args.get(key);
    }
}
