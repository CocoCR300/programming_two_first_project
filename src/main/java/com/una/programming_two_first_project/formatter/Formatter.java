package com.una.programming_two_first_project.formatter;

import com.una.programming_two_first_project.model.Collaborator;
import com.una.programming_two_first_project.model.Model;

import java.time.chrono.Chronology;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Collection;
import java.util.Locale;

public interface Formatter<T extends Model>
{
    static DateTimeFormatter defaultDateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(
            FormatStyle.SHORT, FormatStyle.SHORT);

    int FORMAT_FULL = 0;
    int FORMAT_MINIMUM = 1;

    String formatFull(T entity, int indent);

    String formatMany(Collection<T> entities, int formatType, int indent);

    String formatMinimum(T entity, int indent);
}
