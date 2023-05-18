package com.una.programming_two_first_project.formatter;

import com.google.inject.Inject;
import com.una.programming_two_first_project.model.Sprint;
import com.una.programming_two_first_project.util.StringUtils;

public class SprintFormatter extends BaseFormatter<Sprint>
{
    @Inject private ProjectFormatter projectFormatter;
    @Inject private TaskFormatter taskFormatter;

    @Override
    protected String getModelNameLowercase() {
        return "sprint";
    }

    @Override
    public String formatFull(Sprint sprint, int indent) {
        String projectInfo;
        String tasksInfo;

        if (sprint.project != null) {
            projectInfo = String.format("Project: %s", projectFormatter.formatMinimum(sprint.project, 0));
        } else {
            projectInfo = "This sprint is not in any project.";
        }

        if (sprint.tasks.isEmpty()) {
            tasksInfo = "This sprint has no tasks.";
        } else {
            tasksInfo = String.format("Tasks:\n%s", taskFormatter.formatMany(sprint.tasks, FORMAT_MINIMUM, 4));
        }

        return StringUtils.indent(
                String.format("""
                      [Sprint code: %s | ID: %s]
                        Number:        %s
                        Start date:    %s
                        End date:      %s
                        %s
                        %s""",
                        sprint.getCode(), sprint.getId(), sprint.number, defaultDateFormatter.format(sprint.startDate),
                        defaultDateFormatter.format(sprint.endDate),
                        projectInfo, tasksInfo),
                indent);
    }

    @Override
    public String formatMinimum(Sprint sprint, int indent) {
        return StringUtils.indent(String.format("[Sprint code: %s | ID: %s]\n  Start date: %s\n  End date:   %s",
                sprint.getCode(), sprint.getId(), defaultDateFormatter.format(sprint.startDate),
                defaultDateFormatter.format(sprint.endDate)), indent);
    }
}
