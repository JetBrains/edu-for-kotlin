package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.wm.IdeFrame
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.AppIcon
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.authUtils.OAuthRestService
import com.jetbrains.edu.learning.stepik.builtInServer.EduBuiltInServerUtils
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.*
import org.jetbrains.ide.RestService
import org.jetbrains.io.send
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.util.regex.Pattern

class HyperskillRestService : OAuthRestService(HYPERSKILL) {
  override fun getServiceName(): String = EDU_HYPERSKILL_SERVICE_NAME

  @Throws(InterruptedException::class, InvocationTargetException::class)
  override fun isHostTrusted(request: FullHttpRequest): Boolean {
    val uri = request.uri()
    val codeMatcher = OAUTH_CODE_PATTERN.matcher(uri)
    val openCourseMatcher = OPEN_COURSE_PATTERN.matcher(uri)
    return if (request.method() === HttpMethod.GET && (codeMatcher.matches() || openCourseMatcher.matches())) {
      true
    }
    else super.isHostTrusted(request)
  }

  @Throws(IOException::class)
  override fun execute(decoder: QueryStringDecoder,
                       request: FullHttpRequest,
                       context: ChannelHandlerContext): String? {
    val uri = decoder.uri()
    val matcher = OPEN_COURSE_PATTERN.matcher(uri)
    if (matcher.matches()) {
      val account = HyperskillSettings.INSTANCE.account
      if (account == null) {
        HyperskillConnector.doAuthorize(Runnable { openProject(decoder, request, context) })
      }
      else {
        return openProject(decoder, request, context)
      }
    }

    if (OAUTH_CODE_PATTERN.matcher(uri).matches()) {
      val code = RestService.getStringParameter("code", decoder)!! // cannot be null because of pattern

      val success = HyperskillConnector.login(code)
      if (success) {
        RestService.LOG.info("$myPlatformName: OAuth code is handled")
        val pageContent = FileTemplateManager.getDefaultInstance().getInternalTemplate("hyperskill.redirectPage.html").text
        createResponse(pageContent).send(context.channel(), request)
        return null
      }
      return sendErrorResponse(request, context, "Failed to login using provided code")
    }

    RestService.sendStatus(HttpResponseStatus.BAD_REQUEST, false, context.channel())
    return "Unknown command: $uri"
  }

  private fun openProject(decoder: QueryStringDecoder, request: FullHttpRequest, context: ChannelHandlerContext): String? {
    val stageId = RestService.getStringParameter("stage_id", decoder)?.toInt() ?: return "The stage_id parameter was not found"
    val projectId = RestService.getStringParameter("project_id", decoder)?.toInt() ?: return "The project_id parameter was not found"
    LOG.info("Opening a stage $stageId from project $projectId")

    if (focusOpenProject(projectId, stageId) || openRecentProject(projectId, stageId) || createProject(projectId, stageId)) {
      RestService.sendOk(request, context)
      LOG.info("Hyperskill project opened: $projectId")
      return null
    }
    RestService.sendStatus(HttpResponseStatus.NOT_FOUND, false, context.channel())
    val message = "A project wasn't found or created"
    LOG.info(message)
    return message
  }

  private fun focusOpenProject(courseId: Int, stageId: Int): Boolean {
    val (project, course) = EduBuiltInServerUtils.focusOpenProject { it is HyperskillCourse && it.hyperskillProject.id == courseId } ?: return false
    course.putUserData(HYPERSKILL_STAGE, stageId)
    ApplicationManager.getApplication().invokeLater { openSelectedStage(course, project) }
    return true
  }

  private fun openRecentProject(courseId: Int, stageId: Int): Boolean {
    val (_, course) = EduBuiltInServerUtils.openRecentProject { it is HyperskillCourse && it.hyperskillProject.id == courseId } ?: return false
    course?.putUserData(HYPERSKILL_STAGE, stageId)
    return true
  }

  private fun createProject(projectId: Int, stageId: Int): Boolean {
    runInEdt {
      requestFocus()

      val hyperskillCourse = ProgressManager.getInstance().run(object : Task.WithResult<HyperskillCourse?, Exception>
                                                                        (null, "Loading project", true) {
        override fun compute(indicator: ProgressIndicator): HyperskillCourse? {
          val hyperskillProject = HyperskillConnector.getProject(projectId) ?: return null
          val stages = HyperskillConnector.getStages(projectId) ?: return null
          if (!hyperskillProject.useIde) {
            LOG.warn("Project in not supported yet $projectId")
            Notification(HYPERSKILL, HYPERSKILL, HYPERSKILL_PROJECT_NOT_SUPPORTED, NotificationType.WARNING,
                         HSHyperlinkListener(false)).notify(project)
            return null
          }
          val languageId = EduNames.JAVA
          val hyperskillCourse = HyperskillCourse(hyperskillProject, languageId)
          hyperskillCourse.stages = stages
          return hyperskillCourse
        }
      }) ?: return@runInEdt

      hyperskillCourse.putUserData(HYPERSKILL_STAGE, stageId)
      HyperskillJoinCourseDialog(hyperskillCourse).show()
    }
    return true
  }

  // We have to use visible frame here because project is not yet created
  // See `com.intellij.ide.impl.ProjectUtil.focusProjectWindow` implementation for more details
  private fun requestFocus() {
    val frame = WindowManager.getInstance().findVisibleFrame()
    if (frame is IdeFrame) {
      AppIcon.getInstance().requestFocus(frame)
    }
    frame.toFront()
  }

  companion object {
    private const val EDU_HYPERSKILL_SERVICE_NAME = "edu/hyperskill"
    private val OAUTH_CODE_PATTERN = Pattern.compile("/api/$EDU_HYPERSKILL_SERVICE_NAME/oauth\\?code=(\\w+)")
    private val OPEN_COURSE_PATTERN = Pattern.compile("/api/$EDU_HYPERSKILL_SERVICE_NAME\\?stage_id=.+&project_id=.+")
  }

  override fun isAccessible(request: HttpRequest): Boolean {
    return true
  }
}
