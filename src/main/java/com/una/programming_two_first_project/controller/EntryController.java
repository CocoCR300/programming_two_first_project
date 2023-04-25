package com.una.programming_two_first_project.controller;

import com.una.programming_two_first_project.model.Option;

import java.util.List;

public interface EntryController extends Controller
{
    List<Option> getOptions();
    void registerChildController(String key, String shortKey, Class<? extends ArgsCapableController> controllerType);
}
