package com.una.programming_two_first_project.formatter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.una.programming_two_first_project.model.Department;
import com.una.programming_two_first_project.util.StringUtils;

@Singleton
public class DepartmentFormatter extends BaseFormatter<Department>
{
    @Inject private CollaboratorFormatter collaboratorFormatter;

    @Override
    protected String getModelNameLowercase() {
        return "departments";
    }

    @Override
    public String formatFull(Department department, int indent) {
        String collaboratorsInfo;

        if (department.collaborators.isEmpty()) {
            collaboratorsInfo = "This department has no collaborators.";
        } else {
            collaboratorsInfo = String.format("Collaborators:\n%s", collaboratorFormatter.formatMany(department.collaborators, FORMAT_MINIMUM, 0));
        }

        return StringUtils.indent(
                String.format("""
              [Department ID: %s]
                Name:         %s
                %s""", department.id, department.name, collaboratorsInfo),
                indent);
    }

    @Override
    public String formatMinimum(Department department, int indent) {
        return StringUtils.indent(String.format("[Department ID: %s] %s", department.id, department.name), indent);
    }
}
