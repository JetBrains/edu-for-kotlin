package com.jetbrains.edu.learning.codeforces.run

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.runConfigurationType
import icons.EducationalCoreIcons
import org.jetbrains.annotations.NonNls
import javax.swing.Icon

/**
 * id has to be one of these values
 * @see com.intellij.execution.InputRedirectAware.TYPES_WITH_REDIRECT_AWARE_UI
 * in other cases, Java configuration will not work
 */
class CodeforcesRunConfigurationType : ConfigurationType {
  override fun getId(): String = "Application"

  override fun getIcon(): Icon = EducationalCoreIcons.Codeforces

  override fun getConfigurationTypeDescription(): String = CONFIGURATION_ID

  override fun getDisplayName(): String = CONFIGURATION_ID

  override fun getConfigurationFactories(): Array<ConfigurationFactory> {
    return arrayOf(CodeforcesRunConfigurationFactory(this))
  }

  companion object {
    @NonNls
    const val CONFIGURATION_ID = "Codeforces"

    fun getInstance(): CodeforcesRunConfigurationType {
      return runConfigurationType()
    }
  }
}
