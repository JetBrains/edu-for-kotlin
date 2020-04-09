package com.jetbrains.edu.scala.sbt

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.EduTaskCheckerBase
import com.jetbrains.edu.learning.checker.EnvironmentChecker
import com.jetbrains.edu.learning.courseFormat.ext.getAllTestDirectories
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask

class ScalaSbtEduTaskChecker(task: EduTask, envChecker: EnvironmentChecker, project: Project) :
  EduTaskCheckerBase(task, envChecker, project) {
  override fun createTestConfigurations(): List<RunnerAndConfigurationSettings> {
    return task.getAllTestDirectories(project).mapNotNull { ConfigurationContext(it).configuration }
  }
}

