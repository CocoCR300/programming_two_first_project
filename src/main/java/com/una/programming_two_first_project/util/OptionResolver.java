package com.una.programming_two_first_project.util;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class OptionResolver {
    public static @NotNull String extractOptionName(String optionArg) {
        String optionName = "";

        if (optionArg.startsWith("--")) {
            optionName = optionArg.substring(2);
        } else if (optionArg.startsWith("-")) {
            optionName = optionArg.substring(1);
        }

        return optionName;
    }

    public static @NotNull String[] extractOptionNames(String[] args) {
        List<String> optionNames = new ArrayList<>();

        for (int i = 0; i < args.length; ++i) {
            String optionName = extractOptionName(args[i]);

            if (!optionName.isEmpty()) {
                optionNames.add(optionName);
            }
        }

        return optionNames.toArray(String[]::new);
    }
}
