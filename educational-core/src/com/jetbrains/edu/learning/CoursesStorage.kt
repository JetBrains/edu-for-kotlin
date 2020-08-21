package com.jetbrains.edu.learning

import com.intellij.openapi.components.*
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.XCollection
import com.jetbrains.edu.learning.courseFormat.Course

@State(name = "CoursesStorage", storages = [Storage("coursesStorage.xml", roamingType = RoamingType.DISABLED)])
@Service
class CoursesStorage : SimplePersistentStateComponent<UserCoursesState>(UserCoursesState()) {

  fun addCourse(course: Course, location: String, tasksSolved: Int = 0, tasksTotal: Int = 0) {
    state.addCourse(course, location, tasksSolved, tasksTotal)
  }

  fun getCoursePath(course: Course): String? {
    return state.courses.find { it.name == course.name && it.id == course.id && it.courseMode == course.courseMode }?.location
  }

  fun hasCourse(course: Course): Boolean = getCoursePath(course) != null

  fun getCourseMetaInfo(course: Course): CourseMetaInfo? {
    return state.courses.find { it.name == course.name && it.id == course.id }
  }

  fun updateCourseProgress(course: Course, location: String, tasksSolved: Int, tasksTotal: Int) {
    state.updateCourseProgress(course, location, tasksSolved, tasksTotal)
  }

  fun removeCourseByLocation(location: String) {
    state.removeCourseByLocation(location)
  }

  companion object {
    fun getInstance(): CoursesStorage = service()
  }
}

@Tag("course")
data class CourseMetaInfo(
  var location: String = "",
  var id: Int = 0,
  var name: String = "",
  var description: String = "",
  var type: String = "",
  var courseMode: String = "",
  var environment: String = "",
  var programmingLanguage: String = "",
  var programmingLanguageVersion: String? = null,
  var humanLanguage: String = "",
  var tasksTotal: Int = 0,
  var tasksSolved: Int = 0
) {
  constructor(location: String = "", course: Course, tasksTotal: Int = 0, tasksSolved: Int = 0)
    : this(location,
           course.id,
           course.name,
           course.description,
           course.itemType,
           course.courseMode,
           course.environment,
           course.languageID,
           course.languageVersion,
           course.humanLanguage,
           tasksTotal,
           tasksSolved)
}

class UserCoursesState : BaseState() {
  //  courses list is not updated on course removal and could contain removed courses.
  @get:XCollection(style = XCollection.Style.v2)
  val courses by list<CourseMetaInfo>()

  fun addCourse(course: Course, location: String, tasksSolved: Int = 0, tasksTotal: Int = 0) {
    val systemIndependentLocation = FileUtilRt.toSystemIndependentName(location)
    courses.removeIf { it.location == systemIndependentLocation }
    val courseMetaInfo = CourseMetaInfo(systemIndependentLocation, course, tasksTotal, tasksSolved)
    courses.add(courseMetaInfo)
  }

  fun removeCourseByLocation(location: String) {
    val courseMetaInfo = courses.find { it.location == location }
    courses.remove(courseMetaInfo)
  }

  fun updateCourseProgress(course: Course, location: String, tasksSolved: Int, tasksTotal: Int) {
    val systemIndependentLocation = FileUtilRt.toSystemIndependentName(location)
    val courseMetaInfo = courses.find { it.location == systemIndependentLocation }
    if (courseMetaInfo != null) {
      courseMetaInfo.tasksSolved = tasksSolved
      courseMetaInfo.tasksTotal = tasksTotal
      intIncrementModificationCount()
    }
    else {
      courses.add(CourseMetaInfo(systemIndependentLocation, course, tasksTotal, tasksSolved))
    }
  }
}