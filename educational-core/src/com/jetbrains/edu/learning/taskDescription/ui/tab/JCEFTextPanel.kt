package com.jetbrains.edu.learning.taskDescription.ui.tab

import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JCEFHtmlPanel
import com.jetbrains.edu.learning.taskDescription.ui.*
import org.jsoup.nodes.Element
import java.awt.BorderLayout
import javax.swing.JComponent


class JCEFTextPanel(project: Project) : TabTextPanel(project) {
  private val jcefBrowser = JCEFHtmlPanel(JBCefApp.getInstance().createClient(), null)

  override val component: JComponent
    get() = jcefBrowser.component

  init {
    // BACKCOMPAT: 2020.3: error page is disabled by default in 211 branch
    jcefBrowser.disableErrorPage()

    val toolWindowLinkHandler = JCefToolWindowLinkHandler(project)
    val requestHandler = JCEFToolWindowRequestHandler(toolWindowLinkHandler)
    jcefBrowser.jbCefClient.addRequestHandler(requestHandler, jcefBrowser.cefBrowser)
    val lifeSpanHandler = JCEFTaskInfoLifeSpanHandler(toolWindowLinkHandler)
    jcefBrowser.jbCefClient.addLifeSpanHandler(lifeSpanHandler, jcefBrowser.cefBrowser)
    add(jcefBrowser.component, BorderLayout.CENTER)

    Disposer.register(this, jcefBrowser)
    ApplicationManager.getApplication().messageBus.connect(this)
      .subscribe(LafManagerListener.TOPIC,
                 LafManagerListener {
                   TaskDescriptionView.updateAllTabs(project)
                 })
  }

  override fun setText(text: String) {
    jcefBrowser.loadHTML(text)
  }

  override fun wrapHint(hintElement: Element, displayedHintNumber: String): String {
    return JCEFToolWindow.wrapHint(project, hintElement, displayedHintNumber)
  }
}