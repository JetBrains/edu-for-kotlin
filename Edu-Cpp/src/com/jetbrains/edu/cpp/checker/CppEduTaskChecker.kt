package com.jetbrains.edu.cpp.checker

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.EduTaskCheckerBase
import com.jetbrains.edu.learning.checker.EnvironmentChecker
import com.jetbrains.edu.learning.courseFormat.ext.getAllTestFiles
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask

class CppEduTaskChecker(task: EduTask, envChecker: EnvironmentChecker, project: Project) : EduTaskCheckerBase(task, envChecker, project) {
  override fun createTestConfigurations(): List<RunnerAndConfigurationSettings> =
    task.getAllTestFiles(project).mapNotNull { ConfigurationContext(it).configuration }
}