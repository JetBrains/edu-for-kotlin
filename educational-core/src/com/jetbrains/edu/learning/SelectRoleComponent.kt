package com.jetbrains.edu.learning

import com.intellij.ide.AppLifecycleListener
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.BaseComponent
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.util.PlatformUtils
import com.jetbrains.edu.coursecreator.actions.CCPluginToggleAction
import com.jetbrains.edu.learning.ui.SelectRolePanel

@Suppress("ComponentNotRegistered") // educational-core.xml
class SelectRoleComponent : BaseComponent {
  private val IDEA_EDU_PREFIX = "IdeaEdu" // BACKCOMPAT: 2018.3

  override fun getComponentName() = "edu.selectRole"

  override fun disposeComponent() {}

  override fun initComponent() {
    if (!PlatformUtils.isPyCharmEducational() && PlatformUtils.getPlatformPrefix() != IDEA_EDU_PREFIX) {
      PropertiesComponent.getInstance().setValue(CCPluginToggleAction.COURSE_CREATOR_ENABLED, true)
      return
    }

    if (PropertiesComponent.getInstance().isValueSet(CCPluginToggleAction.COURSE_CREATOR_ENABLED)) {
      return
    }

    val connection = ApplicationManager.getApplication().messageBus.connect()
    connection.subscribe(AppLifecycleListener.TOPIC, object : AppLifecycleListenerAdapter {
      override fun appFrameCreated() {
        showInitialConfigurationDialog()
      }
    })
  }

  private fun showInitialConfigurationDialog() {
    val dialog = DialogBuilder()
    val panel = SelectRolePanel()
    dialog.setPreferredFocusComponent(panel.getStudentButton())
    dialog.title("Are you a Learner or an Educator?").centerPanel(panel)
    dialog.addOkAction().setText("Start using EduTools")
    dialog.show()
  }
}