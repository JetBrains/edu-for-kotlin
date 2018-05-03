package com.jetbrains.edu.learning.placeholderDependencies

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.actions.RefreshTaskFileAction
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.ext.getDocument

class PlaceholderDependencyTest : EduTestCase() {

  fun `test placeholder replaced with solution`() {
    courseWithFiles {
      lesson {
        eduTask {
          taskFile("Task.kt", """
          |def f():
          |  <p>print(1)</p>
        """.trimMargin("|"))
        }
      }
      lesson {
        eduTask {
          taskFile("Task.kt", """
          |def <p>foo</p>():
          |  <p>type here</p>
          """.trimMargin("|")) {
            placeholder(1, dependency = "lesson1#task1#Task.kt#1")
          }
        }
      }
    }

    getCourse().lessons[0].taskList[0].status = CheckStatus.Solved

    val virtualFile = findVirtualFile(1, 0, "Task.kt")
    myFixture.openFileInEditor(virtualFile)

    checkPlaceholderContent("print(1)", findPlaceholder(1, 0, "Task.kt", 1))
  }

  fun `test unsolved dependency`() {
    courseWithFiles {
      lesson {
        eduTask { taskFile("task.txt", "task with <p>placeholder</p>") }
      }
      lesson {
        eduTask {
          taskFile("task.txt", "task with another <p>type here</p>") {
            placeholder(0, dependency = "lesson1#task1#task.txt#1")
          }
        }
      }
    }

    val virtualFile = findVirtualFile(1, 0, "task.txt")
    myFixture.openFileInEditor(virtualFile)

    checkEditorNotification(virtualFile, listOf("task1"))

    checkPlaceholderContent("type here", findPlaceholder(1, 0, "task.txt", 0))
  }

  fun `test no replacement`() {
    courseWithFiles {
      lesson {
        eduTask {
          taskFile("Task.kt", """
          |def f():
          |  <p>print(1)</p>
        """.trimMargin("|"))
        }
      }
      lesson {
        eduTask {
          taskFile("Task.kt", """
          |def <p>foo</p>():
          |  <p>type here</p>
          """.trimMargin("|")) {
            placeholder(1, dependency = "lesson1#task1#Task.kt#1")
          }
        }
      }
    }

    getCourse().lessons[0].taskList[0].status = CheckStatus.Failed

    val virtualFile = findVirtualFile(1, 0, "Task.kt")
    myFixture.openFileInEditor(virtualFile)

    checkPlaceholderContent("type here", findPlaceholder(1, 0, "Task.kt", 1))
  }

  fun `test several files`() {
    courseWithFiles {
      lesson {
        eduTask {
          taskFile("Task.kt", """
          |def f():
          |  <p>print(1)</p>
        """.trimMargin("|"))
        }
      }
      lesson {
        eduTask {
          taskFile("Task.kt", """
          |def foo():
          |  <p>type here</p>
          """.trimMargin("|")) {
            placeholder(0, dependency = "lesson1#task1#Task.kt#1")
          }
          taskFile("Task1.kt", """
            |def bar():
            |<p>type here</p>
          """.trimMargin("|")) {
            placeholder(0, dependency = "lesson1#task1#Task.kt#1")
          }
        }
      }
    }

    getCourse().lessons[0].taskList[0].status = CheckStatus.Solved

    val virtualFile = findVirtualFile(1, 0, "Task.kt")
    myFixture.openFileInEditor(virtualFile)

    checkPlaceholderContent("print(1)", findPlaceholder(1, 0, "Task.kt", 0))
    checkPlaceholderContent("print(1)", findPlaceholder(1, 0, "Task1.kt", 0))
  }

  fun `test refresh task file with dependency`() {
    courseWithFiles {
      lesson {
        eduTask { taskFile("task.txt", "<p>placeholder</p>") }
      }
      lesson {
        eduTask {
          taskFile("task.txt", "this is a <p>type here</p> and one more <p>type here</p>") {
            placeholder(0, dependency = "lesson1#task1#task.txt#1")
            placeholder(0, possibleAnswer = "placeholder")
          }
        }
      }
    }

    getCourse().lessons[0].taskList[0].status = CheckStatus.Solved
    val secondPlaceholder = findPlaceholder(1, 0, "task.txt", 1)
    CCUtils.replaceAnswerPlaceholder(secondPlaceholder.taskFile.getDocument(project)!!, secondPlaceholder)

    val virtualFile = findVirtualFile(1, 0, "task.txt")
    myFixture.openFileInEditor(virtualFile)

    RefreshTaskFileAction.refresh(project)

    checkPlaceholderContent("placeholder", findPlaceholder(1, 0, "task.txt", 0))
    checkPlaceholderContent("type here", findPlaceholder(1, 0, "task.txt", 1))
  }

  private fun checkEditorNotification(virtualFile: VirtualFile, taskNames: List<String>) {
    val fileEditor = FileEditorManager.getInstance(project).getSelectedEditor(virtualFile)!!
    val notificationPanel = fileEditor.getUserData(UnsolvedDependenciesNotificationProvider.KEY)
    assertNotNull("Notification not shown", notificationPanel)
    assertEquals("Panel text is incorrect", UnsolvedDependenciesNotificationProvider.getText(taskNames),
                          notificationPanel?.getText())
  }

  private fun checkPlaceholderContent(expectedContent: String, answerPlaceholder: AnswerPlaceholder) {
    val taskFile = answerPlaceholder.taskFile
    val document = taskFile.getDocument(project)!!
    val startOffset = answerPlaceholder.offset
    val endOffset = startOffset + answerPlaceholder.realLength
    val actualContent = document.getText(TextRange.create(startOffset, endOffset))

    assertEquals("Placeholder content is incorrect", expectedContent, actualContent)
  }
}