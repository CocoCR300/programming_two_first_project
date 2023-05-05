package com.una.programming_two_first_project.model;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Function;

public class Result<T, E>
{
    private final T result;
    private final E errorValue;

    private Result(T result, E errorValue) {
        this.result = result;
        this.errorValue = errorValue;
    }

    public T get() {
        if (isOk()) {
            return result;
        }

        throw new IllegalStateException("Attempted to access result on an error variant");
    }

    public T okOrElse(T elseValue) {
        return isOk() ? result : elseValue;
    }

    public boolean isErr() {
        return errorValue != null;
    }

    public boolean isOk() {
        return result != null;
    }

    public Object unwrapSafe() {
        if (isOk()) {
            return result;
        }

        return errorValue;
    }

    public Result<T, E> inspect(Consumer<T> function) {
        if (isOk()) {
            function.accept(result);
        }

        return this;
    }

    public T unwrapOr(T defaultValue) {
        if (isOk()) {
            return result;
        }

        return defaultValue;
    }

    public <U> Result<U, E> andThen(Function<T, Result<U, E>> function) {
        if (isOk()) {
            return function.apply(result);
        }

        return err(errorValue);
    }

    public <U> Result<U, E> map(Function<T, U> mapperFunction) {
        if (isOk()) {
            return ok(mapperFunction.apply(result));
        }

        return err(errorValue);
    }

    public <U> U mapOrElse(Function<T, U> okMapperFunction, Function<E, U> errMapperFunction) {
        if (isOk()) {
            return okMapperFunction.apply(result);
        }

        return errMapperFunction.apply(errorValue);
    }

    public static <T, E> Result<T, E> err(@NotNull E errorValue) {
        return new Result<>(null, errorValue);
    }

    public static <T, E> Result<T, E> ok(@NotNull T result) {
        return new Result<>(result, null);
    }
}
