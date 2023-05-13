package com.una.programming_two_first_project.formatter;

import com.una.programming_two_first_project.model.Model;

import java.util.Collection;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public String indent(String input, int indent) {
        // indent() from java.lang.String class, changed to not include a new "new line" character on every line
        // and removed unnecessary details
        if (input.isEmpty()) {
            return "";
        }
        Stream<String> stream = input.lines();
//        if (indent > 0) {
            final String spaces = " ".repeat(indent);
            stream = stream.map(s -> spaces + s);
//        }
//        else if (indent == Integer.MIN_VALUE) {
//            stream = stream.map(s -> s.stripLeading());
//        }
//        else if (n < 0) {
//            stream = stream.map(s -> s.substring(Math.min(-indent, s.indexOfNonWhitespace())));
//        }
        return stream.collect(Collectors.joining("\n", "", ""));
    }
}
