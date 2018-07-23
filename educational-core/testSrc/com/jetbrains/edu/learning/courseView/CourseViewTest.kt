// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.jetbrains.edu.learning.courseView

import com.intellij.ide.projectView.ProjectView
import com.intellij.ide.projectView.impl.AbstractProjectViewPane
import com.intellij.ide.util.treeView.AbstractTreeBuilder
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.*
import com.intellij.ui.tree.AsyncTreeModel
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.EduState
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.actions.RefreshTaskFileAction
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.projectView.CourseViewPane
import junit.framework.TestCase
import org.junit.Assert
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.swing.JTree

class CourseViewTest : EduTestCase() {
  private var myCourse: Course? = null

  @Throws(Exception::class)
  override fun setUp() {
    super.setUp()
    myCourse?.language = PlainTextLanguage.INSTANCE.id
    ProjectViewTestUtil.setupImpl(project, true)
  }

  fun testCoursePane() {
    configureByTaskFile(1, 1, "taskFile1.txt")
    val pane = createPane()

    val structure = "-Project\n" +
                    " -CourseNode Edu test course  0/4\n" +
                    "  -LessonNode lesson1\n" +
                    "   +TaskNode task1\n" +
                    "   +TaskNode task2\n" +
                    "   +TaskNode task3\n" +
                    "   +TaskNode task4\n"
    PlatformTestUtil.assertTreeEqual(pane.tree, structure)
  }

  fun testProjectOpened() {
    val projectView = ProjectView.getInstance(project)
    projectView.changeView(CourseViewPane.ID)
    val pane = projectView.currentProjectViewPane
    waitWhileBusy(pane)
    EduUtils.openFirstTask(myCourse!!, project)
    PlatformTestUtil.waitForAlarm(600)
    waitWhileBusy(pane)
    val structure = "-Project\n" +
                          " -CourseNode Edu test course  0/4\n" +
                          "  -LessonNode lesson1\n" +
                          "   -TaskNode task1\n" +
                          "    taskFile1.txt\n" +
                          "   +TaskNode task2\n" +
                          "   +TaskNode task3\n" +
                          "   +TaskNode task4\n"
    PlatformTestUtil.assertTreeEqual(pane.tree, structure)
  }

  fun testExpandAfterNavigation() {
    configureByTaskFile(1, 1, "taskFile1.txt")
    val projectView = ProjectView.getInstance(project)
    projectView.changeView(CourseViewPane.ID)
    navigateToNextTask()

    val pane = projectView.currentProjectViewPane
    val structure = "-Project\n" +
                    " -CourseNode Edu test course  0/4\n" +
                    "  -LessonNode lesson1\n" +
                    "   +TaskNode task1\n" +
                    "   -TaskNode task2\n" +
                    "    taskFile2.txt\n" +
                    "   +TaskNode task3\n" +
                    "   +TaskNode task4\n"
    PlatformTestUtil.assertTreeEqual(pane.tree, structure)
  }

  fun testCourseProgress() {
    configureByTaskFile(1, 1, "taskFile1.txt")
    val projectView = ProjectView.getInstance(project)
    projectView.changeView(CourseViewPane.ID)
    val pane = projectView.currentProjectViewPane
    UsefulTestCase.assertInstanceOf(pane, CourseViewPane::class.java)
    TestCase.assertNotNull((pane as CourseViewPane).getProgressBar())
  }

  fun testSwitchingPane() {
    val projectView = ProjectView.getInstance(project)
    projectView.changeView(CourseViewPane.ID)
    TestCase.assertEquals(CourseViewPane.ID, projectView.currentViewId)
  }

  fun testCheckTask() {
    configureByTaskFile(1, 1, "taskFile1.txt")
    val projectView = ProjectView.getInstance(project)
    projectView.changeView(CourseViewPane.ID)

    val fileName = "lesson1/task1/taskFile1.txt"
    val taskFile = myFixture.findFileInTempDir(fileName)
    val action = CheckAction()
    launchAction(taskFile, action)

    val pane = projectView.currentProjectViewPane
    val structure = "-Project\n" +
                          " +CourseNode Edu test course  1/4\n"
    waitWhileBusy(pane)
    PlatformTestUtil.assertTreeEqual(pane.tree, structure)

    val refreshTaskFileAction = RefreshTaskFileAction()
    launchAction(taskFile, refreshTaskFileAction)
  }


  private fun launchAction(taskFile: VirtualFile, action: AnAction) {
    val e = getActionEvent(taskFile, action)
    action.beforeActionPerformedUpdate(e)
    Assert.assertTrue(e.presentation.isEnabled && e.presentation.isVisible)
    action.actionPerformed(e)
  }

  private fun getActionEvent(virtualFile: VirtualFile, action: AnAction): TestActionEvent {
    val context = MapDataContext()
    context.put(CommonDataKeys.VIRTUAL_FILE_ARRAY, arrayOf(virtualFile))
    context.put<Project>(CommonDataKeys.PROJECT, project)
    return TestActionEvent(context, action)
  }

  private fun navigateToNextTask() {
    val eduEditor = EduUtils.getSelectedEduEditor(project)
    val eduState = EduState(eduEditor)
    TestCase.assertTrue(eduState.isValid)
    val targetTask = NavigationUtils.nextTask(eduState.task)
    TestCase.assertNotNull(targetTask)
    NavigationUtils.navigateToTask(project, targetTask!!)
  }

  @Throws(IOException::class)
  override fun createCourse() {
    myFixture.copyDirectoryToProject("lesson1", "lesson1")
    myCourse = Course()
    myCourse!!.language = PlainTextLanguage.INSTANCE.id
    myCourse!!.name = "Edu test course"
    StudyTaskManager.getInstance(myFixture.project).course = myCourse

    val lesson1 = createLesson(1, 4)
    myCourse!!.addLesson(lesson1)
    myCourse!!.init(null, null, false)
  }

  private fun createPane(): CourseViewPane {
    val pane = CourseViewPane(project)
    pane.createComponent()
    Disposer.register(project, pane)
    return pane
  }

  override fun getTestDataPath(): String {
    return super.getTestDataPath() + "/projectView"
  }
}

fun waitWhileBusy(pane: AbstractProjectViewPane) {
  val startTimeMillis = System.currentTimeMillis()
  while (isBusy(pane.tree)) {
    assertMaxWaitTimeSince(startTimeMillis)
  }
}

private fun getMillisSince(startTimeMillis: Long): Long {
  return System.currentTimeMillis() - startTimeMillis
}

private fun assertMaxWaitTimeSince(startTimeMillis: Long) {
  assert(getMillisSince(startTimeMillis) <= MAX_WAIT_TIME) { "the waiting takes too long" }
}

private fun isBusy(tree: JTree): Boolean {
  UIUtil.dispatchAllInvocationEvents()
  val model = tree.model
  if (model is AsyncTreeModel) {
    if (model.isProcessing) return true
    UIUtil.dispatchAllInvocationEvents()
    return model.isProcessing
  }
  val builder = AbstractTreeBuilder.getBuilderFor(tree) ?: return false
  val ui = builder.ui ?: return false
  return ui.hasPendingWork()
}
private val MAX_WAIT_TIME = TimeUnit.MINUTES.toMillis(2)

