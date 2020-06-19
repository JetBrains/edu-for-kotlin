package com.jetbrains.edu.kotlin.slow.checker

import com.jetbrains.edu.jvm.slow.checker.JdkCheckerTestBase
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import org.jetbrains.kotlin.idea.KotlinLanguage

class KtCheckersTest : JdkCheckerTestBase() {

  override fun createCourse(): Course = course(language = KotlinLanguage.INSTANCE) {
    lesson {
      eduTask("EduTask") {
        kotlinTaskFile("src/Task.kt", """
          fun foo() = 42
        """)
        kotlinTaskFile("test/Tests.kt", """
          import org.junit.Assert
          import org.junit.Test

          class Test {
              @Test
              fun testSolution() {
                  Assert.assertTrue("foo() should return 42", foo() == 42)
              }
          }
        """)
      }
      theoryTask("TheoryTask") {
        kotlinTaskFile("src/Task.kt", """
          fun main(args: Array<String>) {
              val a = 1
              println(a)
          }
        """)
      }
      outputTask("OutputTask") {
        kotlinTaskFile("src/Task.kt", """
          fun main(args: Array<String>) {
              println("OK")
          }
        """)
        taskFile("test/output.txt") {
          withText("OK\n")
        }
      }
      outputTask("OutputTaskMainInsideTask") {
        kotlinTaskFile("src/Task.kt", """
          object Task {
            @JvmStatic
            fun main(args: Array<String>) {
              println("OK")  
            }
          }
        """)
        taskFile("test/output.txt") {
          withText("OK\n")
        }
      }
      outputTask("OutputTaskWithSeveralFiles") {
        kotlinTaskFile("src/utils.kt", """
          fun ok(): String = "OK"
        """)
        kotlinTaskFile("src/Task.kt", """
          fun main(args: Array<String>) {
              println(ok())
          }
        """)
        taskFile("test/output.txt") {
          withText("OK\n")
        }
      }
      outputTask("OutputTask:With;Special&Symbols?()") {
        kotlinTaskFile("src/Task.kt", """
          fun main(args: Array<String>) {
              println("OK")
          }
        """)
        taskFile("test/output.txt") {
          withText("OK\n")
        }
      }
    }
  }

  fun `test kotlin course`() {
    CheckActionListener.expectedMessage { task ->
      when (task) {
        is OutputTask, is EduTask -> CheckUtils.CONGRATULATIONS
        is TheoryTask -> ""
        else -> null
      }
    }
    doTest()
  }
}
