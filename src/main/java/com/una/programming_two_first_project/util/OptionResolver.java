package com.una.programming_two_first_project.util;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OptionResolver {
    public static @NotNull Map.Entry<Boolean, String> extractOptionName(String optionArg) {
        if (optionArg.startsWith("--")) {
            return Map.entry(true, optionArg.substring(2));
        } else if (optionArg.startsWith("-")) {
            return Map.entry(true, optionArg.substring(1));
        }

        return Map.entry(false, "");
    }

    public static @NotNull String[] extractOptionNames(String[] args) {
        List<String> optionNames = new ArrayList<>();

        for (int i = 0; i < args.length; ++i) {
            Map.Entry<Boolean, String> optionName = extractOptionName(args[i]);

            if (!optionName.getKey()) {
                optionNames.add(optionName.getValue());
            }
        }

        return optionNames.toArray(String[]::new);
    }
}
