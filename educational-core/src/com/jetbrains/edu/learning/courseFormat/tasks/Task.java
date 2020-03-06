package com.jetbrains.edu.learning.courseFormat.tasks;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.xmlb.XmlSerializer;
import com.intellij.util.xmlb.annotations.MapAnnotation;
import com.intellij.util.xmlb.annotations.OptionTag;
import com.intellij.util.xmlb.annotations.Transient;
import com.jetbrains.edu.coursecreator.stepik.StepikChangeRetriever;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.actions.CheckAction;
import com.jetbrains.edu.learning.checker.TaskCheckerProvider;
import com.jetbrains.edu.learning.courseFormat.*;
import com.jetbrains.edu.learning.courseFormat.ext.TaskExt;
import com.jetbrains.edu.learning.stepik.StepikTaskBuilder;
import com.jetbrains.edu.learning.stepik.api.StepikJacksonDeserializersKt;
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse;
import com.jetbrains.edu.learning.yaml.YamlDeserializer;
import icons.EducationalCoreIcons;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;
import java.util.*;

/**
 * Implementation of task which contains task files, tests, input file for tests
 *
 * Update {@link StepikChangeRetriever#taskFilesChanged and StepikChangeRetriever#taskInfoChanged} if you added new property that has to be compared
 *
 * To implement new task there are 6 steps to be done:
 * - Extend {@link Task} class
 * - Go to {@link ItemContainer#items} and update elementTypes in AbstractCollection annotation. Needed for proper xml serialization
 * - Update {@link StepikJacksonDeserializersKt#doDeserializeTask} to handle json serialization
 * - Update {@link TaskCheckerProvider#getTaskChecker} and provide default checker for new task
 * - Update {@link StepikTaskBuilder#pluginTaskTypes} for the tasks we do not have separately on stepik and {@link StepikTaskBuilder#stepikTaskTypes} otherwise
 * - Handle yaml deserialization:
 *    - add type in {@link YamlDeserializer#deserializeTask(com.fasterxml.jackson.databind.ObjectMapper, String)}
 *    - add yaml mixins for course creator and student fields {@link com.jetbrains.edu.learning.yaml.format}
 */
public abstract class Task extends StudyItem {
  private static final Logger LOG = Logger.getInstance(Task.class);
  protected CheckStatus myStatus = CheckStatus.Unchecked;
  private Map<String, TaskFile> myTaskFiles = new LinkedHashMap<>();
  @NotNull
  private String descriptionText = "";
  private DescriptionFormat descriptionFormat = EduUtils.getDefaultTaskDescriptionFormat();
  @NotNull
  private FeedbackLink myFeedbackLink = new FeedbackLink();
  @Nullable
  private Boolean solutionHidden;
  private int myRecord = -1;
  @Transient private Lesson myLesson;

  public Task() {} //use only for deserialization

  public Task(@NotNull final String name) {
    super(name);
  }

  public Task(@NotNull final String name, int id, int position, @NotNull Date updateDate, @NotNull CheckStatus status) {
    super(name);
    myId = id;
    setIndex(position);
    setUpdateDate(updateDate);
    myStatus = status;
  }

  public void init(@Nullable Course course, @Nullable final StudyItem parentItem, boolean isRestarted) {
    setLesson(parentItem instanceof Lesson ? (Lesson)parentItem : null);
    for (TaskFile taskFile : getTaskFileValues()) {
      taskFile.initTaskFile(this, isRestarted);
    }
  }

  @MapAnnotation(sortBeforeSave = false)
  @OptionTag("files")
  public Map<String, TaskFile> getTaskFiles() {
    return myTaskFiles;
  }

  // Use carefully. taskFiles is supposed to be ordered so use LinkedHashMap
  @OptionTag("files")
  public void setTaskFiles(Map<String, TaskFile> taskFiles) {
    this.myTaskFiles = taskFiles;
  }

  @NotNull
  public String getDescriptionText() {
    return descriptionText;
  }

  public void setDescriptionText(@NotNull String descriptionText) {
    this.descriptionText = descriptionText;
  }

  public DescriptionFormat getDescriptionFormat() {
    return descriptionFormat;
  }

  public void setDescriptionFormat(DescriptionFormat descriptionFormat) {
    this.descriptionFormat = descriptionFormat;
  }

  @Nullable
  public TaskFile getTaskFile(final String name) {
    return name != null ? myTaskFiles.get(name) : null;
  }

  public TaskFile addTaskFile(@NotNull final String name, boolean isVisible) {
    TaskFile taskFile = new TaskFile();
    taskFile.setTask(this);
    taskFile.setName(name);
    taskFile.setVisible(isVisible);
    myTaskFiles.put(name, taskFile);
    return taskFile;
  }

  public TaskFile addTaskFile(@NotNull final String name) {
    return addTaskFile(name, true);
  }

  public void addTaskFile(@NotNull final TaskFile taskFile) {
    taskFile.setTask(this);
    myTaskFiles.put(taskFile.getName(), taskFile);
  }

