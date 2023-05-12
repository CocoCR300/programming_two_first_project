package com.una.programming_two_first_project.util;

import com.una.programming_two_first_project.model.Model;
import com.una.programming_two_first_project.model.Result;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

public class EntityCreator
{
    public static <T extends Model> Result<T, Exception> newInstance(Class<T> modelClass, Map<String, Object> fieldMappings) {
        try {
            Constructor<T> parameterlessConstructor = modelClass.getConstructor();
            Field[] modelFields = modelClass.getFields();
            T newInstance = parameterlessConstructor.newInstance();

            for (Field field : modelFields) {
                field.setAccessible(true);
                Object fieldValue = fieldMappings.get(field.getName());

                if (!Collection.class.isAssignableFrom(field.getType()) && fieldValue != null) {
                    field.set(newInstance, fieldValue);
                }
            }

            return Result.ok(newInstance);
        } catch (Exception ex) {
            return Result.err(ex);
        }
    }
}
