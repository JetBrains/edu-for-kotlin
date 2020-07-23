package com.jetbrains.edu.learning.statistics

import com.intellij.internal.statistic.eventLog.FeatureUsageData
import com.intellij.internal.statistic.service.fus.collectors.FUCounterUsageLogger
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.StudyItem

/**
 * IMPORTANT: if you modify anything in this class, updated whitelist rules should be
 * provided to analytics platform team.
 * See `docs/statisticsRules.md` for more information
 */
object EduCounterUsageCollector {
  enum class TaskNavigationPlace {
    CHECK_ALL_NOTIFICATION,
    TASK_DESCRIPTION_TOOLBAR,
    CHECK_PANEL,
    UNRESOLVED_DEPENDENCY_NOTIFICATION
  }

  @JvmStatic
  fun taskNavigation(place: TaskNavigationPlace) =
    reportEvent("navigate.to.task", mapOf(SOURCE to place.toLower()))

  @JvmStatic
  fun eduProjectCreated(course: Course) = reportEvent("edu.project.created", mapOf(MODE to course.courseMode, TYPE to course.itemType,
                                                                                   LANGUAGE to course.languageID))

  @JvmStatic
  fun eduProjectOpened(course: Course) = reportEvent("edu.project.opened", mapOf(MODE to course.courseMode, TYPE to course.itemType))

  @JvmStatic
  fun studyItemCreated(item: StudyItem) =
    reportEvent("study.item.created", mapOf(MODE to item.course.courseMode, TYPE to item.itemType))

  enum class LinkType {
    IN_COURSE, STEPIK, EXTERNAL, PSI, CODEFORCES
  }

  @JvmStatic
  fun linkClicked(linkType: LinkType) = reportEvent("link.clicked", mapOf(TYPE to linkType.toLower()))

  private enum class AuthorizationEvent {
    LOG_IN, LOG_OUT
  }

  enum class AuthorizationPlace {
    SETTINGS, WIDGET, START_COURSE_DIALOG, SUBMISSIONS_TAB
  }

  private fun authorization(event: AuthorizationEvent, platform: String, place: AuthorizationPlace) =
    reportEvent("authorization", mapOf(EVENT to event.toLower(), "platform" to platform, SOURCE to place.toLower()))

  @JvmStatic
  fun loggedIn(platform: String, place: AuthorizationPlace) =
    authorization(AuthorizationEvent.LOG_IN, platform, place)

  @JvmStatic
  fun loggedOut(platform: String, place: AuthorizationPlace) =
    authorization(AuthorizationEvent.LOG_OUT, platform, place)

  @JvmStatic
  fun fullOutputShown() = reportEvent("show.full.output")

  @JvmStatic
  fun solutionPeeked() = reportEvent("peek.solution")

  @JvmStatic
  fun leaveFeedback() = reportEvent("leave.feedback")

  @JvmStatic
  fun revertTask() = reportEvent("revert.task")

  @JvmStatic
  fun reviewStageTopics() = reportEvent("review.stage.topics")

  @JvmStatic
  fun checkTask(status: CheckStatus) = reportEvent("check.task", mapOf("status" to status.toLower()))

  private enum class HintEvent {
    EXPANDED, COLLAPSED
  }

  private fun hintClicked(event: HintEvent) = reportEvent("hint", mapOf(EVENT to event.toLower()))

  @JvmStatic
  fun hintExpanded() = hintClicked(HintEvent.EXPANDED)

  @JvmStatic
  fun hintCollapsed() = hintClicked(HintEvent.COLLAPSED)

  fun createCoursePreview() = reportEvent("create.course.preview")

  @JvmStatic
  fun previewTaskFile() = reportEvent("preview.task.file")

  @JvmStatic
  fun createCourseArchive() = reportEvent("create.course.archive")

  private enum class PostCourseEvent {
    UPLOAD, UPDATE
  }

  private fun postCourse(event: PostCourseEvent) = reportEvent("post.course", mapOf(EVENT to event.toLower()))

  @JvmStatic
  fun updateCourse() = postCourse(PostCourseEvent.UPDATE)

  @JvmStatic
  fun uploadCourse() = postCourse(PostCourseEvent.UPLOAD)

  enum class SynchronizeCoursePlace {
    WIDGET, PROJECT_GENERATION, PROJECT_REOPEN
  }

  @JvmStatic
  fun synchronizeCourse(place: SynchronizeCoursePlace) = reportEvent("synchronize.course", mapOf(SOURCE to place.toLower()))

  @JvmStatic
  fun importCourseArchive() = reportEvent("import.course")

  @JvmStatic
  fun codeforcesSubmitSolution() = reportEvent("codeforces.submit.solution")

  @JvmStatic
  fun twitterDialogShown(course: Course) = reportEvent(
    "twitter.dialog.shown",
    mapOf(TYPE to course.itemType, LANGUAGE to course.languageID)
  )

  @JvmStatic
  fun twitterAchievementPosted(course: Course) = reportEvent(
    "twitter.achievement.posted",
    mapOf(TYPE to course.itemType, LANGUAGE to course.languageID)
  )

  @Suppress("UnstableApiUsage")
  private fun reportEvent(eventId: String, additionalData: Map<String, String> = emptyMap()) {
    val data = FeatureUsageData()
    additionalData.forEach {
      data.addData(it.key, it.value)
    }
    FUCounterUsageLogger.getInstance().logEvent(GROUP_ID, eventId, data)
  }

  private fun Enum<*>.toLower() = this.toString().toLowerCase()

  const val GROUP_ID = "educational.counters"
  private const val MODE = "mode"
  private const val SOURCE = "source"
  private const val EVENT = "event"
  private const val TYPE = "type"
  private const val LANGUAGE = "language"
}
