package com.una.programming_two_first_project.util;

import com.una.programming_two_first_project.model.Option;
import com.una.programming_two_first_project.model.Token;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TokenMapGenerator
{
    public static <T extends Token> Map<String, T> generateMap(List<T> tokens) {
        Map<String, T> optionMap = new HashMap<>(tokens.size());

        for (T token : tokens) {
            optionMap.put(token.name, token);

            if (token instanceof Option option) {
                optionMap.put(option.shortName, token);
            }
        }

        return optionMap;
    }

    @SafeVarargs
    public static <T extends Token> Map<String, T> generateMap(T... tokens) {
        Map<String, T> optionMap = new HashMap<>(tokens.length);

        for (T token : tokens) {
            optionMap.put(token.name, token);

            if (token instanceof Option option) {
                optionMap.put(option.shortName, token);
            }
        }

        return optionMap;
    }
}
