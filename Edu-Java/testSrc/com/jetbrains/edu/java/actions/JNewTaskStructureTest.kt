package com.jetbrains.edu.java.actions

import com.intellij.lang.Language
import com.intellij.lang.java.JavaLanguage
import com.jetbrains.edu.coursecreator.actions.create.CCNewTaskStructureTestBase
import com.jetbrains.edu.jvm.JdkProjectSettings

class JNewTaskStructureTest : CCNewTaskStructureTestBase() {
  override val language: Language get() = JavaLanguage.INSTANCE
  override val settings: Any get() = JdkProjectSettings.emptySettings()

  fun `test create edu task`() = checkEduTaskCreation(
    fullTaskStructure = {
      file("task.html")
      dir("src") {
        file("Task.java")
      }
      dir("test") {
        file("Tests.java")
      }
    },
    taskStructureWithoutSources = {
      file("task.html")
      dir("test") {
        file("Tests.java")
      }
    }
  )

  fun `test create output task`() = checkOutputTaskCreation(
    fullTaskStructure = {
      file("task.html")
      dir("src") {
        file("Main.java")
      }
      dir("test") {
        file("output.txt")
      }
    },
    taskStructureWithoutSources = {
      file("task.html")
      dir("test") {
        file("output.txt")
      }
    }
  )

  fun `test create theory task`() = checkTheoryTaskCreation(
    fullTaskStructure = {
      file("task.html")
      dir("src") {
        file("Main.java")
      }
    },
    taskStructureWithoutSources = {
      file("task.html")
    }
  )

  fun `test create IDE task`() = checkIdeTaskCreation(
    fullTaskStructure = {
      file("task.html")
      dir("src") {
        file("Main.java")
      }
    },
    taskStructureWithoutSources = {
      file("task.html")
    }
  )

  fun `test create choice task`() = checkChoiceTaskCreation(
    fullTaskStructure = {
      file("task.html")
      dir("src") {
        file("Main.java")
      }
    },
    taskStructureWithoutSources = {
      file("task.html")
    }
  )
}
