/*
 * Copyright 2000-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jetbrains.edu.learning.projectView

import com.intellij.ide.SelectInTarget
import com.intellij.ide.impl.ProjectViewSelectInTarget
import com.intellij.ide.projectView.ProjectView
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.AbstractProjectViewPSIPane
import com.intellij.ide.projectView.impl.ProjectAbstractTreeStructureBase
import com.intellij.ide.projectView.impl.ProjectTreeStructure
import com.intellij.ide.projectView.impl.ProjectViewTree
import com.intellij.ide.ui.laf.darcula.ui.DarculaProgressBarUI
import com.intellij.ide.util.PropertiesComponent
import com.intellij.ide.util.treeView.AbstractTreeBuilder
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.ide.util.treeView.AbstractTreeUpdater
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.util.ColorProgressBar
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.ui.ScrollPaneFactory
import com.intellij.util.ArrayUtil
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.coursecreator.CCStudyItemDeleteProvider
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.projectView.CCLessonNode
import com.jetbrains.edu.coursecreator.projectView.CCSectionNode
import com.jetbrains.edu.coursecreator.projectView.CCTaskNode
import com.jetbrains.edu.learning.CourseSetListener
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.StudyItem
import icons.EducationalCoreIcons
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.TestOnly
import java.awt.BorderLayout
import java.awt.Color
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.border.EmptyBorder
import javax.swing.tree.DefaultTreeModel

class CourseViewPane(project: Project) : AbstractProjectViewPSIPane(project) {

  private val myStudyItemDeleteProvider = CCStudyItemDeleteProvider()

  private lateinit var progressBar: JProgressBar

  override fun createTree(treeModel: DefaultTreeModel): ProjectViewTree {
    return object : ProjectViewTree(myProject, treeModel) {
      override fun toString(): String = "$title ${super.toString()}"
    }
  }

  override fun createComponent(): JComponent {
    val component = super.createComponent()
    installComparator(treeBuilder, EduNodeComparator)

    if (!EduUtils.isStudentProject(myProject)) return component
    val panel = JPanel(BorderLayout())
    panel.background = UIUtil.getTreeBackground()

    panel.add(createProgressPanel(), BorderLayout.NORTH)
    panel.add(tree, BorderLayout.CENTER)

    if (StudyTaskManager.getInstance(myProject).course != null) {
      updateCourseProgress()
    }
    else {
      myProject.messageBus.connect().subscribe(StudyTaskManager.COURSE_SET, object : CourseSetListener {
        override fun courseSet(course: Course) {
          updateCourseProgress()
        }
      })
    }
    return ScrollPaneFactory.createScrollPane(panel)
  }

  private fun createProgressPanel(): JPanel {
    val panel = JPanel(BorderLayout())

    progressBar = JProgressBar()

    progressBar.ui = object : DarculaProgressBarUI() {
      override fun getRemainderColor(): Color {
        return JBColor(Gray._237, Color(76, 77, 79))
      }
    }
    progressBar.foreground = ColorProgressBar.GREEN
    progressBar.isIndeterminate = false
    progressBar.putClientProperty("ProgressBar.flatEnds", java.lang.Boolean.TRUE)
    panel.background = UIUtil.getTreeBackground()
    panel.add(progressBar, BorderLayout.NORTH)
    panel.border = EmptyBorder(0, 0, 5, 0)
    return panel
  }

  override fun addToolbarActions(actionGroup: DefaultActionGroup?) {
    actionGroup?.removeAll()
    val hideSolvedLessons = object: ToggleAction("Hide Solved Lessons"), DumbAware {
      override fun isSelected(p0: AnActionEvent?): Boolean {
        return PropertiesComponent.getInstance().getBoolean(HIDE_SOLVED_LESSONS, false)
      }

      override fun setSelected(p0: AnActionEvent?, p1: Boolean) {
        val hideSolved = PropertiesComponent.getInstance().getBoolean(HIDE_SOLVED_LESSONS, false)
        PropertiesComponent.getInstance().setValue(HIDE_SOLVED_LESSONS, !hideSolved)
        ProjectView.getInstance(myProject).refresh()
      }
    }
    actionGroup?.add(hideSolvedLessons)
  }

  fun updateCourseProgress() {
    val course = StudyTaskManager.getInstance(myProject).course
    if (course == null) {
      Logger.getInstance(CourseViewPane::class.java).error("course is null")
      return
    }
    val (taskSolved, tasksTotal) = ProgressUtil.countProgress(course)

    progressBar.maximum = tasksTotal
    progressBar.value = taskSolved
  }

  @TestOnly
  fun getProgressBar(): JProgressBar = progressBar

  override fun createStructure(): ProjectAbstractTreeStructureBase = object : ProjectTreeStructure(myProject, ID) {
    override fun createRoot(project: Project?, settings: ViewSettings?): AbstractTreeNode<*> {
      return RootNode(myProject, settings)
    }

    override fun getChildElements(element: Any?): Array<Any> {
      if (element !is AbstractTreeNode<*>) {
        return ArrayUtil.EMPTY_OBJECT_ARRAY
      }
      val elements = element.children
      elements.forEach { node -> node.setParent(element) }
      return ArrayUtil.toObjectArray(elements)
    }
  }
  override fun createTreeUpdater(treeBuilder: AbstractTreeBuilder): AbstractTreeUpdater = AbstractTreeUpdater(treeBuilder)

  override fun getTitle(): String = ID
  override fun getIcon(): Icon = EducationalCoreIcons.CourseTree
  override fun getId(): String = ID
  override fun getWeight(): Int = 10

  override fun createSelectInTarget(): SelectInTarget {
    return object : ProjectViewSelectInTarget(myProject) {
      override fun getMinorViewId(): String? = ID
      override fun toString(): String = ID
    }
  }

  override fun supportsFoldersAlwaysOnTop(): Boolean = false
  override fun supportsSortByType(): Boolean = false

  override fun getData(dataId: String?): Any? {
    if (myProject.isDisposed) return null

    if (CCUtils.isCourseCreator(myProject)) {
      val userObject = selectedNode?.userObject
      val studyItem = when (userObject) {
        is CCTaskNode -> userObject.task
        is CCLessonNode -> userObject.lesson
        is CCSectionNode -> userObject.section
        else -> null
      }
      if (studyItem != null) {
        when {
          PlatformDataKeys.DELETE_ELEMENT_PROVIDER.`is`(dataId) -> return myStudyItemDeleteProvider
          STUDY_ITEM.`is`(dataId) -> return studyItem
        }
      }
    }
    return super.getData(dataId)
  }

  companion object {
    @NonNls
    const val ID = "Course"
    const val HIDE_SOLVED_LESSONS = "Edu.HideSolvedLessons"

    val STUDY_ITEM: DataKey<StudyItem> = DataKey.create<StudyItem>("Edu.studyItem")
  }
}
