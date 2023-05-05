package com.una.programming_two_first_project.model;

public class Collaborator implements Model {

    public final boolean isActive;
    public final transient Department department;
    public final String departmentId, emailAddress, id, name, lastName, telephoneNumber;

    public Collaborator() {
        isActive = false;
        department = null;
        departmentId = emailAddress = id = name = lastName = telephoneNumber = "";
    }

    public Collaborator(String id, String name, String lastName,
                        String telephoneNumber, String emailAddress,
                        Department department, boolean isActive) {
        this.id = id;
        this.name = name;
        this.lastName = lastName;
        this.telephoneNumber = telephoneNumber;
        this.emailAddress = emailAddress;
        this.department = department;
        this.isActive = isActive;

        if (department != null) {
            departmentId = department.id;
        } else {
            departmentId = "";
        }
    }

    @Override
    public String getId() {
        return id;
    }
}
