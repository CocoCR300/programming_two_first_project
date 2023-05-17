package com.una.programming_two_first_project.formatter;

import com.una.programming_two_first_project.data_store.Model;

import java.time.format.DateTimeFormatter;
import java.util.Collection;

public interface Formatter<T extends Model>
{
//    DateTimeFormatter defaultDateFormatter = DateTimeFormatter.ofPattern(
//            DateTimeFormatterBuilder.getLocalizedDateTimePattern(
//                    FormatStyle.SHORT, null, Chronology.ofLocale(Locale.getDefault()), Locale.getDefault()));
    DateTimeFormatter defaultDateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
    static DateTimeFormatter defaultDateFormatter() {
        return DateTimeFormatter.ISO_LOCAL_DATE;
    };

    int FORMAT_FULL = 0;
    int FORMAT_MINIMUM = 1;

    String formatFull(T entity, int indent);

    String formatMany(Collection<T> entities, int formatType, int indent);

    String formatMinimum(T entity, int indent);
}
