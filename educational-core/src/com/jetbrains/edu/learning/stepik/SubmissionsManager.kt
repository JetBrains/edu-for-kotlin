package com.jetbrains.edu.learning.stepik

import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.stepik.api.Reply
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import com.jetbrains.edu.learning.stepik.api.Submission
import java.util.concurrent.ConcurrentHashMap

object SubmissionsManager {
  private val submissions = ConcurrentHashMap<Int, MutableList<Submission>>()

  @JvmStatic
  fun getSubmissionsFromMemory(taskId: Int): List<Submission>? {
    return submissions[taskId]
  }

  @JvmStatic
  fun getSubmissions(taskId: Int, isSolved: Boolean): List<Submission> {
    val status = if (isSolved) EduNames.CORRECT else EduNames.WRONG
    return getAllSubmissions(taskId).filter { it.status == status }
  }

  @JvmStatic
  fun getAllSubmissions(taskId: Int): MutableList<Submission> {
    return submissions.getOrPut(taskId) { StepikConnector.getInstance().getAllSubmissions(taskId) }
  }

  @JvmStatic
  fun getLastSubmission(taskId: Int, isSolved: Boolean): Reply? {
    val submissions = getSubmissions(taskId, isSolved)
    return submissions.firstOrNull()?.reply
  }

  @JvmStatic
  fun addToSubmissions(taskId: Int, submission: Submission?) {
    if (submission == null) return
    val submissionsList = submissions.getOrPut(taskId) { mutableListOf(submission) }
    if (!submissionsList.contains(submission)) {
      submissionsList.add(submission)
      submissionsList.sortByDescending { it.time }
      //potential race when loading submissions and checking task at one time
    }
  }

  fun putToSubmissions(taskId: Int, submissionsToAdd: MutableList<Submission>) {
    submissions[taskId] = submissionsToAdd
  }
}