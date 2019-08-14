package com.jetbrains.edu.coursecreator.actions.stepik.hyperskill

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.Experiments
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.io.FileUtilRt
import com.jetbrains.edu.coursecreator.actions.CCPluginToggleAction
import com.jetbrains.edu.coursecreator.ui.CCNewCourseDialog
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import com.jetbrains.edu.learning.stepik.api.StepikCourseLoader
import com.jetbrains.edu.learning.stepik.api.loadAttachment
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import icons.EducationalCoreIcons

@Suppress("ComponentNotRegistered") // Hyperskill.xml
class GetHyperskillLesson : DumbAwareAction("Get Hyperskill Lesson from Stepik", "Get Hyperskill Lesson from Stepik",
                                            EducationalCoreIcons.Hyperskill) {

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = CCPluginToggleAction.isCourseCreatorFeaturesEnabled
                                         && Experiments.isFeatureEnabled(EduExperimentalFeatures.HYPERSKILL)
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.getData(CommonDataKeys.PROJECT)
    val lessonId = Messages.showInputDialog("Please, enter lesson id", "Get Hyperskill Lesson from Stepik", EducationalCoreIcons.Hyperskill)
    if (lessonId != null && lessonId.isNotEmpty()) {
      ProgressManager.getInstance().run(object : Task.Modal(project, "Loading Course", true) {
        override fun run(indicator: ProgressIndicator) {
          createCourse(lessonId)
        }
      })
    }
  }

  private fun createCourse(lessonId: String) {
    val course = HyperskillCourse()
    val lesson = StepikConnector.getInstance().getLesson(Integer.valueOf(lessonId)) ?: return
    val allStepSources = StepikConnector.getInstance().getStepSources(lesson.steps)
    val tasks = StepikCourseLoader.getTasks(course, lesson, allStepSources)
    for (task in tasks) {
      lesson.addTask(task)
    }

    course.name = "Hyperskill lesson $lessonId"
    course.description = "Hyperskill lesson $lessonId"
    course.language = getLanguage(lesson)

    val hyperskillLesson = FrameworkLesson(lesson)
    course.addItem(hyperskillLesson, 0)
    course.additionalFiles = loadAttachment(course, lesson)

    runInEdt {
      CCNewCourseDialog("Get Hyperskill Lesson from Stepik", "Create", course).show()
    }
  }

  private fun getLanguage(lesson: Lesson): String {
    for (task in lesson.taskList) {
      for (taskFile in task.taskFiles.keys) {
        val extension = FileUtilRt.getExtension(taskFile)
        if (extension == "py")
          return EduNames.PYTHON
        if (extension == "kt") {
          return EduNames.KOTLIN
        }
      }
    }
    return EduNames.JAVA
  }
}