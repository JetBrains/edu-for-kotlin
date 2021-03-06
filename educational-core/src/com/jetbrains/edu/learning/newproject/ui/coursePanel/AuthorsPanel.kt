package com.jetbrains.edu.learning.newproject.ui.coursePanel

import com.intellij.ide.plugins.newui.HorizontalLayout
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.courseFormat.Course
import java.awt.BorderLayout
import java.awt.Color


private const val INFO_PANEL_TOP_OFFSET = 7
private const val HORIZONTAL_OFFSET = 10

private val GRAY_TEXT_FOREGROUND: Color = JBColor.namedColor("Plugins.tagForeground", JBColor(0x787878, 0x999999))

class AuthorsPanel : NonOpaquePanel(), CourseSelectionListener {
  private var authorsLabel: JBLabel = JBLabel()

  private val Course.allAuthors: String
    get() = course.authorFullNames.joinToString()

  init {
    border = JBUI.Borders.empty(INFO_PANEL_TOP_OFFSET, HORIZONTAL_MARGIN, 0, 0)
    layout = HorizontalLayout(HORIZONTAL_OFFSET)

    authorsLabel.foreground = GRAY_TEXT_FOREGROUND
    add(authorsLabel, BorderLayout.PAGE_START)
  }

  override fun onCourseSelectionChanged(courseInfo: CourseInfo, courseDisplaySettings: CourseDisplaySettings) {
    val course = courseInfo.course
    authorsLabel.isVisible = courseDisplaySettings.showInstructorField && course.allAuthors.isNotEmpty()
    if (authorsLabel.isVisible) {
      authorsLabel.text = "by ${course.allAuthors}"
    }
  }
}