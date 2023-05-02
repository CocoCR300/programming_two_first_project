package com.una.programming_two_first_project.controller;

public class ProjectController implements ArgsCapableController
{
    @Override
    public String getHelp(String tokenName) {
        return null;
    }

    @Override
    public String resolveArgs(String[] args) {
        return "";
    }

    @Override
    public String getCommandInfo(String command) {
        return null;
    }

    @Override
    public void selectOption(int optionIndex) {

    }
}
