package com.una.programming_two_first_project.controller;

import com.una.programming_two_first_project.model.Token;

import java.util.List;

public interface EntryController extends Controller
{
    List<Token> getCommands();
    void registerControllerOption(String key, String description, Class<? extends ArgsCapableController> controllerType);
}
