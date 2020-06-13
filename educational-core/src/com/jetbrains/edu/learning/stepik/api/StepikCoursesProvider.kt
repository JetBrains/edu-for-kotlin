package com.jetbrains.edu.learning.stepik.api

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.util.ConcurrencyUtil
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.compatibility.CourseCompatibility
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseVisibility
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.stepik.course.StepikCourse
import com.jetbrains.edu.learning.stepik.course.stepikCourseFromRemote
import com.jetbrains.edu.learning.stepik.featuredStepikCourses
import com.jetbrains.edu.learning.stepik.inProgressCourses
import kotlinx.coroutines.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext


class StepikCoursesProvider : CoroutineScope {
  private val LOG = Logger.getInstance(StepikCoursesProvider::class.java)
  private val THREAD_NUMBER = Runtime.getRuntime().availableProcessors()
  private val EXECUTOR_SERVICE = Executors.newFixedThreadPool(THREAD_NUMBER)
  private val PUBLIC_COURSES_THREADS_NUMBER = 4
  private val loadedCourses: Deferred<List<EduCourse>> = async { loadAllCourses() }

  override val coroutineContext: CoroutineContext
    get() = Dispatchers.IO

  private suspend fun loadAllCourses(): List<EduCourse> {
    LOG.info("Loading courses started...")
    val startTime = System.currentTimeMillis()

    return coroutineScope {
      val publicCourses = async { getPublicCourseInfos() }
      val privateCourses = async { loadPrivateCourseInfos() }
      val listedStepikCourses = async { loadListedStepikCourses() }

      val result = awaitAll(publicCourses, privateCourses, listedStepikCourses).flatten()

      coroutineScope {
        launch { setAuthors(result) }
        launch { setReviews(result) }
      }

      LOG.info("Loading courses finished...Took " + (System.currentTimeMillis() - startTime) + " ms")
      result
    }
  }

  suspend fun getStepikCourses(): List<StepikCourse> {
    return loadedCourses.await().filterIsInstance<StepikCourse>()
  }

  private fun loadPrivateCourseInfos(): List<EduCourse> {
    if (EduSettings.getInstance().user == null) {
      return emptyList()
    }
    val result = mutableListOf<EduCourse>()
    var currentPage = 1
    while (true) {
      val coursesList = StepikConnector.getInstance().getCourses(false, currentPage, true) ?: break

      val availableCourses = getAvailableCourses(coursesList)
      result.addAll(availableCourses)
      currentPage += 1
      if (!coursesList.meta.containsKey("has_next") || coursesList.meta["has_next"] == false) break
    }

    return result
  }

  suspend fun getCommunityCourses(): List<Course> {
    return loadedCourses.await().filterNot { it is StepikCourse }
  }

  private fun getPublicCourseInfos(): List<EduCourse> {
    val indicator = ProgressManager.getInstance().progressIndicator
    val tasks = mutableListOf<Callable<List<EduCourse>?>>()
    val minEmptyPageNumber = AtomicInteger(Integer.MAX_VALUE)

    for (i in 0 until PUBLIC_COURSES_THREADS_NUMBER) {
      tasks.add(Callable {
        val courses = mutableListOf<EduCourse>()
        var pageNumber = i + 1
        while (pageNumber < minEmptyPageNumber.get() && addCoursesFromStepik(courses, true, pageNumber, null, minEmptyPageNumber)) {
          if (indicator != null && indicator.isCanceled) {
            return@Callable null
          }
          pageNumber += PUBLIC_COURSES_THREADS_NUMBER
        }
        return@Callable courses
      })
    }

    val result = mutableListOf<EduCourse>()
    ConcurrencyUtil.invokeAll(tasks, EXECUTOR_SERVICE)
      .filterNot { it.isCancelled }
      .mapNotNull { it.get() }
      .forEach { result.addAll(it) }

    return result
  }

  private fun setAuthors(result: List<EduCourse>) {
    val allUsers = StepikConnector.getInstance().getUsers(result)
    val usersById = allUsers.associateBy { it.id }

    for (course in result) {
      val authors = course.instructors.mapNotNull { usersById[it] }
      course.authors = authors
    }
  }

  private fun setReviews(courses: List<EduCourse>) {
    val summaryIds = courses.map { it.reviewSummary }
    val reviewsByCourseId = StepikConnector.getInstance().getCourseReviewSummaries(summaryIds).associateBy { it.courseId }
    for (course in courses) {
      course.reviewScore = reviewsByCourseId[course.id]?.average ?: 0.0
    }
  }

  private fun loadListedStepikCourses(): List<StepikCourse> {
    val courses = StepikConnector.getInstance().getCourses(featuredStepikCourses.keys.plus(inProgressCourses)) ?: return emptyList()
    return courses.mapNotNull { course ->
      val courseId = course.id
      featuredStepikCourses[courseId]?.let { course.language = it }
      val remoteCourse = stepikCourseFromRemote(course) ?: return@mapNotNull null
      if (inProgressCourses.contains(courseId)) {
        remoteCourse.visibility = CourseVisibility.InProgressVisibility(inProgressCourses.indexOf(courseId))
      }
      remoteCourse
    }.filter {
      val compatibility = it.compatibility
      compatibility == CourseCompatibility.Compatible || compatibility is CourseCompatibility.PluginsRequired
    }
  }
}