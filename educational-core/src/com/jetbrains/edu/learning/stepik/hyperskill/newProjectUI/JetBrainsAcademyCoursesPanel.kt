package com.jetbrains.edu.learning.stepik.hyperskill.newProjectUI

import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.JetBrainsAcademyCourse
import com.jetbrains.edu.learning.newproject.coursesStorage.CourseMetaInfo
import com.jetbrains.edu.learning.newproject.ui.*
import com.jetbrains.edu.learning.stepik.hyperskill.JBA_HELP
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings
import com.jetbrains.edu.learning.ui.EduColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Color

class JetBrainsAcademyCoursesPanel(
  private val platformProvider: JetBrainsAcademyPlatformProvider, scope: CoroutineScope
) : CoursesPanel(platformProvider, scope) {

  override fun tabInfo(): TabInfo {
    val infoText = EduCoreBundle.message("hyperskill.courses.explanation", EduNames.JBA)
    val linkText = EduCoreBundle.message("course.dialog.find.details")
    val linkInfo = LinkInfo(linkText, JBA_HELP)
    return TabInfo(infoText, linkInfo, JetBrainsAcademyLoginPanel())
  }

  override fun updateModelAfterCourseDeletedFromStorage(deletedCourse: Course) {
    if (deletedCourse is CourseMetaInfo) {
      val coursesGroup = coursesGroups.first()

      coursesGroup.courses = coursesGroup.courses.filter { it != deletedCourse }

      if (coursesGroup.courses.isEmpty()) {
        coursesGroup.courses = listOf(JetBrainsAcademyCourse())
      }
    }

    super.updateModelAfterCourseDeletedFromStorage(deletedCourse)
  }

  override fun createCoursesListPanel() = JetBrainsAcademyCoursesListPanel()

  inner class JetBrainsAcademyCoursesListPanel : CoursesListWithResetFilters() {
    override fun createCardForNewCourse(course: Course): CourseCardComponent {
      return JetBrainsAcademyCourseCard(course)
    }
  }

  private inner class JetBrainsAcademyLoginPanel : LoginPanel(isLoginNeeded(),
                                                              EduCoreBundle.message("course.dialog.jba.log.in.label.before.link"),
                                                              EduCoreBundle.message("course.dialog.log.in.to", EduNames.JBA),
                                                              { handleLogin() }) {
    override val beforeLinkForeground: Color
      get() = EduColors.warningTextForeground
  }

  override fun isLoginNeeded() = HyperskillSettings.INSTANCE.account == null

  private fun handleLogin() {
    HyperskillConnector.getInstance().doAuthorize(
      Runnable { coursePanel.hideErrorPanel() },
      Runnable { setButtonsEnabled(true) },
      Runnable { hideLoginPanel() },
      Runnable { scheduleUpdateAfterLogin() }
    )
  }

  override suspend fun updateCoursesAfterLogin(preserveSelection: Boolean) {
    val academyCoursesGroups = withContext(Dispatchers.IO) { platformProvider.loadCourses() }
    coursesGroups.clear()
    coursesGroups.addAll(academyCoursesGroups)
    super.updateCoursesAfterLogin(false)
  }
}