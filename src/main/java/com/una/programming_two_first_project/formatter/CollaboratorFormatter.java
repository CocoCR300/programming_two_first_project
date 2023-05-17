package com.una.programming_two_first_project.formatter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.una.programming_two_first_project.model.Collaborator;
import com.una.programming_two_first_project.util.StringUtils;

@Singleton
public class CollaboratorFormatter extends BaseFormatter<Collaborator>
{
    @Inject private DepartmentFormatter departmentFormatter;
    @Inject private TaskFormatter taskFormatter;

    @Override
    protected String getModelNameLowercase() {
        return "departments";
    }

    public String formatFull(Collaborator collaborator, int indent) {
        String departmentInfo;
        String tasksInfo;

        if (collaborator.department != null) {
            departmentInfo = String.format("Department: %s", departmentFormatter.formatMinimum(collaborator.department, 0));
        } else {
            departmentInfo = "This collaborator is not in any department.";
        }

        if (collaborator.tasks.isEmpty()) {
            tasksInfo = "This collaborator has no tasks.";
        } else {
            tasksInfo = String.format("Tasks:\n%s", taskFormatter.formatMany(collaborator.tasks, FORMAT_MINIMUM, 4));
        }

        return StringUtils.indent(
                String.format("""
                      [Collaborator ID: %s]
                        Name:              %s
                        Last name:         %s
                        Telephone number:  %s
                        Email address:     %s
                        %s
                        %s
                        %s""",
                        collaborator.id, collaborator.name, collaborator.lastName, collaborator.telephoneNumber,
                        collaborator.emailAddress, departmentInfo,
                        String.format("This collaborator is %s", collaborator.isActive ? "active" : "inactive"),
                        tasksInfo),
                indent);
    }

    public String formatMinimum(Collaborator collaborator, int indent) {
        return StringUtils.indent(
                String.format("[Collaborator ID: %s] %s %s\n  %s",
                        collaborator.id, collaborator.name, collaborator.lastName,
                        String.format("This collaborator is %s", collaborator.isActive ? "active" : "inactive")),
                indent);
    }
}
