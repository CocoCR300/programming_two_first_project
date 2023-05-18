package com.una.programming_two_first_project.model;

import com.una.programming_two_first_project.data_store.Model;
import com.una.programming_two_first_project.data_store.annotation.ForeignKey;
import com.una.programming_two_first_project.data_store.annotation.InverseProperty;
import com.una.programming_two_first_project.data_store.annotation.PrimaryKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Collaborator implements Model
{
    public final boolean isActive;
    public final transient Department department;
    @InverseProperty(relationModelClass = Task.class, relationModelRelationIdFieldName = "collaboratorId",
                     relationModelRelationFieldName = "collaborator")
    public final transient List<Task> tasks;
    public final String emailAddress, name, lastName, telephoneNumber;
    @ForeignKey(relationModelClass = Department.class, relationFieldName = "department")
    public final String departmentId;
    @PrimaryKey(autogenerate = false)
    public final String id;

    public Collaborator() {
        isActive = false;
        department = null;
        departmentId = emailAddress = id = name = lastName = telephoneNumber = "";
        tasks = new ArrayList<>(0);
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
        tasks = new ArrayList<>(0);

        if (department != null) {
            departmentId = department.id;
        } else {
            departmentId = "";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Collaborator that = (Collaborator) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String getId() {
        return id;
    }
}
