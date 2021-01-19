package com.jetbrains.edu.html.taskDescription

import com.jetbrains.edu.codeInsight.taskDescription.EduUriSchemaCompletionProviderTestBase
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat

@Suppress("HtmlUnknownTarget")
class EduHtmlUriSchemaCompletionProviderTest : EduUriSchemaCompletionProviderTestBase() {
  override val taskDescriptionFormat: DescriptionFormat get() = DescriptionFormat.HTML
}
