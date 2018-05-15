package com.jetbrains.edu.coursecreator.actions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Function;
import com.jetbrains.edu.learning.EduConfigurator;
import com.jetbrains.edu.learning.EduConfiguratorManager;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.StudyItem;
import com.jetbrains.edu.learning.courseFormat.ext.TaskExt;
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import icons.EducationalCoreIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;

public class CCCreateTask extends CCCreateStudyItemActionBase<Task> {

  public CCCreateTask() {
    super(EduNames.TASK, EducationalCoreIcons.Task);
  }

  @Override
  protected void addItem(@NotNull Course course, @NotNull Task item) {
    item.getLesson().addTask(item);
  }

  @Override
  protected Function<VirtualFile, ? extends StudyItem> getStudyOrderable(@NotNull final StudyItem item,
                                                                         @NotNull Course course) {
    return (Function<VirtualFile, StudyItem>)file -> {
      if (item instanceof Task) {
        return ((Task)item).getLesson().getTask(file.getName());
      }
      return null;
    };
  }

  @Override
  @Nullable
  protected VirtualFile createItemDir(@NotNull final Project project, @NotNull final Task item,
                                      @NotNull final VirtualFile parentDirectory, @NotNull final Course course) {
    EduConfigurator configurator = EduConfiguratorManager.forLanguage(course.getLanguageById());
    if (configurator != null) {
      return configurator.getCourseBuilder().createTaskContent(project, item, parentDirectory, course);
    }
    return null;
  }

  @Override
  protected int getSiblingsSize(@NotNull Course course, @Nullable StudyItem parentItem) {
    if (parentItem instanceof Lesson) {
      return ((Lesson)parentItem).getTaskList().size();
    }
    return 0;
  }

  @Nullable
  @Override
  protected StudyItem getParentItem(@NotNull Course course, @NotNull VirtualFile directory) {
    Task task = EduUtils.getTask(directory, course);
    if (task == null) {
      return EduUtils.getLesson(directory, course);
    }
    return task.getLesson();
  }

  @Nullable
  @Override
  protected StudyItem getThresholdItem(@NotNull Course course, @NotNull VirtualFile sourceDirectory) {
    return EduUtils.getTask(sourceDirectory, course);
  }

  @Override
  protected boolean isAddedAsLast(@NotNull VirtualFile sourceDirectory,
                                  @NotNull Project project,
                                  @NotNull Course course) {
    return EduUtils.getLesson(sourceDirectory, course) != null;
  }

  @Override
  protected void sortSiblings(@NotNull Course course, @Nullable StudyItem parentItem) {
    if (parentItem instanceof Lesson) {
      Collections.sort(((Lesson)parentItem).getTaskList(), EduUtils.INDEX_COMPARATOR);
    }
  }

  @Override
  public Task createAndInitItem(@NotNull Course course, @Nullable StudyItem parentItem, @NotNull String name, int index) {
    final Task task = new EduTask(name);
    task.setIndex(index);
    if (parentItem == null) {
      return null;
    }
    task.setLesson(((Lesson)parentItem));
    TaskExt.addDefaultTaskDescription(task);
    return task;
  }
}