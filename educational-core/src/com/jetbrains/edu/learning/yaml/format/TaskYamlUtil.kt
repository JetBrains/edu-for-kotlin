@file:JvmName("TaskYamlUtil")

package com.jetbrains.edu.learning.yaml.format

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.util.StdConverter
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.PlaceholderPainter
import com.jetbrains.edu.learning.courseFormat.FeedbackLink
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.getTaskFile
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
import com.jetbrains.edu.learning.yaml.YamlLoader.addItemAsNew
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.CUSTOM_NAME
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.FEEDBACK_LINK
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.FILES
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.SOLUTION_HIDDEN
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TYPE

/**
 * Mixin class is used to deserialize [Task] item.
 * Update [TaskChangeApplier] if new fields added to mixin
 */
@Suppress("UNUSED_PARAMETER", "unused") // used for yaml serialization
@JsonPropertyOrder(TYPE, CUSTOM_NAME, FILES, FEEDBACK_LINK, SOLUTION_HIDDEN)
abstract class TaskYamlMixin {
  @JsonProperty(TYPE)
  private fun getItemType(): String {
    throw NotImplementedInMixin()
  }

  @JsonProperty(FILES)
  open fun getTaskFileValues(): Collection<TaskFile> {
    throw NotImplementedInMixin()
  }

  @JsonProperty(FILES)
  open fun setTaskFileValues(taskFiles: List<TaskFile>) {
    throw NotImplementedInMixin()
  }

  @JsonSerialize(converter = FeedbackLinkToStringConverter::class)
  @JsonDeserialize(converter = StringToFeedbackLinkConverter::class)
  @JsonProperty(value = FEEDBACK_LINK, access = JsonProperty.Access.READ_WRITE)
  protected open lateinit var myFeedbackLink: FeedbackLink

  @JsonProperty(CUSTOM_NAME)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private var myCustomPresentableName: String? = null

  @JsonProperty(SOLUTION_HIDDEN)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private var solutionHidden: Boolean? = null
}

private class FeedbackLinkToStringConverter : StdConverter<FeedbackLink?, String>() {
  override fun convert(value: FeedbackLink?): String? {
    if (value?.link.isNullOrBlank()) {
      return ""
    }

    return value?.link
  }
}

private class StringToFeedbackLinkConverter : StdConverter<String?, FeedbackLink>() {
  override fun convert(value: String?): FeedbackLink {
    if (value == null || value.isBlank()) {
      return FeedbackLink()
    }

    return FeedbackLink(value)
  }
}

open class TaskChangeApplier(val project: Project) : StudyItemChangeApplier<Task>() {
  override fun applyChanges(existingItem: Task, deserializedItem: Task) {
    val project = existingItem.project ?: error("Project not found for ${existingItem.name}")
    if (existingItem.itemType != deserializedItem.itemType) {
      changeType(project, existingItem, deserializedItem)
      return
    }
    existingItem.feedbackLink = deserializedItem.feedbackLink
    @Suppress("DEPRECATION") // it's ok as we just copy value of deprecated field
    existingItem.customPresentableName = deserializedItem.customPresentableName
    existingItem.solutionHidden = deserializedItem.solutionHidden
    if (deserializedItem is TheoryTask && existingItem is TheoryTask) {
      existingItem.postSubmissionOnOpen = deserializedItem.postSubmissionOnOpen
    }
    if (deserializedItem is ChoiceTask && existingItem is ChoiceTask) {
      existingItem.isMultipleChoice = deserializedItem.isMultipleChoice
      existingItem.choiceOptions = deserializedItem.choiceOptions
      existingItem.messageCorrect = deserializedItem.messageCorrect
      existingItem.messageIncorrect = deserializedItem.messageIncorrect
      TaskDescriptionView.getInstance(project).updateTaskDescription()
    }
    hideOldPlaceholdersForOpenedFiles(project, existingItem)
    existingItem.applyTaskFileChanges(deserializedItem)
    paintPlaceholdersForOpenedFiles(project, existingItem)
  }

  open fun changeType(project: Project, existingItem: StudyItem, deserializedItem: Task) {
    val existingTask = existingItem as Task
    hideOldPlaceholdersForOpenedFiles(project, existingTask)

    deserializedItem.name = existingItem.name
    deserializedItem.index = existingItem.index

    val parentItem = existingTask.lesson
    parentItem.removeItem(existingItem)
    parentItem.addItemAsNew(project, deserializedItem)
  }

  private fun Task.applyTaskFileChanges(deserializedItem: Task) {
    val orderedTaskFiles = LinkedHashMap<String, TaskFile>()
    for ((name, deserializedTaskFile) in deserializedItem.taskFiles) {
      val existingTaskFile = taskFiles[name]
      val taskFile: TaskFile = if (existingTaskFile != null) {
        applyTaskFileChanges(existingTaskFile, deserializedTaskFile)
        existingTaskFile
      }
      else {
        deserializedTaskFile
      }
      orderedTaskFiles[name] = taskFile
      deserializedTaskFile.initTaskFile(this, false)
    }
    taskFiles = orderedTaskFiles
  }

  protected open fun applyTaskFileChanges(existingTaskFile: TaskFile,
                                   deserializedTaskFile: TaskFile) {
    existingTaskFile.applyPlaceholderChanges(deserializedTaskFile)
    existingTaskFile.isVisible = deserializedTaskFile.isVisible
  }

  private fun TaskFile.applyPlaceholderChanges(deserializedTaskFile: TaskFile) {
    PlaceholderPainter.hidePlaceholders(this)
    answerPlaceholders = deserializedTaskFile.answerPlaceholders
  }

  private fun paintPlaceholdersForOpenedFiles(project: Project, task: Task) {
    getOpenedTaskFiles(project, task).forEach { PlaceholderPainter.showPlaceholders(project, it) }
  }

  private fun hideOldPlaceholdersForOpenedFiles(project: Project, task: Task) {
    getOpenedTaskFiles(project, task).forEach { PlaceholderPainter.hidePlaceholders(it) }
  }

  private fun getOpenedTaskFiles(project: Project, task: Task): List<TaskFile> {
    return FileEditorManager.getInstance(project).openFiles.mapNotNull { it.getTaskFile(project) }.filter { it.task == task }
  }
}