package com.jetbrains.edu.learning.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.RightAlignedToolbarAction
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.learning.EduBrowser
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTask.Companion.codeforcesTaskLink
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.hyperskillTaskLink
import org.jetbrains.annotations.NonNls


@Suppress("ComponentNotRegistered")
class OpenTaskOnSiteAction : DumbAwareAction(EduCoreBundle.lazyMessage("action.open.on.site.text")), RightAlignedToolbarAction {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val task = EduUtils.getCurrentTask(project) ?: return
    val link = when (task.course) {
      is CodeforcesCourse -> codeforcesTaskLink(task)
      is HyperskillCourse -> hyperskillTaskLink(task)
      else -> error("Only Codeforces and Hyperskill are supported")
    }
    EduBrowser.getInstance().browse(link)
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false

    val project = e.project ?: return
    if (!EduUtils.isStudentProject(project)) return
    val task = EduUtils.getCurrentTask(project) ?: return
    val course = task.course

    e.presentation.isEnabledAndVisible = course is CodeforcesCourse || course is HyperskillCourse
  }

  companion object {
    @NonNls
    const val ACTION_ID: String = "Educational.OpenTaskOnSiteAction"
  }
}