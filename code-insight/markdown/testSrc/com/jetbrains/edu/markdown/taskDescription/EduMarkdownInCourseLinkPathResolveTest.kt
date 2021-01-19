package com.jetbrains.edu.markdown.taskDescription

import com.jetbrains.edu.codeInsight.taskDescription.EduInCourseLinkPathResolveTestBase
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat

class EduMarkdownInCourseLinkPathResolveTest : EduInCourseLinkPathResolveTestBase() {
  override val taskDescriptionFormat: DescriptionFormat get() = DescriptionFormat.MD
}
