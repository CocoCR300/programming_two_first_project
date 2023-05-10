package com.una.programming_two_first_project.util;

import com.una.programming_two_first_project.model.Option;
import com.una.programming_two_first_project.model.Token;
import com.una.programming_two_first_project.model.Tuple;

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
    public static <T extends Token> Map<String, Tuple<T, Boolean>> generateMap(Tuple<T, Boolean>... tokens) {
        Map<String, Tuple<T, Boolean>> optionMap = new HashMap<>(tokens.length);

        for (Tuple<T, Boolean> tuple : tokens) {
            T token = tuple.x();
            optionMap.put(token.name, tuple);

            if (token instanceof Option option) {
                optionMap.put(option.shortName, tuple);
            }
        }

        return optionMap;
    }
}
