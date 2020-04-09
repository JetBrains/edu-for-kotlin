package com.jetbrains.edu.rust.checker

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.CodeExecutor
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask

class RsTaskCheckerProvider : TaskCheckerProvider {
  // TODO implement envChecker validation
  override fun getEduTaskChecker(task: EduTask, project: Project): TaskChecker<EduTask> = RsEduTaskChecker(project, envChecker, task)

  override fun getCodeExecutor(): CodeExecutor = RsCodeExecutor()
}
