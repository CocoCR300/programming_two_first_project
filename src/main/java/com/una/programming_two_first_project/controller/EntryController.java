package com.una.programming_two_first_project.controller;

import com.una.programming_two_first_project.model.ArgsCapableOption;
import com.una.programming_two_first_project.model.Option;

import java.util.List;

public interface EntryController extends Controller
{
    List<Option> getOptions();
    void registerControllerOption(String key, String shortKey, String description, Class<? extends ArgsCapableController> controllerType);
}
