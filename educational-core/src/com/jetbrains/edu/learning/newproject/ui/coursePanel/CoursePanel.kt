package com.jetbrains.edu.learning.newproject.ui.coursePanel

import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.openapi.util.io.FileUtil
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanelWithEmptyText
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.coursesStorage.CoursesStorage
import com.jetbrains.edu.learning.newproject.ui.ErrorComponent
import com.jetbrains.edu.learning.newproject.ui.ErrorState
import com.jetbrains.edu.learning.newproject.ui.ValidationMessage
import com.jetbrains.edu.learning.newproject.ui.courseSettings.CourseSettingsPanel
import com.jetbrains.edu.learning.newproject.ui.getErrorState
import org.jetbrains.annotations.VisibleForTesting
import java.awt.CardLayout
import java.awt.Component
import java.awt.FlowLayout
import java.io.File
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants
import javax.swing.event.DocumentEvent

const val HORIZONTAL_MARGIN = 20
const val DESCRIPTION_AND_SETTINGS_TOP_OFFSET = 23

private const val EMPTY = "empty"
private const val CONTENT = "content"
private const val ERROR_TOP_GAP = 17
private const val ERROR_RIGHT_GAP = 19
private const val ERROR_PANEL_MARGIN = 10
private const val DEFAULT_BUTTON_OFFSET = 3

abstract class CoursePanel(isLocationFieldNeeded: Boolean) : JPanel() {
  private val tagsPanel: TagsPanel = TagsPanel()
  private val titlePanel = CourseNameHtmlPanel().apply {
    border = JBUI.Borders.empty(11, HORIZONTAL_MARGIN, 0, 0)
  }
  private val authorsPanel = AuthorsPanel()
  private val errorComponent = ErrorComponent(ErrorStateHyperlinkListener(), ERROR_PANEL_MARGIN) { doValidation() }.apply {
    border = JBUI.Borders.empty(ERROR_TOP_GAP, HORIZONTAL_MARGIN, 0, ERROR_RIGHT_GAP)
  }
  @VisibleForTesting
  val buttonsPanel: ButtonsPanel = ButtonsPanel().apply {
    setStartButtonText(startButtonText)
    setOpenButtonText(openButtonText)
  }
  private val courseDetailsPanel: CourseDetailsPanel = CourseDetailsPanel(HORIZONTAL_MARGIN)
  private val settingsPanel: CourseSettingsPanel = CourseSettingsPanel(isLocationFieldNeeded).apply { background = MAIN_BG_COLOR }
  private val content = ContentPanel()

  var errorState: ErrorState = ErrorState.NothingSelected
  var course: Course? = null

  protected open val startButtonText: String
    get() = EduCoreBundle.message("course.dialog.start.button")

  protected open val openButtonText: String
    get() = EduCoreBundle.message("course.dialog.start.button")

  val locationString: String?
    get() = settingsPanel.locationString

  val projectSettings: Any?
    get() = settingsPanel.getProjectSettings()

  val languageSettings: LanguageSettings<*>?
    get() = settingsPanel.languageSettings

  init {
    layout = CardLayout()
    border = JBUI.Borders.customLine(DIVIDER_COLOR, 0, 0, 0, 0)

    layoutComponents()
    @Suppress("LeakingThis")
    setButtonsEnabled(canStartCourse())
  }

  private fun layoutComponents() {
    val emptyStatePanel = JBPanelWithEmptyText().withEmptyText(EduCoreBundle.message("course.dialog.no.course.selected"))
    add(emptyStatePanel, EMPTY)

    with(content) {
      add(tagsPanel)
      add(titlePanel)
      add(authorsPanel)
      add(errorComponent)
      add(buttonsPanel)
      add(courseDetailsPanel)
      add(settingsPanel)
    }

    val scrollPane = JBScrollPane(content, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                                  ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER).apply {
      border = null
    }

    add(scrollPane, CONTENT)
  }

  protected abstract fun joinCourseAction(info: CourseInfo, mode: CourseMode)

  fun doValidation() {
    setError(getErrorState(course) { validateSettings(it) })
    setButtonsEnabled(errorState.courseCanBeStarted)
  }

  fun validateSettings(it: Course) = settingsPanel.validateSettings(it)

  fun bindCourse(course: Course, settings: CourseDisplaySettings = CourseDisplaySettings()): LanguageSettings<*>? {
    (layout as CardLayout).show(this, CONTENT)
    this.course = course
    val courseInfo = CourseInfo(course, { settingsPanel.locationString }, { settingsPanel.languageSettings })

    doValidation()

    content.update(courseInfo, settings)

    revalidate()
    repaint()

    return settingsPanel.languageSettings
  }

  fun showEmptyState() {
    (layout as CardLayout).show(this, EMPTY)
  }

