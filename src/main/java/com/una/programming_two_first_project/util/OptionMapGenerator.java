package com.una.programming_two_first_project.util;

import com.una.programming_two_first_project.model.Option;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OptionMapGenerator {
    public static Map<String, Option> generateMap(List<Option> options) {
        Map<String, Option> optionMap = new HashMap<>(options.size());

        for (Option option : options) {
            optionMap.put(option.name, option);
            optionMap.put(option.shortName, option);
        }

        return optionMap;
    }

    @SafeVarargs
    public static <T extends Option> Map<String, T> generateMap(T... options) {
        Map<String, T> optionMap = new HashMap<>(options.length);

        for (T option : options) {
            optionMap.put(option.name, option);
            optionMap.put(option.shortName, option);
        }

        return optionMap;
    }
}
