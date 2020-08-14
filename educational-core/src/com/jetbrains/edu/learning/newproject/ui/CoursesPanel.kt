package com.jetbrains.edu.learning.newproject.ui

import com.intellij.ide.BrowserUtil
import com.intellij.ide.plugins.newui.HorizontalLayout
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.ui.FilterComponent
import com.intellij.ui.JBCardLayout
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBPanelWithEmptyText
import com.intellij.util.ui.AsyncProcessIcon
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.compatibility.CourseCompatibility
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.technologyName
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseInfo
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CoursePanel
import com.jetbrains.edu.learning.newproject.ui.coursePanel.MAIN_BG_COLOR
import com.jetbrains.edu.learning.newproject.ui.coursePanel.groups.CoursesListPanel
import com.jetbrains.edu.learning.newproject.ui.courseSettings.CourseSettings
import com.jetbrains.edu.learning.newproject.ui.filters.HumanLanguageFilterDropdown
import com.jetbrains.edu.learning.newproject.ui.filters.ProgrammingLanguageFilterDropdown
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.NonNls
import java.awt.BorderLayout
import java.awt.Rectangle
import java.awt.event.ActionListener
import java.util.*
import javax.swing.JComponent
import javax.swing.JPanel
import kotlin.collections.HashSet


private const val CONTENT_CARD_NAME = "CONTENT"
private const val LOADING_CARD_NAME = "PROGRESS"
private const val NO_COURSES = "NO_COURSES"

abstract class CoursesPanel(private val coursesProvider: CoursesPlatformProvider) : JPanel() {

  protected var coursePanel: CoursePanel = CoursePanel(isLocationFieldNeeded = true) { courseInfo, courseMode, panel ->
    coursesProvider.joinAction(courseInfo, courseMode, panel)
  }

  private val coursesListPanel = CoursesListPanel { courseInfo, courseMode ->
    coursesProvider.joinAction(courseInfo, courseMode, coursePanel)
  }

  private val coursesListDecorator = CoursesListDecorator(coursesListPanel, this.tabInfo(), this.toolbarAction())
  protected var courses: MutableList<Course> = mutableListOf()
  private lateinit var myProgrammingLanguagesFilterDropdown: ProgrammingLanguageFilterDropdown
  private lateinit var myHumanLanguagesFilterDropdown: HumanLanguageFilterDropdown
  private val cardLayout = JBCardLayout()

  val languageSettings get() = coursePanel.languageSettings

  val selectedCourse get() = coursesListPanel.selectedCourse

  val locationString: String
    get() {
      // We use `coursePanel` with location field
      // so `coursePanel.locationString` must return not null value
      return coursePanel.locationString!!
    }

  init {
    layout = cardLayout
    coursesListPanel.setSelectionListener { processSelectionChanged() }

    addCourseValidationListener(object : CourseValidationListener {
      override fun validationStatusChanged(canStartCourse: Boolean) {
        coursePanel.setButtonsEnabled(canStartCourse)
        coursesListPanel.updateButtons()
      }
    })

    this.add(createContentPanel(), CONTENT_CARD_NAME)
    this.add(createLoadingPanel(), LOADING_CARD_NAME)
    this.add(createNoCoursesPanel(), NO_COURSES)
    showProgressState()
  }

  fun hideLoginPanel() = coursesListDecorator.hideLoginPanel()

  private fun createContentPanel(): JPanel {
    val mainPanel = JPanel(BorderLayout())
    mainPanel.add(createAndBindSearchComponent(), BorderLayout.NORTH)
    mainPanel.add(createSplitPane(), BorderLayout.CENTER)
    mainPanel.background = MAIN_BG_COLOR
    return mainPanel
  }

  suspend fun loadCourses() {
    courses.addAll(
      withContext(Dispatchers.IO) {
        coursesProvider.loadCourses()
      }.filter {
        val compatibility = it.compatibility
        compatibility == CourseCompatibility.Compatible || compatibility is CourseCompatibility.PluginsRequired
      })
    updateFilters()
    updateModel(courses, null)
    showContent(courses.isEmpty())
    processSelectionChanged()
  }

  private fun createSplitPane(): JPanel {
    val splitPane = OnePixelSplitter()
    splitPane.firstComponent = coursesListDecorator
    splitPane.secondComponent = coursePanel
    splitPane.divider.background = CoursePanel.DIVIDER_COLOR
    splitPane.proportion = 0.46f

    val splitPaneRoot = JPanel(BorderLayout()) // needed to set borders
    splitPaneRoot.add(splitPane, BorderLayout.CENTER)
    splitPaneRoot.border = JBUI.Borders.customLine(CoursePanel.DIVIDER_COLOR, 1, 0, 0, 0)
    return splitPaneRoot
  }

  protected open fun toolbarAction(): AnAction? = null

  protected open fun tabInfo(): TabInfo? = null

  interface CourseValidationListener {
    fun validationStatusChanged(canStartCourse: Boolean)
  }

  private fun createLoadingPanel() = JPanel(BorderLayout()).apply {
    add(CenteredIcon(), BorderLayout.CENTER)
  }

  private fun createNoCoursesPanel(): JPanel {
    val panel = JBPanelWithEmptyText()
    val text = panel.emptyText
    text.text = EduCoreBundle.message("course.dialog.no.courses", ApplicationNamesInfo.getInstance().fullProductName)
    text.appendSecondaryText(EduCoreBundle.message("help.use.guide1", EduNames.NO_COURSES_URL) + " ",
                             SimpleTextAttributes.GRAYED_ATTRIBUTES, null)
    text.appendSecondaryText(EduCoreBundle.message("help.use.guide2", EduNames.NO_COURSES_URL),
                             SimpleTextAttributes.LINK_ATTRIBUTES, ActionListener { BrowserUtil.browse(EduNames.NO_COURSES_URL) })
    return panel
  }

