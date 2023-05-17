package com.una.programming_two_first_project.util;

import com.una.programming_two_first_project.data_store.Model;
import com.una.programming_two_first_project.model.Result;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Map;

public class EntityCreator
{
    private static <T extends Model> Result<T, Exception> setFields(T instance, Map<String, Object> fieldMappings) {
        try {
            @SuppressWarnings("unchecked") Class<T> modelClass = (Class<T>) instance.getClass();
            Field[] modelFields = modelClass.getFields();

            for (Field field : modelFields) {
                field.setAccessible(true);
                Object fieldValue = fieldMappings.get(field.getName());

                if (fieldValue != null) {
                    field.set(instance, fieldValue);
                }
            }

            return Result.ok(instance);
        } catch (Exception ex) {
            return Result.err(ex);
        }
    }

    public static <T extends Model> Result<T, Exception> newInstance(Class<T> modelClass, Map<String, Object> fieldMappings) {
        try {
            Constructor<T> parameterlessConstructor = modelClass.getConstructor();
            T newInstance = parameterlessConstructor.newInstance();
            return setFields(newInstance, fieldMappings);
        } catch (Exception ex) {
            return Result.err(ex);
        }
    }
}
