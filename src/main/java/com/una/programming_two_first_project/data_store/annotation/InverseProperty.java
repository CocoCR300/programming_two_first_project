package com.una.programming_two_first_project.data_store.annotation;

import com.una.programming_two_first_project.data_store.Model;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface InverseProperty
{
    Class<? extends Model> relationModelClass();
    String relationModelRelationFieldName();
    String relationModelRelationIdFieldName();
}
