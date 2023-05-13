package com.una.programming_two_first_project.formatter;

import com.google.inject.Inject;
import com.una.programming_two_first_project.model.Sprint;

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

        return indent(
                String.format("""
                      [Sprint ID: %s]
                        Number:                 %s
                        Start date and time:    %s
                        End date and time:      %s
                        %s
                        %s""",
                        sprint.id, sprint.number, defaultDateTimeFormatter.format(sprint.startDateTime),
                        defaultDateTimeFormatter.format(sprint.endDateTime),
                        projectInfo, tasksInfo),
                indent);
    }

    @Override
    public String formatMinimum(Sprint sprint, int indent) {
        String projectInfo;

        if (sprint.project != null) {
            projectInfo = String.format("Project: %s", projectFormatter.formatMinimum(sprint.project, 0));
        } else {
            projectInfo = "This sprint is not in any project.";
        }

        return indent(String.format("[Sprint ID: %s]\n  Start: %s\n  End:   %s\n  %s",
                sprint.id, defaultDateTimeFormatter.format(sprint.startDateTime),
                defaultDateTimeFormatter.format(sprint.endDateTime), projectInfo), indent);
    }
}