  fun setError(errorState: ErrorState) {
    this.errorState = errorState
    setButtonsEnabled(errorState.courseCanBeStarted)
    buttonsPanel.setButtonToolTip(null)
    hideErrorPanel()

    showError(errorState)
  }

  private fun setError(message: ValidationMessage) {
    errorComponent.setErrorMessage(message)
    buttonsPanel.setButtonToolTip(message.beforeLink + message.linkText + message.afterLink)
  }

  protected open fun showError(errorState: ErrorState) {
    if (errorState is ErrorState.LocationError) {
      addOneTimeLocationFieldValidation()
    }

    val message = errorState.message ?: return
    when (errorState) {
      is ErrorState.JetBrainsAcademyLoginNeeded -> {
        errorComponent.setErrorMessage(message)
        buttonsPanel.setButtonToolTip(EduCoreBundle.message("course.dialog.login.required"))
      }
      is ErrorState.LoginRequired -> {
        course?.let {
          if (CoursesStorage.getInstance().hasCourse(it)) {
            return
          }
        }
        setError(message)
      }
      else -> {
        setError(message)
      }
    }
    showErrorPanel()
  }

  private fun addOneTimeLocationFieldValidation() {
    settingsPanel.addLocationFieldDocumentListener(object : DocumentAdapter() {
      override fun textChanged(e: DocumentEvent) {
        doValidation()
        settingsPanel.removeLocationFieldDocumentListener(this)
      }
    })
  }

  private fun showErrorPanel() {
    errorComponent.isVisible = true
    revalidate()
    repaint()
  }

  fun hideErrorPanel() {
    errorComponent.isVisible = false
    revalidate()
    repaint()
  }

  fun setButtonsEnabled(canStartCourse: Boolean) {
    buttonsPanel.setButtonsEnabled(canStartCourse)
  }

  fun canStartCourse(): Boolean = errorState.courseCanBeStarted

  private fun joinCourse(courseInfo: CourseInfo, courseMode: CourseMode) {
    val currentLocation = courseInfo.location()
    val locationErrorState = when {
      // if it's null it means there's no location field and it's ok
      currentLocation == null -> ErrorState.None
      currentLocation.isEmpty() -> ErrorState.EmptyLocation
      !FileUtil.ensureCanCreateFile(File(FileUtil.toSystemDependentName(currentLocation))) -> ErrorState.InvalidLocation
      else -> ErrorState.None
    }
    if (locationErrorState != ErrorState.None) {
      setError(locationErrorState)
    }
    else {
      joinCourseAction(courseInfo, courseMode)
    }
  }

  @VisibleForTesting
  inner class ButtonsPanel() : NonOpaquePanel(), CourseSelectionListener {
    @VisibleForTesting
    val buttons: List<CourseButtonBase> = mutableListOf(
      StartCourseButton(joinCourse = { courseInfo, courseMode -> joinCourse(courseInfo, courseMode) }),
      OpenCourseButton(),
      EditCourseButton { courseInfo, courseMode -> joinCourse(courseInfo, courseMode) }
    )

    init {
      layout = FlowLayout(FlowLayout.LEFT, 0, 0)
      border = JBUI.Borders.empty(17, HORIZONTAL_MARGIN - DEFAULT_BUTTON_OFFSET, 0, 0)
      buttons.forEach {
        add(it)
      }
    }

    fun setStartButtonText(text: String) {
      buttons.first().text = text
    }

    fun setOpenButtonText(text: String) {
      buttons[1].text = text
    }

    override fun onCourseSelectionChanged(courseInfo: CourseInfo, courseDisplaySettings: CourseDisplaySettings) {
      buttons.forEach {
        it.update(courseInfo)
      }
    }

    fun setButtonToolTip(tooltip: String?) {
      buttons.forEach {
        it.toolTipText = tooltip
      }
    }

    fun setButtonsEnabled(isEnabled: Boolean) {
      buttons.forEach {
        it.isEnabled = isEnabled
      }
    }
  }

  companion object {
    // default divider's color too dark in Darcula, so use the same color as in plugins dialog
    val DIVIDER_COLOR = JBColor(0xC5C5C5, 0x515151)
  }

  private class ContentPanel : NonOpaquePanel() {
    init {
      layout = VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 0, true, false)
      background = MAIN_BG_COLOR
      border = JBUI.Borders.emptyRight(HORIZONTAL_MARGIN)
    }

    override fun add(comp: Component?): Component {
      if (comp !is CourseSelectionListener) {
        error("Content of this panel is updatable, so component must implement `Updatable`")
      }
      return super.add(comp)
    }

    fun update(courseInfo: CourseInfo, settings: CourseDisplaySettings) {
      // we have to update settings prior to buttons
      components.forEach {
        (it as CourseSelectionListener).onCourseSelectionChanged(courseInfo, settings)
      }
    }
  }
}

