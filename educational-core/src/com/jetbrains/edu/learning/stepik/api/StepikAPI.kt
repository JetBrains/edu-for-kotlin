@file:Suppress("unused", "PropertyName", "MemberVisibilityCanBePrivate", "UNUSED_PARAMETER")

package com.jetbrains.edu.learning.stepik.api

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.module.SimpleModule
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.JSON_FORMAT_VERSION
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.ChoiceStepSource
import com.jetbrains.edu.learning.stepik.StepSource
import com.jetbrains.edu.learning.stepik.StepikUserInfo
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.SOLUTIONS_HIDDEN
import java.util.*

const val USERS = "users"
const val META = "meta"
const val COURSES = "courses"
const val LESSONS = "lessons"
const val STEP_SOURCES = "step-sources"
const val SUBMISSIONS = "submissions"
const val PROGRESSES = "progresses"
const val ATTEMPTS = "attempts"
const val ASSIGNMENTS = "assignments"
const val ENROLLMENT = "enrollment"
const val SUBMISSION = "submission"
const val ATTEMPT = "attempt"
const val VIEW = "view"
const val UNIT = "unit"
const val STEP_SOURCE = "step_source"
const val MEMBER = "member"
const val USER = "user"
const val GROUP = "group"
const val ASSIGNMENT = "assignment"
const val STEP = "step"
const val IS_PASSED = "is_passed"
const val IS_MULTIPLE_CHOICE = "is_multiple_choice"
const val OPTIONS = "options"
const val DATASET = "dataset"
const val REPLY = "reply"
const val HINT = "hint"
const val FEEDBACK = "feedback"
const val MESSAGE = "message"
const val CHOICES = "choices"
const val SCORE = "score"
const val SOLUTION = "solution"
const val CODE = "code"
const val EDU_TASK = "edu_task"
const val VERSION = "version"
const val ATTACHMENTS = "attachments"
const val ADDITIONAL_FILES = "additional_files"
const val TASK_FILES = "task_files"
const val TASKS_INFO = "tasks_info"
const val TIME = "time"
const val MEMORY = "memory"

// List wrappers for GET requests:

class UsersList {
  @JsonProperty(USERS)
  lateinit var users: List<StepikUserInfo>
}

class CoursesList {
  @JsonProperty(META)
  lateinit var meta: Map<Any, Any>

  @JsonProperty(COURSES)
  lateinit var courses: MutableList<EduCourse>
}

class SectionsList {
  @JsonProperty(SECTIONS)
  lateinit var sections: List<Section>
}

class LessonsList {
  @JsonProperty(LESSONS)
  lateinit var lessons: List<Lesson>
}

class UnitsList {
  @JsonProperty(UNITS)
  lateinit var units: List<StepikUnit>
}

class StepsList {
  @JsonProperty(STEPS)
  lateinit var steps: List<StepSource>
}

class StepSourcesList {
  @JsonProperty(STEP_SOURCES)
  lateinit var steps: List<StepSource>
}

class ChoiceStepSourcesList {
  @JsonProperty(STEP_SOURCES)
  lateinit var steps: List<ChoiceStepSource>
}

class SubmissionsList {
  @JsonProperty(META)
  lateinit var meta: Map<Any, Any>

  @JsonProperty(SUBMISSIONS)
  lateinit var submissions: List<Submission>
}

class ProgressesList {
  @JsonProperty(PROGRESSES)
  lateinit var progresses: List<Progress>
}

class AttemptsList {
  @JsonProperty(ATTEMPTS)
  lateinit var attempts: List<Attempt>
}

class AssignmentsList {
  @JsonProperty(ASSIGNMENTS)
  lateinit var assignments: List<Assignment>
}

class AttachmentsList {
  @JsonProperty(ATTACHMENTS)
  lateinit var attachments: List<Attachment>
}

// Data wrappers for POST requests:

class EnrollmentData(courseId: Int) {
  @JsonProperty(ENROLLMENT)
  var enrollment: Enrollment = Enrollment(courseId.toString())
}

class SubmissionData() {
  @JsonProperty(SUBMISSION)
  lateinit var submission: Submission

