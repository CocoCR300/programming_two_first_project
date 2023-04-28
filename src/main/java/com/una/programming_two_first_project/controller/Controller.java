package com.una.programming_two_first_project.controller;

public interface Controller
{
    String getHelp(String optionName);
    String resolveArgs(String[] args);
    void selectOption(int optionIndex);
}

