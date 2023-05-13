//package com.una.programming_two_first_project.model;
//
//import org.jetbrains.annotations.NotNull;
//
//import java.util.function.BiFunction;
//import java.util.function.Function;
//
//public class TypedOption extends ConvertibleArgumentOption
//{
//    public final Function<String, Result<Object, String>> converterFunction;
//
//    public TypedOption(@NotNull String name, @NotNull String shortName, @NotNull String description, boolean isRequired,
//                       BiFunction<String, String, Result<Object, String>> converterFunction, Function<String, Result<Object, String>> converterFunction) {
//        super(name, shortName, description, isRequired, converterFunction);
//
//        this.converterFunction = converterFunction;
//    }
//}
