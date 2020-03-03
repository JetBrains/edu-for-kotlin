package com.jetbrains.edu.learning.newproject.ui

import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.OnePixelDivider
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.openapi.updateSettings.impl.pluginsAdvertisement.PluginsAdvertiser
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.*
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBList
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.messages.MessageBusConnection
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.actions.ImportLocalCourseAction.Companion.importLocation
import com.jetbrains.edu.learning.actions.ImportLocalCourseAction.Companion.saveLastImportLocation
import com.jetbrains.edu.learning.actions.ImportLocalCourseAction.Companion.showInvalidCourseDialog
import com.jetbrains.edu.learning.actions.ImportLocalCourseAction.Companion.showUnsupportedCourseDialog
import com.jetbrains.edu.learning.checkio.CheckiOConnectorProvider
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.getDecoratedLogo
import com.jetbrains.edu.learning.courseFormat.ext.tooltipText
import com.jetbrains.edu.learning.courseLoading.CourseLoader.getCourseInfosUnderProgress
import com.jetbrains.edu.learning.newproject.LocalCourseFileChooser
import com.jetbrains.edu.learning.newproject.ui.ErrorState.*
import com.jetbrains.edu.learning.newproject.ui.ErrorState.Companion.forCourse
import com.jetbrains.edu.learning.newproject.ui.coursePanel.NewCoursePanel
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector.importCourseArchive
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector.loggedIn
import com.jetbrains.edu.learning.stepik.HYPERSKILL
import com.jetbrains.edu.learning.stepik.StepikAuthorizer
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.course.StartStepikCourseAction
import com.jetbrains.edu.learning.stepik.course.StepikCourse
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector.Companion.getInstance
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView.Companion.getTaskDescriptionBackgroundColor
import org.jetbrains.annotations.NonNls
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Point
import java.util.*
import javax.swing.*

