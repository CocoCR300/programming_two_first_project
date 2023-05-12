package com.una.programming_two_first_project.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Function;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface PrimaryKey
{
    boolean autogenerate() default true;
    String composerMethodName() default "";
    String[] composerAttributeNames() default "";
}