  constructor(attemptId: Int, score: String, files: ArrayList<SolutionFile>, task: Task) : this() {
    val objectMapper = StepikConnector.createMapper(SimpleModule())
    val serializedTask = objectMapper.writeValueAsString(TaskData(task))

    submission = Submission(score, attemptId, files, serializedTask)
  }
}

class AttemptData(step: Int) {
  @JsonProperty(ATTEMPT)
  var attempt: Attempt = Attempt(step)
}

class ViewData(assignment: Int, step: Int) {
  @JsonProperty(VIEW)
  var view: View = View(assignment, step)
}

class CourseData(course: Course) {
  @JsonProperty(COURSE)
  var course: EduCourse = EduCourse()

  init {
    this.course.name = course.name
    this.course.environment = course.environment
    this.course.language = course.language
    this.course.description = course.description
    this.course.authors = course.authors
    if (course is EduCourse && course.isRemote) {
      this.course.instructors = course.instructors
      this.course.isPublic = course.isPublic
    }
  }
}

class SectionData(@field:JsonProperty(SECTION) var section: Section)

class LessonData(lesson: Lesson) {
  @JsonProperty(LESSON)
  var lesson: Lesson = Lesson()

  init {
    this.lesson.name = lesson.name
    this.lesson.id = lesson.id
    this.lesson.steps = ArrayList()
    this.lesson.isPublic = true
  }
}

class UnitData(lessonId: Int, position: Int, sectionId: Int, unitId: Int? = null) {
  @JsonProperty(UNIT)
  var unit: StepikUnit = StepikUnit()

  init {
    unit.lesson = lessonId
    unit.position = position
    unit.section = sectionId
    unit.id = unitId
  }
}

class StepSourceData(project: Project, task: Task, lessonId: Int) {
  @JsonProperty(STEP_SOURCE)
  var stepSource: StepSource = StepSource(project, task, lessonId)
}

class MemberData(userId: String, group: String) {
  @JsonProperty(MEMBER)
  var member: Member = Member(userId, group)
}

class TaskData {
  @JsonProperty(TASK)
  lateinit var task: Task

  constructor()

  constructor(task: Task) {
    this.task = task
  }
}

// Auxiliary:

class Member(@field:JsonProperty(USER) var user: String, @field:JsonProperty(GROUP) var group: String)

class Enrollment(@field:JsonProperty(COURSE) var course: String)

class View(@field:JsonProperty(ASSIGNMENT) var assignment: Int, @field:JsonProperty(STEP) var step: Int)

class Progress {
  @JsonProperty(ID)
  lateinit var id: String

  @JsonProperty(IS_PASSED)
  var isPassed: Boolean = false
}

class Assignment {
  @JsonProperty(ID)
  var id: Int = 0

  @JsonProperty(STEP)
  var step: Int = 0
}

class Dataset {
  @JsonProperty(IS_MULTIPLE_CHOICE)
  var isMultipleChoice: Boolean = false

  @JsonProperty(OPTIONS)
  var options: List<String>? = null

  constructor()
  constructor(emptyDataset: String)  // stepik returns empty string instead of null
}

class Attempt {

  @JsonProperty(STEP)
  var step: Int = 0

  @JsonProperty(DATASET)
  var dataset: Dataset? = null

  @JsonProperty(STATUS)
  var status: String? = null

  @JsonProperty(USER)
  var user: String? = null

  @JsonProperty(ID)
  var id: Int = 0

  val isActive: Boolean
    @JsonIgnore
    get() = status == "active"

  constructor()

  constructor(step: Int) {
    this.step = step
  }
}

class StepikUnit {
  @JsonProperty(ID)
  var id: Int? = 0

  @JsonProperty(SECTION)
  var section: Int = 0

  @JsonProperty(LESSON)
  var lesson: Int = 0

  @JsonProperty(POSITION)
  var position: Int = 0

  @JsonProperty(ASSIGNMENTS)
  var assignments: List<Int> = mutableListOf()

  @JsonProperty(UPDATE_DATE)
  var updateDate: Date = Date()
}

class Feedback {
  @JsonProperty(MESSAGE)
  var message: String? = null

  constructor()

  constructor(feedback: String) {
    message = feedback
  }
}

class Submission {
  @JsonProperty(ATTEMPT)
  var attempt: Int = 0

