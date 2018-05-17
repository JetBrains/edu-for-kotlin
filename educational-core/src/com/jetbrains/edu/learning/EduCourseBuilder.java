package com.jetbrains.edu.learning;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils;
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * The main interface provides courses creation for some language.
 *
 * @param <Settings> container type holds course project settings state
 */
public interface EduCourseBuilder<Settings> {

  Logger LOG = Logger.getInstance(EduCourseBuilder.class);

  /**
   * Creates content (including its directory or module) of new lesson in project
   *
   * @param project Parameter is used in Java and Kotlin plugins
   * @param lesson  Lesson to create content for. It's already properly initialized and added to course.
   * @return VirtualFile of created lesson
   */
  default VirtualFile createLessonContent(@NotNull Project project,
                                          @NotNull Lesson lesson,
                                          @NotNull VirtualFile parentDirectory) {
    final VirtualFile[] lessonDirectory = new VirtualFile[1];
    ApplicationManager.getApplication().runWriteAction(() -> {
      try {
        lessonDirectory[0] = VfsUtil.createDirectoryIfMissing(parentDirectory, lesson.getName());
      } catch (IOException e) {
        LOG.error("Failed to create lesson directory", e);
      }
    });
    return lessonDirectory[0];
  }

  /**
   * Creates content (including its directory or module) of new task in project
   *
   * @param task Task to create content for. It's already properly initialized and added to corresponding lesson.
   * @return VirtualFile of created task
   */
  @Nullable
  default VirtualFile createTaskContent(@NotNull final Project project,
                                        @NotNull final Task task,
                                        @NotNull final VirtualFile parentDirectory,
                                        @NotNull final Course course) {
    if (!course.isStudy()) {
      initNewTask(task);
    }
    try {
      GeneratorUtils.createTask(task, parentDirectory);
    } catch (IOException e) {
      LOG.error("Failed to create task", e);
    }
    final VirtualFile taskDir = parentDirectory.findChild(task.getName());
    refreshProject(project);
    return taskDir;
  }

  /**
   * Allows to update project modules and the whole project structure
   */
  default void refreshProject(@NotNull final Project project) {}

  /**
   * Add initial content for a new task: task and tests files if the corresponding files don't exist.
   * Supposed to use in course creator mode
   *
   * @param task initializing task
   */
  default void initNewTask(@NotNull final Task task) {
    if (task.taskFiles.isEmpty()) {
      TaskFile taskFile = new TaskFile();
      taskFile.setTask(task);
      String taskTemplateName = getTaskTemplateName();
      if (taskTemplateName != null) {
        taskFile.name = taskTemplateName;
        taskFile.text = EduUtils.getTextFromInternalTemplate(taskTemplateName);
      } else {
        GeneratorUtils.DefaultFileProperties taskFileProperties =
          GeneratorUtils.createDefaultFile(task.getLesson().getCourse(), "Task", "type task text here");
        taskFile.name = taskFileProperties.getName();
        taskFile.text = taskFileProperties.getText();
      }
      task.addTaskFile(taskFile);
    }

    if (task.getTestsText().isEmpty()) {
      String testTemplateName = getTestTemplateName();
      if (testTemplateName != null) {
        task.getTestsText().put(testTemplateName, EduUtils.getTextFromInternalTemplate(testTemplateName));
      }
    }
  }

  @Nullable
  default String getTaskTemplateName() {
    return null;
  }

  @Nullable
  default String getTestTemplateName() {
    return null;
  }

  /**
   * @return object responsible for language settings
   * @see LanguageSettings
   */
  @NotNull
  LanguageSettings<Settings> getLanguageSettings();

  @Nullable
  default CourseProjectGenerator<Settings> getCourseProjectGenerator(@NotNull Course course) {
    return null;
  }

  /**
   * Main interface responsible for course project language settings such as JDK or interpreter
   *
   * @param <Settings> container type holds project settings state
   */
  interface LanguageSettings<Settings> {

    /**
     * Returns list of UI components that allows user to select course project settings such as project JDK or interpreter.
     *
     * @param course course of creating project
     * @return list of UI components with project settings
     */
    @NotNull
    default List<LabeledComponent<JComponent>> getLanguageSettingsComponents(@NotNull Course course) {
      return Collections.emptyList();
    }

    /**
     * Returns project settings associated with state of language settings UI component.
     * It should be passed into project generator to set chosen settings in course project.
     *
     * @return project settings object
     */
    @NotNull
    Settings getSettings();
  }
}
