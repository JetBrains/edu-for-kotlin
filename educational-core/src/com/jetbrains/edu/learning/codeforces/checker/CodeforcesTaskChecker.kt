package com.jetbrains.edu.learning.codeforces.checker

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.checker.*
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.TEST_DATA_FOLDER
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTask
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.editor.EduEditor
import java.awt.datatransfer.StringSelection

class CodeforcesTaskChecker(
  task: CodeforcesTask,
  private val envChecker: EnvironmentChecker,
  project: Project,
  private val codeExecutor: CodeExecutor
) : TaskChecker<CodeforcesTask>(task, project) {

  override fun check(indicator: ProgressIndicator): CheckResult {
    val studyEditor = FileEditorManager.getInstance(project).selectedEditor
    val solution = (studyEditor as EduEditor).editor.document.text
    val url = (task.course as CodeforcesCourse).getSubmissionUrl()

    indicator.text = "Executing tests"
    val testFolders = task.getDir(project)?.findChild(TEST_DATA_FOLDER)?.children.orEmpty()

    for ((index, testFolder) in testFolders.withIndex()) {
      val inputVirtualFile = testFolder.findChild(task.inputFileName) ?: continue
      val outputVirtualFile = testFolder.findChild(task.outputFileName) ?: continue

      val inputDocument = runReadAction { FileDocumentManager.getInstance().getDocument(inputVirtualFile) }
                          ?: error("Can't get document of input file - ${inputVirtualFile.path}")
      val outputDocument = runReadAction { FileDocumentManager.getInstance().getDocument(outputVirtualFile) }
                           ?: error("Can't get document of output file - ${outputVirtualFile.path}")

      val testNumber = index + 1
      indicator.text2 = "Running test $testNumber of ${testFolders.size}"

      val input = runReadAction { inputDocument.text }

      val possibleError = envChecker.checkEnvironment(project)
      if (possibleError != null) return CheckResult(CheckStatus.Unchecked, possibleError)

      val result = withRunProcessesWithPtyOff {
        codeExecutor.execute(project, task, indicator, input)
      }

      val output = when (result) {
        is Ok -> result.value
        is Err -> return result.error
      }

      val expectedOutput = runReadAction { outputDocument.text }
      if (expectedOutput.trimEnd('\n') != output.trimEnd('\n')) {
        val message = "Test №$testNumber is failed"
        val diff = CheckResultDiff(expected = expectedOutput, actual = output, message = message)
        return CheckResult(CheckStatus.Failed, message, diff = diff)
      }
    }

    CopyPasteManager.getInstance().setContents(StringSelection(solution))
    return CheckResult(CheckStatus.Unchecked, "<a href=\"$url\">Submit solution</a> (already copied to clipboard)<br>", needEscape = false)
  }

  private fun <T> withRunProcessesWithPtyOff(action: () -> T): T {
    val value = Registry.get(runProcessesWithPtyRegistryKey)
    val currentValue = value.asBoolean()
    val result: T
    try {
      value.setValue(false)
      result = action()
    }
    finally {
      value.setValue(currentValue)
    }
    return result
  }

  companion object {
    const val runProcessesWithPtyRegistryKey = "run.processes.with.pty"
  }
}
