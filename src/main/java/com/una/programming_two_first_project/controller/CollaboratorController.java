package com.una.programming_two_first_project.controller;


import com.una.programming_two_first_project.model.NoArgsOption;
import com.una.programming_two_first_project.model.Option;
import com.una.programming_two_first_project.util.OptionMapGenerator;
import com.una.programming_two_first_project.util.OptionResolver;

import java.util.List;
import java.util.Map;

public class CollaboratorController implements ArgsCapableController
{
    public final NoArgsOption HelpOption = new NoArgsOption("help", "h", "", this::getHelp);

    private final List<Option> Options = List.of(HelpOption);
    private final Map<String, Option> OptionsMap = OptionMapGenerator.generateMap(Options);

    @Override
    public String getHelp() {
        return "Collaborator management";
    }

    @Override
    public String resolveArgs(String[] args) {
        if (args.length > 0) {
            String selectedOptionName = OptionResolver.extractOptionName(args[0]);

            if (!OptionsMap.containsKey(selectedOptionName)) {
                return "Invalid option: " + args[0];
            }

            if (args.length == 1) {
                Option option = OptionsMap.get(selectedOptionName);

                if (option instanceof NoArgsOption noArgsOption) {
                    return noArgsOption.function.get();
                }
            }
        }

        return "";
    }

    @Override
    public void selectOption(int optionIndex) {

    }

    @Override
    public String getOptionInfo(String option) {
        return "";
    }
}
