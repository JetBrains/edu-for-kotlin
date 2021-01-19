package com.jetbrains.edu.html.taskDescription

import com.jetbrains.edu.codeInsight.taskDescription.EduInCourseLinkPathResolveTestBase
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat

class EduHtmlInCourseLinkPathResolveTest : EduInCourseLinkPathResolveTestBase() {
  override val taskDescriptionFormat: DescriptionFormat get() = DescriptionFormat.HTML
}
