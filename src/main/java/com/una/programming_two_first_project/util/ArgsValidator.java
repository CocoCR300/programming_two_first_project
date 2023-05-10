package com.una.programming_two_first_project.util;

import com.una.programming_two_first_project.model.Result;
import org.jetbrains.annotations.NotNull;

public class ArgsValidator
{
    public static Result<Object, String> isNotBlank(@NotNull String arg) {
        return arg.isBlank() ? Result.err("Argument for option '%s' cannot be empty.") : Result.ok(arg);
    }
}
