package com.una.programming_two_first_project.model;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
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

    public T okOrElse(T elseValue) {
        return isOk() ? result : elseValue;
    }

    public boolean isErr() {
        return errorValue != null;
    }

    public boolean isOk() {
        return errorValue == null;
    }

    public Object unwrapSafe() {
        if (isOk()) {
            return result;
        }

        return errorValue;
    }

    @Override
    public String toString() {
        Object value;
        String variantName;
        if (isOk()) {
            variantName = "ok";
            value = result;
        } else {
            variantName = "err";
            value = errorValue;
        }

        return String.format("Result.%s(%s)", variantName, value);
    }

    public <U> Result<U, E> and(Result<U, E> otherResult) {
        if (isOk()) {
            return otherResult;
        }

        return Result.err(errorValue);
    }

    public Result<T, E> inspect(Consumer<T> function) {
        if (isOk()) {
            function.accept(result);
        }

        return this;
    }

    public T unwrap() {
        if (isOk()) {
            return result;
        }

        throw new IllegalStateException("Attempted to access result on an Error variant");
    }

    public E unwrapErr() {
        if (isErr()) {
            return errorValue;
        }

        throw new IllegalStateException("Attempted to access the error value on an Ok variant");
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

    public <F> Result<T, F> mapErr(Function<E, F> mapperFunction) {
        if (isErr()) {
            return err(mapperFunction.apply(errorValue));
        }

        return ok(result);
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

    public static <T, E> Result<T, E> ok(@Nullable T result) {
        return new Result<>(result, null);
    }

    public static <T> T unwrapSafe(@NotNull Result<T, T> result) {
        if (result.isOk()) {
            return result.result;
        }

        return result.errorValue;
    }
}
