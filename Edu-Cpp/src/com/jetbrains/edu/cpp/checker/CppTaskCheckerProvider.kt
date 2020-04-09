package com.jetbrains.edu.cpp.checker

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.CodeExecutor
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.checker.TheoryTaskChecker
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask

class CppTaskCheckerProvider : TaskCheckerProvider {
  // TODO implement envChecker validation
  override fun getEduTaskChecker(task: EduTask, project: Project): TaskChecker<EduTask> = CppEduTaskChecker(task, envChecker, project)
  override fun getTheoryTaskChecker(task: TheoryTask, project: Project): TheoryTaskChecker = CppTheoryTaskChecker(task, project)
  override fun getCodeExecutor(): CodeExecutor = CppCodeExecutor()
}
