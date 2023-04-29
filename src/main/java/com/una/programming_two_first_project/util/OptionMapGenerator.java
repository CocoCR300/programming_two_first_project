package com.una.programming_two_first_project.util;

import com.una.programming_two_first_project.model.ArgsCapableOption;
import com.una.programming_two_first_project.model.Option;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OptionMapGenerator {
    public static <T extends Option> Map<String, T> generateMap(List<T> options) {
        Map<String, T> optionMap = new HashMap<>(options.size());

        for (T option : options) {
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
