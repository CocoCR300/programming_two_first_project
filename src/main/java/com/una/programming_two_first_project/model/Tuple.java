package com.una.programming_two_first_project.model;

public record Tuple<T1, T2>(T1 x, T2 y)
{
    public static <T1, T2> Tuple<T1, T2> tuple(T1 x, T2 y) { return new Tuple<>(x, y); }
}
