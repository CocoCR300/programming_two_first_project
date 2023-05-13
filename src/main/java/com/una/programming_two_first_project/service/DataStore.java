package com.una.programming_two_first_project.service;

import com.una.programming_two_first_project.model.Model;
import com.una.programming_two_first_project.model.Result;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface DataStore
{
    String ENTITY_ALREADY_EXISTS = "ENTITY_ALREADY_EXISTS";
    String ENTITY_DOES_NOT_EXIST = "ENTITY_DOES_NOT_EXIST";
    String ENTITY_ALREADY_RELATED = "ENTITY_ALREADY_RELATED";
    String MODEL_NOT_FOUND = "MODEL_NOT_FOUND";

    Optional<Class<? extends Model>> getClassFromSimpleName(String modelClassSimpleName);
    <T extends Model> Result<T, String> add(T newEntity);
    <T extends Model> Result<T, String> delete(Class<T> modelClass, String id);
    <T extends Model> Result<Optional<T>, String> get(Class<T> modelClass, String id);
//    public <T extends Model> Result<Optional<T>, String> get(String modelKey, String id);
    <T extends Model> Result<Map<String, T>, String> getAll(Class<T> modelClass);
//    public <T extends Model> Result<Map<String, T>, String> getAll(String modelKey);
    <T extends Model> Result<List<Optional<T>>, String> getMany(Class<T> modelClass, String[] ids);
    <T extends Model> Result<T, String> update(T newEntity, boolean force);
    <T extends Model> Result<List<T>, String> updateAll(Class<T> modelClass, List<T> newEntities, boolean force);
    Result<Integer, Exception> commitChanges();
}
