package com.una.programming_two_first_project.model;

import java.time.OffsetDateTime;

public class Project {

    public final OffsetDateTime endDateTime, startDateTime;
    public final String code, name;

    public Project(String code, String name, OffsetDateTime startDateTime, OffsetDateTime endDateTime) {
        this.code = code;
        this.name = name;
        this.endDateTime = endDateTime;
        this.startDateTime = startDateTime;
    }
}