  public void addTaskFile(@NotNull final TaskFile taskFile, int position) {
    taskFile.setTask(this);
    if (position < 0 || position > myTaskFiles.size()) {
      throw new IndexOutOfBoundsException();
    }
    Map<String, TaskFile> newTaskFileMap = new LinkedHashMap<>(myTaskFiles.size() + 1);
    int currentIndex = 0;
    for (Map.Entry<String, TaskFile> entry : myTaskFiles.entrySet()) {
      if (currentIndex == position) {
        newTaskFileMap.put(taskFile.getName(), taskFile);
      }
      newTaskFileMap.put(entry.getKey(), entry.getValue());
      currentIndex++;
    }
    if (currentIndex == position) {
      newTaskFileMap.put(taskFile.getName(), taskFile);
    }
    myTaskFiles = newTaskFileMap;
  }

  @Nullable
  public TaskFile getFile(@NotNull final String fileName) {
    return myTaskFiles.get(fileName);
  }

  @Transient
  public Lesson getLesson() {
    return myLesson;
  }

  @Transient
  public void setLesson(Lesson lesson) {
    myLesson = lesson;
  }

  @Nullable
  public VirtualFile getTaskDir(@NotNull final Project project) {
    if (myLesson == null) {
      LOG.warn("Lesson is null for task " + getName() + " (id " + getId() + ")");
      return null;
    }
    final VirtualFile lessonDir = myLesson.getLessonDir(project);

    return lessonDir == null ? null : lessonDir.findChild(TaskExt.getDirName(this));
  }

  public String getTaskDescription() {
    return descriptionText;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Task task = (Task)o;

    if (getIndex() != task.getIndex()) return false;
    if (!Objects.equals(getName(), task.getName())) return false;
    if (!Objects.equals(myTaskFiles, task.myTaskFiles)) return false;
    if (!descriptionText.equals(task.descriptionText)) return false;
    if (!Objects.equals(descriptionFormat, task.descriptionFormat)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = getName() != null ? getName().hashCode() : 0;
    result = 31 * result + getIndex();
    result = 31 * result + (myTaskFiles != null ? myTaskFiles.hashCode() : 0);
    result = 31 * result + descriptionText.hashCode();
    result = 31 * result + (descriptionFormat != null ? descriptionFormat.hashCode() : 0);
    return result;
  }

  public CheckStatus getStatus() {
    return myStatus;
  }

  public void setStatus(CheckStatus status) {
    for (TaskFile taskFile : myTaskFiles.values()) {
      for (AnswerPlaceholder placeholder : taskFile.getAnswerPlaceholders()) {
        placeholder.setStatus(status);
      }
    }
    myStatus = status;
  }

  public Task copy() {
    Element element = XmlSerializer.serialize(this);
    Task copy = XmlSerializer.deserialize(element, getClass());
    copy.init(null, null, true);
    return copy;
  }

  public int getPosition() {
    final Lesson lesson = getLesson();
    return lesson.getTaskList().indexOf(this) + 1;
  }

  public boolean isValid(@NotNull Project project) {
    VirtualFile taskDir = getTaskDir(project);
    if (taskDir == null) return false;
    for (TaskFile taskFile : getTaskFileValues()) {
      VirtualFile file = EduUtils.findTaskFileInDir(taskFile, taskDir);
      if (file == null) continue;
      try {
        String text = VfsUtilCore.loadText(file);
        if (!taskFile.isValid(text)) return false;
      }
      catch (IOException e) {
        return false;
      }
    }
    return true;
  }

  public boolean isToSubmitToStepik() {
    return false;
  }

  public Icon getIcon() {
    if (myStatus == CheckStatus.Unchecked) {
      return EducationalCoreIcons.Task;
    }
    return myStatus == CheckStatus.Solved ? EducationalCoreIcons.TaskSolved : EducationalCoreIcons.TaskFailed;
  }

  @NotNull
  public FeedbackLink getFeedbackLink() {
    return myFeedbackLink;
  }

  public void setFeedbackLink(@NotNull FeedbackLink feedbackLink) {
    myFeedbackLink = feedbackLink;
  }

  @Override
  @Nullable
  public VirtualFile getDir(@NotNull Project project) {
    return getTaskDir(project);
  }

  @NotNull
  @Override
  public Course getCourse() {
    return myLesson.getCourse();
  }

  @NotNull
  private Collection<TaskFile> getTaskFileValues() {
    return getTaskFiles().values();
  }

  @SuppressWarnings("unused") //used for yaml deserialization
  private void setTaskFileValues(List<TaskFile> taskFiles) {
    this.myTaskFiles.clear();
    for (TaskFile taskFile : taskFiles) {
      this.myTaskFiles.put(taskFile.getName(), taskFile);
    }
  }

  @NotNull
  @Override
  public StudyItem getParent() {
    return myLesson;
  }

  public int getRecord() {
    return myRecord;
  }

  public void setRecord(int record) {
    myRecord = record;
  }

  public String getUIName() {
    if (getCourse() instanceof HyperskillCourse) {
      return "Stage";
    }
    return "Task";
  }

  public boolean supportSubmissions() {
    return false;
  }

  /**
   * @return null means that behaviour for this particular Task hasn't been configured by a user
   * and {@link Course#getSolutionsHidden()} should be used instead
   */
  @Nullable
  public Boolean getSolutionHidden() {
    return solutionHidden;
  }

  public void setSolutionHidden(@Nullable Boolean solutionHidden) {
    this.solutionHidden = solutionHidden;
  }

  public boolean isPluginTaskType() {
    return true;
  }

  @NotNull
  public CheckAction getCheckAction() {
    return getCourse().getCheckAction();
  }
}
