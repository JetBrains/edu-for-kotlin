package com.jetbrains.edu.codeInsight.taskDescription

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat

abstract class EduTaskDescriptionTestBase : EduTestCase() {
  protected abstract val taskDescriptionFormat: DescriptionFormat

  protected fun String.withDescriptionFormat(): String {
    return when (taskDescriptionFormat) {
      // language=HTML
      DescriptionFormat.HTML -> "<a href='$this'>link text</a>"
      // language=Markdown
      DescriptionFormat.MD -> "[link Text]($this)"
    }
  }
}
