package com.una.programming_two_first_project.util;

public class Defaults
{
    public static <T> T getDefault(Class<T> objectClass) {
        T defaultValue = null;

        if (objectClass.isPrimitive()) {
            if (objectClass == boolean.class) {
                defaultValue = (T) Boolean.FALSE;
            } else if (objectClass == int.class) {
                defaultValue = (T) Integer.valueOf(0);
            }

        } else if (objectClass == String.class) {
            defaultValue = (T) "";
        }

        return defaultValue;
    }
}
