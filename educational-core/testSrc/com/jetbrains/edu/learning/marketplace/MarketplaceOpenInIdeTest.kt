package com.jetbrains.edu.learning.marketplace

import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.MockProjectOpener
import com.jetbrains.edu.learning.courseGeneration.ProjectOpener
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.marketplace.api.MockMarketplaceConnector
import com.jetbrains.edu.learning.marketplace.courseGeneration.MarketplaceOpenCourseRequest
import com.jetbrains.edu.learning.marketplace.courseGeneration.MarketplaceOpenInIdeRequestHandler
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.onError
import java.util.concurrent.atomic.AtomicInteger

class MarketplaceOpenInIdeTest : EduTestCase() {
  private val mockConnector: MockMarketplaceConnector get() = MarketplaceConnector.getInstance() as MockMarketplaceConnector
  private val mockProjectOpener: MockProjectOpener get() = ProjectOpener.getInstance() as MockProjectOpener
  private val graphqlRequestsCounter: AtomicInteger = AtomicInteger()

  override fun setUp() {
    super.setUp()
    mockProjectOpener.project = project
  }

  override fun getTestDataPath(): String = super.getTestDataPath() + "/marketplace/projectOpener/"

  override fun tearDown() {
    mockProjectOpener.project = null
    graphqlRequestsCounter.set(0)
    super.tearDown()
  }

  private fun configureCoursesResponse(fileName: String) {
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      val path = request.path
      when {
        COURSES_REQUEST_RE.matches(path) -> if (graphqlRequestsCounter.get() == 0) {
          graphqlRequestsCounter.incrementAndGet()
          mockResponse(fileName)
        }
        else mockResponse("update_info.json")
        LOAD_STRUCTURE_REQUEST_RE.matches(path) -> mockResponse("test_course_structure.zip")
        else -> return@withResponseHandler null
      }
    }
  }

  fun `test open course in new project`() {
    configureCoursesResponse("test_course_info.json")

    mockProjectOpener.open(MarketplaceOpenInIdeRequestHandler, MarketplaceOpenCourseRequest(1))

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.kt", "lesson1 task1")
          }
          dir("test") {
            file("Tests.kt")
          }
          file("task.html")
        }
      }
      dir("lesson2") {
        dir("task1") {
          dir("src") {
            file("Task.kt", "lesson2 task1")
          }
          dir("test") {
            file("Tests.kt")
          }
          file("task.html")
        }
        dir("task2") {
          dir("src") {
            file("Task.kt", "lesson2 task2")
          }
          dir("test") {
            file("Tests.kt")
          }
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }

  fun `test language supported with plugin`() {
    configureCoursesResponse("python_course_info.json")
    doLanguageValidationTest {  assertTrue("actual: $it", it.contains(EduCoreBundle.message("course.dialog.error.plugin.install.and.enable"))) }
  }

  fun `test language not supported in IDE`() {
    configureCoursesResponse("unsupported_language_course_info.json")
    doLanguageValidationTest {
      val expectedMessage = EduCoreBundle.message(
        "rest.service.language.not.supported", ApplicationNamesInfo.getInstance().productName,
        "UnsupportedLanguage"
      )
      assertEquals(expectedMessage, it)
    }
  }

  private fun doLanguageValidationTest(checkError: (String) -> Unit) {
    mockProjectOpener.open(MarketplaceOpenInIdeRequestHandler, MarketplaceOpenCourseRequest(1)).onError {
      checkError(it)
      return
    }

    error("Error is expected: project shouldn't open")
  }

  companion object {
    private val COURSES_REQUEST_RE = """/api/search/graphql?.*""".toRegex()
    private val LOAD_STRUCTURE_REQUEST_RE = """//plugin/.*""".toRegex()
  }
}