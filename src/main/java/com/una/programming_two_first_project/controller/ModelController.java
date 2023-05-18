package com.una.programming_two_first_project.controller;

import com.una.programming_two_first_project.model.Command;

import java.util.List;

public interface ModelController extends Controller
{
    Command[] getCommands();
}
