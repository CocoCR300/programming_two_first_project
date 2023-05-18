package com.una.programming_two_first_project.util;

import com.una.programming_two_first_project.data_store.Model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class ControllerBoilerplateHelper
{
    public static boolean validateYesOrNoInput(String input) {
        String inputUppercase = input.toUpperCase();
        return inputUppercase.equals("Y") || inputUppercase.equals("N");
    }

    public static <T extends Model> List<T> checkAlreadyAssignedEntities(List<T> entities,
                                                                         Function<T, Object> relationFieldValueExtractor) {
        List<T> conflictingEntities = new ArrayList<>();
        for (T entity : entities) {
            if (relationFieldValueExtractor.apply(entity) != null) {
                conflictingEntities.add(entity);
            }
        }

        return conflictingEntities;
    }
}
