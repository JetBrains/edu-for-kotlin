@file:JvmName("TaskExt")

package com.jetbrains.edu.learning.courseFormat.ext

import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholderDependency
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.tasks.Task

val Task.course: Course? get() = lesson?.course

val Task.project: Project? get() = course?.project

val Task.sourceDir: String? get() = course?.sourceDir
val Task.testDir: String? get() = course?.testDir

val Task.testTextMap: Map<String, String> get() {
  val course = course ?: return emptyMap()
  val testDir = course.testDir ?: return emptyMap()
  return if (testDir.isEmpty()) testsText else testsText.mapKeys { (path, _) -> "$testDir/$path" }
}

val Task.isFrameworkTask: Boolean get() = lesson is FrameworkLesson

val Task.dirName: String get() = if (isFrameworkTask) EduNames.TASK else name

fun Task.findSourceDir(taskDir: VirtualFile): VirtualFile? {
  val sourceDir = sourceDir ?: return null
  return taskDir.findFileByRelativePath(sourceDir)
}

fun Task.findTestDir(taskDir: VirtualFile): VirtualFile? {
  val testDir = testDir ?: return null
  return taskDir.findFileByRelativePath(testDir)
}

val Task.placeholderDependencies: List<AnswerPlaceholderDependency>
  get() = taskFiles.values.flatMap { it.answerPlaceholders.mapNotNull { it.placeholderDependency } }

fun Task.getUnsolvedTaskDependencies(): List<Task> {
  return placeholderDependencies
    .mapNotNull { it.resolve(course ?: return@mapNotNull null)?.taskFile?.task }
    .filter { it.status != CheckStatus.Solved }
    .distinct()
}

fun Task.hasChangedFiles(project: Project): Boolean {
  for (taskFile in taskFiles.values) {
    val document = taskFile.getDocument(project) ?: continue
    if (taskFile.text != null && document.text != taskFile.text) {
      return true
    }
  }
  return false
}

fun Task.saveStudentAnswersIfNeeded(project: Project) {
  if (lesson !is FrameworkLesson) return

  val taskDir = getTaskDir(project) ?: return
  for ((_, taskFile) in getTaskFiles()) {
    val virtualFile = EduUtils.findTaskFileInDir(taskFile, taskDir) ?: continue
    val document = FileDocumentManager.getInstance().getDocument(virtualFile) ?: continue
    for (placeholder in taskFile.answerPlaceholders) {
      val startOffset = placeholder.offset
      val endOffset = startOffset + placeholder.realLength
      placeholder.studentAnswer = document.getText(TextRange.create(startOffset, endOffset))
    }
  }
}

fun Task.addDefaultTaskDescription() {
  val fileName = EduUtils.getTaskDescriptionFileName()
  val template = FileTemplateManager.getDefaultInstance().getInternalTemplate(fileName) ?: return
  description = template.text
}

fun Task.getDescriptionFile(project: Project): VirtualFile? {
  val taskDir = getTaskDir(project) ?: return null
  return taskDir.findChild(EduNames.TASK_HTML) ?: taskDir.findChild(EduNames.TASK_MD)
}
