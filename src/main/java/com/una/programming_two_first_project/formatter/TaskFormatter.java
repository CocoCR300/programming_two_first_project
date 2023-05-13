package com.una.programming_two_first_project.formatter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.una.programming_two_first_project.model.Task;

@Singleton
public class TaskFormatter extends BaseFormatter<Task>
{
    @Inject private SprintFormatter sprintFormatter;

    @Override
    protected String getModelNameLowercase() {
        return "task";
    }

    @Override
    public String formatFull(Task task, int indent) {
        String sprintInfo;

        if (task.sprint != null) {
            sprintInfo = String.format("Sprint: %s", sprintFormatter.formatMinimum(task.sprint, 0));
        } else {
            sprintInfo = "This task is not in any sprint.";
        }

        return indent(
                String.format("""
                      [Task ID: %s]
                        Name:               %s
                        Description:        %s
                        Needed resources:   %s
                        %s""",
                        task.id, task.name, task.description, task.neededResources, sprintInfo),
                indent);
    }

    @Override
    public String formatMinimum(Task task, int indent) {
        String sprintInfo;

        if (task.sprint != null) {
            sprintInfo = String.format("Sprint: %s", sprintFormatter.formatMinimum(task.sprint, 0));
        } else {
            sprintInfo = "This task is not in any sprint.";
        }

        return indent(String.format("[Task ID: %s] %s\n  %s", task.id, task.name, sprintInfo), indent);
    }
}
