package com.jetbrains.edu.learning.marketplace

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.util.io.origin
import com.jetbrains.edu.learning.EduNames.EDU_PREFIX
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.authUtils.OAuthRestService
import com.jetbrains.edu.learning.authUtils.createResponse
import com.jetbrains.edu.learning.authUtils.hasOpenDialogs
import com.jetbrains.edu.learning.authUtils.sendPluginInfoResponse
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.courseGeneration.ProjectOpener
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.marketplace.courseGeneration.MarketplaceOpenCourseRequest
import com.jetbrains.edu.learning.marketplace.courseGeneration.MarketplaceOpenInIdeRequestHandler
import com.jetbrains.edu.learning.messages.EduCoreBundle
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.*
import org.jetbrains.io.send
import java.lang.reflect.InvocationTargetException
import java.util.regex.Pattern

class MarketplaceRestService : OAuthRestService(MARKETPLACE) {
  override fun getServiceName(): String = EDU_MARKETPLACE_SERVICE_NAME

  @Throws(InterruptedException::class, InvocationTargetException::class)
  override fun isHostTrusted(request: FullHttpRequest, urlDecoder: QueryStringDecoder): Boolean {
    val uri = request.uri()
    val isOauthCodeRequest = getStringParameter(CODE, urlDecoder) != null
    val isOpenCourseRequest = getIntParameter(COURSE_ID, urlDecoder) != -1
    val isErrorRequest = getStringParameter(ERROR, urlDecoder) != null
    val isPluginInfoRequest = uri.contains(INFO)
    return if (request.method() === HttpMethod.GET && (isOauthCodeRequest || isOpenCourseRequest || isErrorRequest || isPluginInfoRequest)) {
      true
    }
    else super.isHostTrusted(request, urlDecoder)
  }

  override fun execute(urlDecoder: QueryStringDecoder, request: FullHttpRequest, context: ChannelHandlerContext): String? {
    val uri = urlDecoder.uri()
    if (uri.contains(INFO)) {
      sendPluginInfoResponse(request, context)
      return null
    }

    val code = getStringParameter(CODE, urlDecoder)
    if (code != null) {
      val success = MarketplaceConnector.getInstance().login(code)
      if (success) {
        LOG.info("$myPlatformName: OAuth code is handled")
        val pageContent = GeneratorUtils.getInternalTemplateText("marketplace.redirectPage.html")
        createResponse(pageContent).send(context.channel(), request)
        return null
      }
      return sendErrorResponse(request, context, "Failed to login using provided code")
    }

    if (hasOpenDialogs(MARKETPLACE)) {
      sendOk(request, context)
      return null
    }

    val courseId = getIntParameter(COURSE_ID, urlDecoder)
    if (courseId != -1) {
      openInIDE(MarketplaceOpenCourseRequest(courseId), request, context)
    }

    sendStatus(HttpResponseStatus.BAD_REQUEST, false, context.channel())
    return "Unknown command: $uri"
  }

  private fun openInIDE(openCourseRequest: MarketplaceOpenCourseRequest,
                        request: FullHttpRequest,
                        context: ChannelHandlerContext): String? {
    LOG.info("Opening $MARKETPLACE course: $openCourseRequest")
    return when (val result = ProjectOpener.getInstance().open(MarketplaceOpenInIdeRequestHandler, openCourseRequest)) {
      is Ok -> {
        sendOk(request, context)
        LOG.info("$MARKETPLACE course opened: $openCourseRequest")
        null
      }
      is Err -> {
        val message = result.error
        LOG.warn(message)
        Notification("EduTools", EduCoreBundle.message("notification.title.failed.to.open.in.ide", openCourseRequest), message,
                     NotificationType.WARNING).notify(null)
        sendStatus(HttpResponseStatus.NOT_FOUND, false, context.channel())
        message
      }
    }
  }

  override fun isOriginAllowed(request: HttpRequest): OriginCheckResult {
    val originAllowed = super.isOriginAllowed(request)
    if (originAllowed == OriginCheckResult.FORBID) {
      val origin = request.origin ?: return OriginCheckResult.FORBID
      return if (TRUSTED_ORIGINS.contains(origin) || JETBRAINS_ORIGIN_PATTERN.matcher(origin).matches()) OriginCheckResult.ALLOW
      else OriginCheckResult.ASK_CONFIRMATION
    }
    return originAllowed
  }

  companion object {
    private const val COURSE_ID = "course_id"
    private const val CODE = "code"
    private const val ERROR = "error"
    private const val INFO = "info"
    private const val EDU_MARKETPLACE_SERVICE_NAME = "$EDU_PREFIX/marketplace"
    private val JETBRAINS_ORIGIN_PATTERN = Pattern.compile("https://([a-z0-9-]+\\.)*jetbrains.com$")
    private val TRUSTED_ORIGINS = setOf(PLUGINS_REPOSITORY_URL, PLUGINS_EDU_DEMO, PLUGINS_MASTER_DEMO)
  }
}