  @JsonProperty(REPLY)
  var reply: Reply? = null

  @JsonProperty(ID)
  var id: Int? = null

  @JsonProperty(STATUS)
  var status: String? = null

  @JsonProperty(HINT)
  var hint: String? = null

  @JsonProperty(FEEDBACK)
  var feedback: Feedback? = null

  @JsonProperty(TIME)
  var time: Date? = null

  constructor()

  constructor(score: String, attemptId: Int, files: ArrayList<SolutionFile>, serializedTask: String, feedback: String? = null) {
    reply = Reply(files, score, serializedTask, feedback)
    this.attempt = attemptId
  }
}

class Reply {
  @JsonProperty(CHOICES)
  var choices: BooleanArray? = null

  @JsonProperty(SCORE)
  var score: String? = null

  @JsonProperty(SOLUTION)
  var solution: List<SolutionFile>? = null

  @JsonProperty(LANGUAGE)
  var language: String? = null

  @JsonProperty(CODE)
  var code: String? = null

  @JsonProperty(EDU_TASK)
  var eduTask: String? = null

  @JsonProperty(VERSION)
  var version = JSON_FORMAT_VERSION

  @JsonProperty(FEEDBACK)
  var feedback: Feedback? = null

  constructor()

  constructor(files: List<SolutionFile>, score: String, serializedTask: String, feedback: String? = null) {
    this.score = score
    solution = files
    eduTask = serializedTask
    if (feedback != null) {
      this.feedback = Feedback(feedback)
    }
  }
}

class SolutionFile {
  @JsonProperty(NAME)
  var name: String = ""

  @JsonProperty(TEXT)
  var text: String = ""

  constructor()

  constructor(name: String, text: String) {
    this.name = name
    this.text = text
  }
}

class Attachment {
  @JsonProperty(ID)
  var id: Int = 0

  @JsonProperty(NAME)
  lateinit var name: String
}

open class AdditionalInfo

class CourseAdditionalInfo : AdditionalInfo {
  @JsonProperty(ADDITIONAL_FILES)
  lateinit var additionalFiles: List<TaskFile>

  @JsonProperty(SOLUTIONS_HIDDEN)
  var solutionsHidden: Boolean = false

  constructor()

  @JvmOverloads
  constructor(additionalFiles: List<TaskFile>, solutionsHidden: Boolean = false) {
    this.additionalFiles = additionalFiles
    this.solutionsHidden = solutionsHidden
  }
}

class LessonAdditionalInfo : AdditionalInfo {
  // needed for com.jetbrains.edu.coursecreator.actions.stepik.hyperskill.GetHyperskillLesson
  @JsonProperty(ADDITIONAL_FILES)
  var additionalFiles: List<TaskFile>? = null

  @JsonProperty(CUSTOM_NAME)
  var customName: String? = null

  /**
   * We have another mechanism to store info about plugin tasks: com.jetbrains.edu.learning.stepik.PyCharmStepOptions
   * This object is used to store additional info about lesson or non-plugin tasks
   * (we use lessonInfo for tasks because Stepik API does not have attachments for tasks)
   * */

  @JsonProperty(TASKS_INFO)
  var tasksInfo: Map<Int, TaskAdditionalInfo> = emptyMap()

  constructor()

  constructor(customName: String?, tasksInfo: Map<Int, TaskAdditionalInfo>, additionalFiles: List<TaskFile>? = null) {
    this.customName = customName
    this.tasksInfo = tasksInfo
    this.additionalFiles = additionalFiles
  }

  val isEmpty: Boolean get() = customName.isNullOrEmpty() && tasksInfo.isEmpty()
}

// Not inherited from AdditionalInfo because Stepik does not support Attachments for tasks
class TaskAdditionalInfo {
  @JsonProperty(NAME)
  var name: String = ""

  @JsonProperty(CUSTOM_NAME)
  var customName: String? = null

  @JsonProperty(TASK_FILES)
  lateinit var taskFiles: List<TaskFile>

  constructor()

  constructor(name: String, customName: String?, taskFiles: List<TaskFile>) {
    this.name = name
    this.customName = customName
    this.taskFiles = taskFiles
  }
}