  private fun showProgressState() = cardLayout.show(this, LOADING_CARD_NAME)

  private fun showContent(empty: Boolean) {
    if (empty) {
      cardLayout.show(this, NO_COURSES)
      return
    }
    cardLayout.show(this, CONTENT_CARD_NAME)
  }

  private fun updateFilters() {
    myHumanLanguagesFilterDropdown.updateItems(humanLanguages(courses))
    myProgrammingLanguagesFilterDropdown.updateItems(programmingLanguages(courses))
  }

  open fun processSelectionChanged() {
    val course = selectedCourse
    if (course != null) {
      coursePanel.bindCourse(course)?.addSettingsChangeListener { doValidation(course) }
    }
    else {
      coursePanel.showEmptyState()
    }
    doValidation(course)
  }

  fun doValidation(course: Course? = coursesListPanel.selectedCourse) {
    val errorState = getErrorState(course) { coursePanel.validateSettings(it) }
    setError(errorState)
    notifyListeners(errorState.courseCanBeStarted)
  }

  fun setError(errorState: ErrorState) {
    coursePanel.setError(errorState)
  }

  private fun filterCourses(courses: List<Course>): List<Course> {
    var filteredCourses = myProgrammingLanguagesFilterDropdown.filter(courses)
    filteredCourses = myHumanLanguagesFilterDropdown.filter(filteredCourses)
    return filteredCourses
  }

  protected fun updateModel(courses: List<Course>, courseToSelect: Course?, filterCourses: Boolean = true) {
    val coursesToAdd = if (filterCourses) filterCourses(courses) else courses
    val courseInfos = coursesToAdd.map {
      CourseInfo(it,
                 { if (coursePanel.course == it) locationString else CourseSettings.nameToLocation(it) },
                 {
                   if (coursePanel.course == it) {
                     languageSettings
                   }
                   else {
                     val settings = CourseSettings.getLanguageSettings(it)
                     settings?.getLanguageSettingsComponents(it, null)
                     settings
                   }
                 })
    }
    coursesListPanel.updateModel(courseInfos, courseToSelect)
  }

  private fun addCourseValidationListener(listener: CourseValidationListener) {
    coursePanel.addCourseValidationListener(listener)
  }

  fun notifyListeners(canStartCourse: Boolean) {
    coursePanel.notifyListeners(canStartCourse)
  }

  private fun humanLanguages(courses: List<Course>): Set<String> = courses.map { it.humanLanguage }.toSet()

  private fun programmingLanguages(courses: List<Course>): Set<String> = courses.mapNotNull { it.technologyName }.toSet()

  private fun createAndBindSearchComponent(): JPanel {
    val searchPanel = JPanel(BorderLayout())
    val searchField = LanguagesFilterComponent()
    coursePanel.bindSearchField(searchField)
    searchPanel.add(searchField, BorderLayout.CENTER)

    myProgrammingLanguagesFilterDropdown = ProgrammingLanguageFilterDropdown(programmingLanguages(emptyList())) {
      updateModel(courses, selectedCourse)
    }
    myHumanLanguagesFilterDropdown = HumanLanguageFilterDropdown(humanLanguages(emptyList())) {
      updateModel(courses, selectedCourse)
    }
    val filtersPanel = JPanel(HorizontalLayout(0))
    filtersPanel.add(myProgrammingLanguagesFilterDropdown)
    filtersPanel.add(myHumanLanguagesFilterDropdown)

    searchPanel.add(filtersPanel, BorderLayout.LINE_END)
    searchPanel.border = JBUI.Borders.empty(8, 0)

    UIUtil.setBackgroundRecursively(searchPanel, MAIN_BG_COLOR)

    return searchPanel
  }

  open fun updateCourseListAfterLogin() {
  }

  inner class LanguagesFilterComponent : FilterComponent("Edu.NewCourse", 5, true) {

    init {
      textEditor.border = null
    }

    override fun filter() {
      val filter = filter
      val filtered = ArrayList<Course>()
      for (course in courses) {
        if (accept(filter, course)) {
          filtered.add(course)
        }
      }
      updateModel(filtered, null)
    }

    private fun accept(@NonNls filter: String, course: Course): Boolean {
      if (filter.isEmpty()) {
        return true
      }
      val filterParts = getFilterParts(filter)
      val courseName = course.name.toLowerCase(Locale.getDefault())
      for (filterPart in filterParts) {
        if (courseName.contains(filterPart)) return true
        for (tag in course.tags) {
          if (tag.accept(filterPart)) {
            return true
          }
        }
        for (authorName in course.authorFullNames) {
          if (authorName.toLowerCase(Locale.getDefault()).contains(filterPart)) {
            return true
          }
        }
      }
      return false
    }

    private fun getFilterParts(@NonNls filter: String): Set<String> {
      return HashSet(listOf(*filter.toLowerCase().split(" ".toRegex()).toTypedArray()))
    }
  }
}

private class CenteredIcon : AsyncProcessIcon.Big("Loading") {
  override fun calculateBounds(container: JComponent): Rectangle {
    val size = container.size
    val iconSize = preferredSize
    return Rectangle((size.width - iconSize.width) / 2, (size.height - iconSize.height) / 2, iconSize.width, iconSize.height)
  }
}
