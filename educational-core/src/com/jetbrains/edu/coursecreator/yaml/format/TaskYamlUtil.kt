@file:JvmName("TaskYamlUtil")

package com.jetbrains.edu.coursecreator.yaml.format

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.util.StdConverter
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.jetbrains.edu.coursecreator.yaml.YamlLoader.itemNotFound
import com.jetbrains.edu.coursecreator.yaml.YamlLoader.taskDirNotFoundError
import com.jetbrains.edu.coursecreator.yaml.setPlaceholdersPossibleAnswer
import com.jetbrains.edu.learning.PlaceholderPainter
import com.jetbrains.edu.learning.courseFormat.FeedbackLink
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.editor.EduEditor

private const val TYPE = "type"
private const val FILES = "files"
private const val FEEDBACK_LINK = "feedback_link"

/**
 * Mixin class is used to deserialize [Task] item.
 * Update [TaskChangeApplier] if new fields added to mixin
 */
@Suppress("UNUSED_PARAMETER", "unused") // used for yaml serialization
@JsonPropertyOrder(TYPE, FILES, FEEDBACK_LINK)
abstract class TaskYamlMixin {
  @JsonProperty(TYPE)
  fun getItemType(): String {
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
  lateinit var myFeedbackLink: FeedbackLink
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

class TaskChangeApplier<T : Task> : StudyItemChangeApplier<T>() {
  override fun applyChanges(existingItem: T, deserializedItem: T) {
    val project = existingItem.project ?: error("Cannot find project for a task: ${existingItem}")
    existingItem.feedbackLink = deserializedItem.feedbackLink
    hideOldPlaceholdersForOpenedFiles(project, existingItem)
    existingItem.applyTaskFileChanges(project, deserializedItem)
    paintPlaceholdersForOpenedFiles(project, existingItem)
  }

  private fun Task.applyTaskFileChanges(project: Project, deserializedItem: Task) {
    for ((name, taskFile) in taskFiles) {
      val deserializedTaskFile = deserializedItem.taskFiles[name] ?: itemNotFound(name)
      taskFile.applyPlaceholderChanges(project, deserializedTaskFile)
      taskFile.isVisible = deserializedTaskFile.isVisible
      // init new placeholders to correctly paint them
      taskFile.initTaskFile(this, false)
    }
  }

  private fun TaskFile.applyPlaceholderChanges(project: Project, deserializedTaskFile: TaskFile) {
    PlaceholderPainter.hidePlaceholders(this)
    answerPlaceholders = deserializedTaskFile.answerPlaceholders
    setPlaceholdersPossibleAnswer(project)
  }

  private fun paintPlaceholdersForOpenedFiles(project: Project, task: Task) {
    getOpenedEduEditors(project, task).forEach { PlaceholderPainter.showPlaceholders(project, it.taskFile) }
  }

  private fun hideOldPlaceholdersForOpenedFiles(project: Project, task: Task) {
    getOpenedEduEditors(project, task).forEach { PlaceholderPainter.showPlaceholders(project, it.taskFile) }
  }

  private fun getOpenedEduEditors(project: Project, task: Task): List<EduEditor> {
    val taskDir = task.getDir(project) ?: taskDirNotFoundError(task.name)
    return FileEditorManager.getInstance(project).openFiles
      .filter { VfsUtil.isAncestor(taskDir, it, true) }
      .map { FileEditorManager.getInstance(project).getSelectedEditor(it) }
      .filterIsInstance(EduEditor::class.java)
  }
}