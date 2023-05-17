package com.una.programming_two_first_project.formatter;

import com.una.programming_two_first_project.data_store.Model;

import java.util.Collection;
import java.util.function.BiFunction;

public abstract class BaseFormatter<T extends Model> implements Formatter<T>
{
    protected abstract String getModelNameLowercase();

    public String formatMany(Collection<T> entities, int formatType, int indent) {
        String modelNameLowercase = getModelNameLowercase();
        if (entities.size() == 0) {
            return String.format("There are no %ss.", modelNameLowercase);
        }

        BiFunction<T, Integer, String> formatter;
        if (formatType == FORMAT_FULL) {
            formatter = this::formatFull;
        } else {
            formatter = this::formatMinimum;
        }

        StringBuilder builder = new StringBuilder();
        for (T entity : entities) {
            builder.append(formatter.apply(entity, indent));
            builder.append(System.lineSeparator());
        }

        return builder.toString();
    }
}
