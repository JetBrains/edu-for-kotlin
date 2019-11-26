package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduLogInListener
import com.jetbrains.edu.learning.LoginWidget
import com.jetbrains.edu.learning.authUtils.OAuthAccount
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import icons.EducationalCoreIcons
import javax.swing.Icon

class HyperskillWidget(project: Project) : LoginWidget(project, HyperskillConnector.AUTHORIZATION_TOPIC, HYPERSKILL) {
  override val account: OAuthAccount<out Any>? get() = HyperskillSettings.INSTANCE.account
  override val icon: Icon get() = EducationalCoreIcons.JB_ACADEMY_ENABLED
  override val disabledIcon: Icon get() = EducationalCoreIcons.JB_ACADEMY_DISABLED

  override fun ID() = "HyperskillAccountWidget"

  override fun authorize() {
    HyperskillConnector.getInstance().doAuthorize()
  }

  override fun resetAccount() {
    HyperskillSettings.INSTANCE.account = null
    project.messageBus.syncPublisher<EduLogInListener>(HyperskillConnector.AUTHORIZATION_TOPIC).userLoggedOut()
  }
}
