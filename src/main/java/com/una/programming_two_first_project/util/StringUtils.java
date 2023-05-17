package com.una.programming_two_first_project.util;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StringUtils
{
    public static String indent(String input, int indent) {
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
