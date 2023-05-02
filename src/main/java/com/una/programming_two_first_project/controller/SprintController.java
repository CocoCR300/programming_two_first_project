package com.una.programming_two_first_project.controller;

import com.una.programming_two_first_project.model.Command;

import java.util.Map;

public class SprintController implements ArgsCapableController
{
    private Map<String, Command> optionsByEntryOptionName;

    private void add(String[] args) {

    }

    private void edit(String[] args) {

    }

    private void list() {

    }

    private void remove(String[] args) {

    }

    @Override
    public String getCommandInfo(String command) {
        return optionsByEntryOptionName.get(command).description;
    }

    @Override
    public String getHelp(String tokenName) {
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
