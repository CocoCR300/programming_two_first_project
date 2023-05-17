package com.una.programming_two_first_project.formatter;

import com.google.inject.Inject;
import com.una.programming_two_first_project.model.Project;
import com.una.programming_two_first_project.util.StringUtils;

public class ProjectFormatter extends BaseFormatter<Project>
{
    @Inject private SprintFormatter sprintFormatter;

    @Override
    protected String getModelNameLowercase() {
        return "project";
    }

    @Override
    public String formatFull(Project project, int indent) {
        String sprintsInfo;

        if (project.sprints.isEmpty()) {
            sprintsInfo = "This project has no sprints.";
        } else {
            sprintsInfo = String.format("Sprints:\n%s", sprintFormatter.formatMany(project.sprints, FORMAT_MINIMUM, 0));
        }

        return StringUtils.indent(
                String.format("""
              [Project code: %s]
                Name:       %s
                Start date: %s
                End date:   %s
                %s""", project.getCode(), project.name, defaultDateFormatter.format(project.startDate),
                defaultDateFormatter.format(project.endDate), sprintsInfo),
                indent);
    }

    @Override
    public String formatMinimum(Project project, int indent) {
        return StringUtils.indent(String.format("[Project code: %s] %s\n  Start date: %s\n  End date:   %s", project.getCode(),
                project.name, defaultDateFormatter.format(project.startDate),
                defaultDateFormatter.format(project.endDate)), indent);
    }
}
