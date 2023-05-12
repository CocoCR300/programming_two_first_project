package com.una.programming_two_first_project.annotation;

import com.una.programming_two_first_project.model.Model;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface InverseProperty
{
    Class<? extends Model> relationModelClass();
    String relationModelRelationFieldName();
    String relationModelRelationIdFieldName();
}
