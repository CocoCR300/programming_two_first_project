package com.una.programming_two_first_project.controller;

public interface Controller
{
    String askForInput(String message);
    String getHelp(String tokenName);
    String resolveArgs(String[] args);
}

