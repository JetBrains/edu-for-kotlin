package com.jetbrains.edu.gradle

import com.android.tools.idea.gradle.project.sync.GradleSyncInvoker
import com.android.tools.idea.gradle.project.sync.GradleSyncListener
import com.google.wireless.android.sdk.stats.GradleSyncStats
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.BuildNumber
import com.jetbrains.edu.jvm.gradle.GradleCourseRefresher
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.RefreshCause
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.projectView.CourseViewPane

class StudioGradleCourseRefresher : GradleCourseRefresher {
  override fun isAvailable(): Boolean = EduUtils.isAndroidStudio()

  override fun refresh(project: Project, cause: RefreshCause, listener: EduCourseBuilder.ProjectRefreshListener?) {
    // Gradle projects are refreshed by Android Studio itself on (re)opening since 2019.2
    // BACKCOMPAT: 2019.1
    if (cause == RefreshCause.PROJECT_CREATED && ApplicationInfo.getInstance().build >= BUILD_192 && !isUnitTestMode) return

    val syncListener = object : GradleSyncListener {
      override fun syncSucceeded(project: Project) {
        ExternalSystemUtil.invokeLater(project, ModalityState.NON_MODAL) {
          ProjectView.getInstance(project).changeViewCB(CourseViewPane.ID, null)
        }
        listener?.onSuccess()
      }

      override fun syncFailed(project: Project, errorMessage: String) {
        listener?.onFailure(errorMessage)
      }
    }
    val request = GradleSyncInvoker.Request(GradleSyncStats.Trigger.TRIGGER_PROJECT_MODIFIED)
    GradleSyncInvoker.getInstance().requestProjectSync(project, request, syncListener)
  }

  companion object {
    private val BUILD_192: BuildNumber = BuildNumber.fromString("192")
  }
}
