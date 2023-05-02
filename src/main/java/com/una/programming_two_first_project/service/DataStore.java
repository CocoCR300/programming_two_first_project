package com.una.programming_two_first_project.service;

import com.una.programming_two_first_project.model.Model;
import com.una.programming_two_first_project.model.Result;

import java.util.Map;

public interface DataStore
{
    public static final String ENTITY_NOT_FOUND = "ENTITY_NOT_FOUND";
    public static final String MODEL_NOT_FOUND = "MODEL_NOT_FOUND";

    public <T extends Model> Result<T, String> get(String modelKey, String id);
    public <T extends Model> Result<Map<String, T>, String> getAll(String modelKey);
}
