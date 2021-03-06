package com.jetbrains.edu.learning.codeforces.courseFormat

import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.codeforces.CodeforcesContestConnector.getContestURLFromID
import com.jetbrains.edu.learning.codeforces.CodeforcesNames
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.CODEFORCES_COURSE_TYPE
import com.jetbrains.edu.learning.codeforces.ContestParameters
import com.jetbrains.edu.learning.compatibility.CourseCompatibility
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Tag
import com.jetbrains.edu.learning.messages.EduCoreBundle
import icons.EducationalCoreIcons
import org.jetbrains.annotations.NonNls
import org.jsoup.nodes.Document
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.swing.Icon

open class CodeforcesCourse : Course {
  var endDateTime: ZonedDateTime? = null
  var startDate: ZonedDateTime? = null
  var length: Duration = Duration.ZERO
  var isRegistrationOpen: Boolean = false
  var availableLanguages: List<String> = emptyList()

  @Suppress("unused") //used for deserialization
  constructor()

  constructor(contestParameters: ContestParameters, doc: Document) {
    setContestParameters(contestParameters)

    parseResponseToAddContent(doc)
  }

  constructor(contestParameters: ContestParameters) {
    setContestParameters(contestParameters)

    val date = endDateTime?.toLocalDate()?.format(DateTimeFormatter.ofPattern("dd MMM YYYY", Locale.ENGLISH))
    if (date != null) {
      val codeforcesLink = "<a href='${getContestURLFromID(id)}'>${CodeforcesNames.CODEFORCES_TITLE}</a>"
      val eduToolsLink = "<a href='${CodeforcesNames.CODEFORCES_EDU_TOOLS_HELP}'>EduTools</a>"
      description = EduCoreBundle.message("codeforces.past.course.description", date, codeforcesLink, eduToolsLink)
    }
  }

  private fun setContestParameters(contestParameters: ContestParameters) {
    id = contestParameters.id
    language = contestParameters.languageId
    languageCode = contestParameters.locale
    endDateTime = contestParameters.endDateTime
    updateDate = Date()
    startDate = contestParameters.startDate
    length = contestParameters.length
    isRegistrationOpen = contestParameters.isRegistrationOpen
    availableLanguages = contestParameters.availableLanguages
    name = contestParameters.name
  }

  override fun getIcon(): Icon = EducationalCoreIcons.Codeforces
  override fun getId(): Int = myId
  override fun getItemType(): String = CODEFORCES_COURSE_TYPE
  override fun getCheckAction(): CheckAction = CheckAction(EduCoreBundle.lazyMessage("action.codeforces.run.local.tests.text"))
  override fun isViewAsEducatorEnabled(): Boolean = false

  fun getContestUrl(): String = getContestURLFromID(id)
  fun isOngoing(): Boolean = if (endDateTime == null) false else (endDateTime!! > ZonedDateTime.now())

  private fun parseResponseToAddContent(doc: Document) {
    @NonNls val error = "Parsing failed. Unable to find CSS elements:"
    name = doc.selectFirst(".caption")?.text() ?: error("$error caption")
    val problems = doc.select(".problem-statement") ?: error("$error problem-statement")

    description = problems.joinToString("\n") {
      it.select("div.header")?.select("div.title")?.text() ?: error("$error div.header, div.title")
    }

    val lesson = Lesson()
    lesson.name = CodeforcesNames.CODEFORCES_PROBLEMS
    lesson.course = this

    addLesson(lesson)
    problems.forEachIndexed { index, task -> lesson.addTask(CodeforcesTask.create(task, lesson, index + 1)) }
  }

  override fun getTags(): List<Tag> {
    return emptyList()
  }

  override fun getCompatibility(): CourseCompatibility {
    return CourseCompatibility.Compatible
  }
}