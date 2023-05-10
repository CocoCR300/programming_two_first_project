package com.una.programming_two_first_project.model;

import com.una.programming_two_first_project.util.TokenMapGenerator;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.una.programming_two_first_project.model.Tuple.tuple;

public class Command<TArg> extends Token
{
    public static final String ARG_INDEX_OUT_OF_BOUNDS = "ARG_INDEX_OUT_OF_BOUNDS";

    private final Map<String, Tuple<Option, Boolean>> argsMap;

    public final Function<TArg, String> function;
    public final Option[] optionalArgs;
    public final Option[] requiredArgs;

    public Command(@NotNull String name, @NotNull String description, @NotNull Function<TArg, String> function,
                   @Nullable Option[] requiredArgs, @Nullable Option[] optionalArgs) {
        super(name, description);
        this.function = function;

        if (requiredArgs != null) {
            this.requiredArgs = requiredArgs;
        } else {
            this.requiredArgs = new Option[0];
        }

        if (optionalArgs != null) {
            this.optionalArgs = optionalArgs;
        } else {
            this.optionalArgs = new Option[0];
        }

        @SuppressWarnings("unchecked")
        Tuple<Option, Boolean>[] allArgs = Stream.concat(Arrays.stream(this.requiredArgs).map(o -> tuple(o, true)),
                                                        Arrays.stream(this.optionalArgs).map(o -> tuple(o, false)))
                                                .toArray(Tuple[]::new);
        argsMap = TokenMapGenerator.generateMap(allArgs);
    }

    public int optionsCount() {
        return requiredArgs.length + optionalArgs.length;
    }

    public Optional<Boolean> isRequired(Option option) {
        Tuple<Option, Boolean> tuple = argsMap.get(option.name);

        if (tuple != null) {
            return Optional.of(tuple.y());
        }

        return Optional.empty();
    }

    public Optional<Option> getOption(@NotNull String key) {
        Tuple<Option, Boolean> tuple = argsMap.get(key);

        if (tuple != null) {
            return Optional.of(tuple.x());
        }

        return Optional.empty();
    }

    public Result<Option, String> getOptionAt(int index) {
        if (index < 0 || index >= requiredArgs.length + optionalArgs.length) {
            return Result.err(ARG_INDEX_OUT_OF_BOUNDS);
        }

        if (index < requiredArgs.length) {
            return Result.ok(requiredArgs[index]);
        }

        return Result.ok(optionalArgs[index]);
    }
}
