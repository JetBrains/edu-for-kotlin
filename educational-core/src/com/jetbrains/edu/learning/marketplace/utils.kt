package com.jetbrains.edu.learning.marketplace

import com.jetbrains.edu.learning.computeUnderProgress
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.FeedbackLink
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer

private const val DATA_DELIMITER = ";"
private const val DELIMITER = "."

fun decodeHubToken(token: String): String? {
  val parts = token.split(DATA_DELIMITER)
  if (parts.size != 2) {
    error("Hub oauth token data part is malformed")
  }
  val userData = parts[0].split(DELIMITER)
  if (userData.size != 4) {
    error("Hub oauth token data part is malformed")
  }
  return if (userData[2].isEmpty()) null else userData[2]
}

fun Course.updateCourseItems() {
  visitSections { section -> section.generateId() }
  visitLessons { lesson ->
    lesson.visitTasks { task ->
      task.generateId()
      task.feedbackLink.type = FeedbackLink.LinkType.MARKETPLACE
    }
    lesson.generateId()
  }
  YamlFormatSynchronizer.saveRemoteInfo(this)
}

fun Course.setRemoteMarketplaceCourseVersion() {
  val updateInfo = MarketplaceConnector.getInstance().getLatestCourseUpdateInfo(id)
  if (updateInfo != null) {
    incrementMarketplaceCourseVersion(updateInfo.version)
  }
}

fun Course.convertToMarketplace() {
  isMarketplace = true
  if (marketplaceCourseVersion == 0) {
    marketplaceCourseVersion = 1
  }
}

fun Course.loadMarketplaceCourseStructure() {
  if (this is EduCourse && isMarketplace && items.isEmpty()) {
    computeUnderProgress(title = EduCoreBundle.message("progress.loading.course")) {
      MarketplaceConnector.getInstance().loadCourseStructure(this)
    }
  }
}