class CoursesPanel(courses: List<Course>,
                   dialog: BrowseCoursesDialog,
                   private val customToolbarActions: DefaultActionGroup?,
                   private val enableCourseViewAsEducator: (Boolean) -> Unit) : JPanel(BorderLayout()) {
  private val myErrorPanel: JPanel = JPanel(BorderLayout())
  private val myMainPanel: JPanel = JPanel(BorderLayout())
  private val myErrorLabel: HyperlinkLabel = HyperlinkLabel()
  private val myCourseListPanel: JPanel = JPanel(BorderLayout())
  private val mySearchField: FilterComponent
  private val mySplitPane: JSplitPane = JSplitPane()
  private val mySplitPaneRoot: JPanel = JPanel(BorderLayout())
  private var myCoursePanel: NewCoursePanel = NewCoursePanel(isStandalonePanel = false, isLocationFieldNeeded = true) {
    dialog.setError(it.error)
  }
  private val myContentPanel: JPanel = JPanel(BorderLayout())
  private var myCourses: MutableList<Course>
  private var myCoursesComparator: Comparator<Course>
  private val myListeners: MutableList<CourseValidationListener> = ArrayList()
  private var myBusConnection: MessageBusConnection? = null
  private var myErrorState: ErrorState? = NothingSelected
  private var myCoursesList: JBList<Course> = JBList()
  var selectedCourse: Course
  val projectSettings: Any
    get() {
      return myCoursePanel.projectSettings()
    }


  private fun initUI() {
    GuiUtils.replaceJSplitPaneWithIDEASplitter(mySplitPaneRoot, true)
    mySplitPane.setDividerLocation(0.5)
    mySplitPane.resizeWeight = 0.5
    myErrorLabel.isVisible = false
    myCoursesList.setEmptyText(NO_COURSES)
    updateModel(myCourses, null)
    val renderer = courseRenderer
    myCoursesList.cellRenderer = renderer
    myCoursesList.addListSelectionListener { processSelectionChanged() }
    val toolbarDecorator = ToolbarDecorator.createDecorator(myCoursesList)
      .disableAddAction()
      .disableRemoveAction()
      .disableUpDownActions()
      .setToolbarPosition(ActionToolbarPosition.BOTTOM)
    myCoursesList.border = null
    myCoursesList.background = getTaskDescriptionBackgroundColor()
    val group = customToolbarActions ?: DefaultActionGroup(ImportCourseAction())
    toolbarDecorator.setActionGroup(group)
    val toolbarDecoratorPanel = toolbarDecorator.createPanel()
    toolbarDecoratorPanel.border = null
    myCourseListPanel.add(toolbarDecoratorPanel, BorderLayout.CENTER)
    myCourseListPanel.border = JBUI.Borders.customLine(OnePixelDivider.BACKGROUND, 1, 1, 1, 1)
    addErrorStateListener()

    processSelectionChanged()
  }

  private fun addErrorStateListener() {
    myErrorLabel.addHyperlinkListener {
      if (myErrorState === NotLoggedIn || myErrorState === StepikLoginRequired) {
        addLoginListener(Runnable { updateCoursesList() })
        StepikAuthorizer.doAuthorize { EduUtils.showOAuthDialog() }
        loggedIn(StepikNames.STEPIK,
                 EduCounterUsageCollector.AuthorizationPlace.START_COURSE_DIALOG)
      }
      else if (myErrorState is CheckiOLoginRequired) {
        val course = myCoursesList.selectedValue as CheckiOCourse
        addCheckiOLoginListener(course)

        //for Checkio course name matches platform name
        loggedIn(course.name,
                 EduCounterUsageCollector.AuthorizationPlace.START_COURSE_DIALOG)
      }
      else if (myErrorState === HyperskillLoginRequired) {
        addHyperskillLoginListener()
        loggedIn(HYPERSKILL,
                 EduCounterUsageCollector.AuthorizationPlace.START_COURSE_DIALOG)
      }
      else if (myErrorState === JavaFXRequired) {
        invokeSwitchBootJdk()
      }
      else if (myErrorState === IncompatibleVersion) {
        PluginsAdvertiser.installAndEnablePlugins(
          setOf(EduNames.PLUGIN_ID)) {}
      }
      else if (myErrorState is RequiredPluginsDisabled) {
        val disabledPluginIds = (myErrorState as RequiredPluginsDisabled).disabledPluginIds
        enablePlugins(disabledPluginIds)
      }
      else if (myErrorState is CustomSevereError) {
        val action = (myErrorState as CustomSevereError).action
        action?.run()
      }
      else if (myErrorState != null) {
        browseHyperlink(myErrorState!!.message)
      }
    }
  }

  private fun invokeSwitchBootJdk() {
    val switchBootJdkId = "SwitchBootJdk"
    val action = ActionManager.getInstance().getAction(
      switchBootJdkId)
    if (action == null) {
      LOG.error("$switchBootJdkId action not found")
      return
    }
    action.actionPerformed(
      AnActionEvent.createFromAnAction(action, null,
                                       ActionPlaces.UNKNOWN,
                                       DataManager.getInstance().getDataContext(this))
    )
  }

  private fun addCheckiOLoginListener(selectedCourse: CheckiOCourse) {
    val checkiOConnectorProvider = (selectedCourse.configurator as CheckiOConnectorProvider?)!!
    val checkiOOAuthConnector = checkiOConnectorProvider.oAuthConnector
    checkiOOAuthConnector.doAuthorize(
      Runnable { myErrorLabel.isVisible = false },
      Runnable { doValidation(selectedCourse) }
    )
  }

  private fun addHyperskillLoginListener() {
    getInstance().doAuthorize(
      Runnable { myErrorLabel.isVisible = false },
      Runnable { notifyListeners(true) }
    )
  }

  private fun addLoginListener(vararg postLoginActions: Runnable) {
    if (myBusConnection != null) {
      myBusConnection!!.disconnect()
    }
    myBusConnection = ApplicationManager.getApplication().messageBus.connect()
    myBusConnection!!.subscribe(EduSettings.SETTINGS_CHANGED, object : EduLogInListener {
      override fun userLoggedOut() {}
      override fun userLoggedIn() {
        runPostLoginActions(*postLoginActions)
      }
    })
  }

  private fun runPostLoginActions(vararg postLoginActions: Runnable) {
    ApplicationManager.getApplication().invokeLater({
                                                      for (action in postLoginActions) {
                                                        action.run()
                                                      }
                                                      if (myBusConnection != null) {
                                                        myBusConnection!!.disconnect()
                                                        myBusConnection = null
                                                      }
                                                    },
                                                    ModalityState.any())
  }

  private fun updateCoursesList() {
    val selectedCourse = myCoursesList.selectedValue
    val courses = getCourseInfosUnderProgress(
      "Getting Available Courses"
    ) { CoursesProvider.loadAllCourses() }
    myCourses = courses?.toMutableList() ?: mutableListOf()
    updateModel(myCourses, selectedCourse)
    myErrorLabel.isVisible = false
    notifyListeners(true)
  }

  private val courseRenderer: ColoredListCellRenderer<Course>
    get() = object : ColoredListCellRenderer<Course>() {
      override fun customizeCellRenderer(jList: JList<out Course>,
                                         course: Course,
                                         i: Int,
                                         b: Boolean,
                                         b1: Boolean) {
        val logo = getLogo(course)
        border = JBUI.Borders.empty(5, 0)
        append(course.name, course.visibility.textAttributes)
        icon = course.getDecoratedLogo(logo)
        toolTipText = course.tooltipText
      }
    }

  private fun processSelectionChanged() {
    val course = myCoursesList.selectedValue
    if (course != null) {
      myCoursePanel.bindCourse(course).addSettingsChangeListener { doValidation(course) }
      enableCourseViewAsEducator(ApplicationManager.getApplication().isInternal ||
                                 course !is StepikCourse && course !is CheckiOCourse)
      selectedCourse = course
    }
    doValidation(course)
  }

  private fun doValidation(course: Course?) {
    var languageError: ErrorState = NothingSelected
    if (course != null) {
      val languageSettingsMessage = myCoursePanel.validateSettings(course)
      languageError = languageSettingsMessage?.let { LanguageSettingsError(it) } ?: None
    }
    val errorState = forCourse(course).merge(languageError)
    updateErrorInfo(errorState)
    notifyListeners(errorState.courseCanBeStarted)
  }

  fun updateErrorInfo(errorState: ErrorState) {
    myErrorState = errorState
    val message = errorState.message
    if (message != null) {
      myErrorLabel.isVisible = true
      myErrorLabel.setHyperlinkText(message.beforeLink, message.linkText, message.afterLink)
    }
    else {
      myErrorLabel.isVisible = false
    }
    myErrorLabel.foreground = errorState.foregroundColor
  }

  fun setCoursesComparator(comparator: Comparator<Course>) {
    myCoursesComparator = comparator
    updateModel(myCourses, null)
  }

  fun setEmptyText(text: String) {
    myCoursesList.setEmptyText(text)
  }

  private fun sortCourses(courses: List<Course>): List<Course> {
    return ContainerUtil.sorted(courses, myCoursesComparator)
  }

  private fun updateModel(courses: List<Course>, courseToSelect: Course?) {
    val sortedCourses = sortCourses(courses)
    val listModel = DefaultListModel<Course>()
    for (course in sortedCourses) {
      listModel.addElement(course)
    }
    myCoursesList.model = listModel
    if (myCoursesList.itemsCount > 0) {
      myCoursesList.setSelectedIndex(0)
    }
    else {
      myCoursePanel.hideContent()
    }
    if (courseToSelect == null) {
      return
    }
    myCourses.stream()
      .filter { course: Course -> course == courseToSelect }
      .findFirst()
      .ifPresent { newCourseToSelect: Course? -> myCoursesList.setSelectedValue(newCourseToSelect, true) }
  }

  override fun getPreferredSize(): Dimension {
    return JBUI.size(750, 400)
  }

  fun addCourseValidationListener(listener: CourseValidationListener) {
    myListeners.add(listener)
    listener.validationStatusChanged(myErrorState!!.courseCanBeStarted)
  }

  private fun notifyListeners(canStartCourse: Boolean) {
    for (listener in myListeners) {
      listener.validationStatusChanged(canStartCourse)
    }
  }

  interface CourseValidationListener {
    fun validationStatusChanged(canStartCourse: Boolean)
  }

  internal inner class ImportCourseAction : AnAction("Import Course",
                                                     "Import local or Stepik course",
                                                     AllIcons.ToolbarDecorator.Import) {
    override fun actionPerformed(e: AnActionEvent) {
      val localCourseOption = "Import local course"
      val stepikCourseOption = "Import Stepik course"
      val popupStep: BaseListPopupStep<String> = object : BaseListPopupStep<String>(
        null, listOf(localCourseOption, stepikCourseOption)) {
        override fun onChosen(selectedValue: String, finalChoice: Boolean): PopupStep<*>? {
          return doFinalStep {
            if (localCourseOption == selectedValue) {
              importLocalCourse()
            }
            else if (stepikCourseOption == selectedValue) {
              if (!EduSettings.isLoggedIn()) {
                val result = Messages.showOkCancelDialog(
                  "Stepik authorization is required to import courses", "Log in to Stepik", "Log in", "Cancel",
                  null)
                if (result == Messages.OK) {
                  addLoginListener(Runnable { updateCoursesList() },
                                   Runnable { importStepikCourse() })
                  StepikAuthorizer.doAuthorize { EduUtils.showOAuthDialog() }
                }
              }
              else {
                importStepikCourse()
              }
            }
          }
        }
      }
      val listPopup = JBPopupFactory.getInstance().createListPopup(popupStep)
      val icon = templatePresentation.icon
      val component = e.inputEvent.component
      val relativePoint = RelativePoint(component, Point(icon.iconWidth + 6, 0))
      listPopup.show(relativePoint)
    }

    private fun importLocalCourse() {
      FileChooser.chooseFile(LocalCourseFileChooser, null, importLocation()
      ) { file: VirtualFile ->
        val fileName = file.path
        val course = EduUtils.getLocalCourse(fileName)
        when {
          course == null -> {
            showInvalidCourseDialog()
          }
          course.configurator == null -> {
            showUnsupportedCourseDialog(course)
          }
          else -> {
            saveLastImportLocation(file)
            importCourseArchive()
            myCourses.add(course)
            updateModel(myCourses, course)
          }
        }
      }
    }

    private fun importStepikCourse() {
      val course = StartStepikCourseAction().importStepikCourse() ?: return
      myCourses.add(course)
      updateModel(myCourses, course)
    }
  }

  companion object {
    private val LOG = Logger.getInstance(CoursesPanel::class.java)
    private const val NO_COURSES = "No courses found"

    fun browseHyperlink(message: ValidationMessage?) {
      if (message == null) {
        return
      }
      val hyperlink = message.hyperlinkAddress
      if (hyperlink != null) {
        BrowserUtil.browse(hyperlink)
      }
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

    private fun getLogo(course: Course): Icon? {
      val language = course.languageById
      val configurator = course.configurator
      if (configurator == null) {
        LOG.info(
          String.format("configurator is null, language: %s course type: %s", language.displayName, course.itemType))
        return null
      }
      return configurator.logo
    }

    @JvmStatic
    fun getFilterParts(@NonNls filter: String): Set<String> {
      return HashSet(listOf(*filter.toLowerCase().split(" ".toRegex()).toTypedArray()))
    }
  }

  init {
    addCourseValidationListener(object : CourseValidationListener {
      override fun validationStatusChanged(canStartCourse: Boolean) {
        myCoursePanel.setButtonsEnabled(canStartCourse)
      }
    })
    myErrorPanel.add(myErrorLabel, BorderLayout.CENTER)
    myErrorPanel.border = JBUI.Borders.emptyTop(20)

    myCourses = courses.toMutableList()
    myCoursesComparator = Comparator.comparing { obj: Course -> obj.visibility }.thenComparing { obj: Course -> obj.name }

    myCoursesList.setEmptyText(NO_COURSES)
    val renderer = courseRenderer
    myCoursesList.cellRenderer = renderer
    myCoursesList.addListSelectionListener { processSelectionChanged() }
    myCoursesList.border = null
    myCoursesList.background = getTaskDescriptionBackgroundColor()
    updateModel(myCourses, null)

    mySearchField = createSearchField()

    selectedCourse = myCoursesList.selectedValue

    myCourseListPanel.add(mySearchField, BorderLayout.NORTH)

    myCoursePanel.bindCourse(selectedCourse)
    myCoursePanel.bindSearchField(mySearchField)

    mySplitPane.leftComponent = myCourseListPanel
    mySplitPane.rightComponent = myCoursePanel

    mySplitPaneRoot.add(mySplitPane, BorderLayout.CENTER)
    myContentPanel.add(mySplitPaneRoot, BorderLayout.CENTER)

    myMainPanel.add(myContentPanel, BorderLayout.CENTER)
    myMainPanel.add(myErrorPanel, BorderLayout.SOUTH)

    add(myMainPanel, BorderLayout.CENTER)
    initUI()
  }

  val locationString: String
    get() {
      // We use `myCoursePanel` with location field
      // so `myCoursePanel.getLocationString()` must return not null value
      return myCoursePanel.locationString()!!
    }

  private fun createSearchField(): FilterComponent {
    val searchField = object : FilterComponent("Edu.NewCourse", 5, true) {
      override fun filter() {
        val selectedCourse = myCoursesList.selectedValue
        val filter = filter
        val filtered: MutableList<Course> = ArrayList()
        for (course in myCourses) {
          if (accept(filter, course)) {
            filtered.add(course)
          }
        }
        updateModel(filtered, selectedCourse)
      }
    }

    UIUtil.setBackgroundRecursively(searchField, UIUtil.getTextFieldBackground())

    return searchField
  }
}