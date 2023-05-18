package com.una.programming_two_first_project.util;

import com.una.programming_two_first_project.formatter.Formatter;
import com.una.programming_two_first_project.model.Result;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class ArgsValidator
{
    public static Result<LocalDate, String> isDate(@NotNull String arg) {
        try {
            LocalDate localDate = LocalDate.parse(arg, Formatter.defaultDateFormatter);
            return Result.ok(localDate);
        } catch (DateTimeParseException ex) {
            // Not an amazing way to propagate an exception, but I don't want to overcomplicate this API with
            // exception error values
            return Result.err(ex.toString());
        }
    }

    public static Result<String, String> isNotBlank(@NotNull String arg) {
        return arg.isBlank() ? Result.err("Argument for option '%s' cannot be empty.") : Result.ok(arg);
    }

    public static Result<String, String> valid(@NotNull String arg) {
        return Result.ok(arg);
    }
}
