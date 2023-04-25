package com.una.programming_two_first_project.controller;

import com.una.programming_two_first_project.model.ArgsCapableOption;

import java.util.Map;

public class SprintController implements ArgsCapableController
{
    private Map<String, ArgsCapableOption> optionsByEntryOptionName;

    private void add(String[] args) {

    }

    private void edit(String[] args) {

    }

    private void list() {

    }

    private void remove(String[] args) {

    }

    @Override
    public String getOptionInfo(String option) {
        return optionsByEntryOptionName.get(option).description;
    }

    @Override
    public String getHelp() {
        return null;
    }

    @Override
    public String resolveArgs(String[] args) {
        return "";
    }

    @Override
    public void selectOption(int optionIndex) {

    }
}
