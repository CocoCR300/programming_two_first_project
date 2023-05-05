package com.una.programming_two_first_project.service;

import com.una.programming_two_first_project.model.Model;
import com.una.programming_two_first_project.model.Result;

import java.util.Map;
import java.util.Optional;

public interface DataStore
{
    String ENTITY_ALREADY_EXISTS = "ENTITY_ALREADY_EXISTS";
    String ENTITY_DOES_NOT_EXIST = "ENTITY_DOES_NOT_EXIST";
    String MODEL_NOT_FOUND = "MODEL_NOT_FOUND";

    <T extends Model> Result<T, String> add(T newEntity);
    <T extends Model> Result<T, String> delete(Class<T> modelClass, String id);
    <T extends Model> Result<Optional<T>, String> get(Class<T> modelClass, String id);
//    public <T extends Model> Result<Optional<T>, String> get(String modelKey, String id);
    <T extends Model> Result<Map<String, T>, String> getAll(Class<T> modelClass);
//    public <T extends Model> Result<Map<String, T>, String> getAll(String modelKey);
    void commitChanges();
}
