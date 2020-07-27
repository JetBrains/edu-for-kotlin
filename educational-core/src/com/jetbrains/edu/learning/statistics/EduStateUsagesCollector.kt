package com.jetbrains.edu.learning.statistics

import com.intellij.internal.statistic.beans.MetricEvent
import com.intellij.internal.statistic.beans.newMetric
import com.intellij.internal.statistic.service.fus.collectors.ApplicationUsagesCollector
import com.jetbrains.edu.coursecreator.actions.CCPluginToggleAction
import com.jetbrains.edu.learning.EduSettings

/**
 * IMPORTANT: if you modify anything in this class, updated whitelist rules should be
 * provided to analytics platform team.
 * See `docs/statisticsRules.md` for more information
 */
class EduStateUsagesCollector : ApplicationUsagesCollector() {

  private enum class EduRole {
    STUDENT, EDUCATOR
  }

  override fun getGroupId() = "educational.state"

  override fun getMetrics(): MutableSet<MetricEvent> {
    val metrics = HashSet<MetricEvent>()

    val taskPanel = EduSettings.getInstance().javaUiLibrary
    metrics.add(newMetric("task.panel", taskPanel))

    val role = if (CCPluginToggleAction.isCourseCreatorFeaturesEnabled) EduRole.EDUCATOR else EduRole.STUDENT
    metrics.add(newMetric("role", role))

    return metrics
  }

  override fun getVersion() = 2
}