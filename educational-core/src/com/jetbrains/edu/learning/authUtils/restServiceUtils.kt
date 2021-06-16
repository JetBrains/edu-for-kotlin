package com.jetbrains.edu.learning.authUtils

import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.impl.ApplicationInfoImpl
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.io.BufferExposingByteArrayOutputStream
import com.intellij.openapi.util.io.StreamUtil
import com.intellij.openapi.wm.IdeFrame
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.AppIcon
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.getInEdt
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.pluginVersion
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpResponse
import org.jetbrains.io.addNoCache
import org.jetbrains.io.response
import org.jetbrains.io.send
import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.charset.StandardCharsets

fun hasOpenDialogs(platformName: String): Boolean = getInEdt(modalityState = ModalityState.any()) {
  if (ModalityState.current() != ModalityState.NON_MODAL) {
    requestFocus()
    Messages.showInfoMessage(EduCoreBundle.message("rest.service.modal.dialogs.message", platformName),
                             EduCoreBundle.message("rest.service.modal.dialogs.title", platformName))
    return@getInEdt true
  }
  false
}

// We have to use visible frame here because project is not yet created
// See `com.intellij.ide.impl.ProjectUtil.focusProjectWindow` implementation for more details
fun requestFocus() {
  val frame = WindowManager.getInstance().findVisibleFrame()
  if (frame is IdeFrame) {
    AppIcon.getInstance().requestFocus(frame)
  }
  frame?.toFront()
}

fun sendPluginInfoResponse(request: FullHttpRequest, context: ChannelHandlerContext) {
  val appInfo = ApplicationInfoImpl.getShadowInstance()
  createResponse(ObjectMapper().writeValueAsString(PluginInfo("${appInfo.versionName} ${appInfo.fullVersion}",
                                                              pluginVersion(EduNames.PLUGIN_ID))))
    .send(context.channel(), request)
}

@Throws(IOException::class)
fun createResponse(template: String): HttpResponse {
  BufferExposingByteArrayOutputStream().use { byteOut ->
    byteOut.write(StreamUtil.loadFromStream(ByteArrayInputStream(template.toByteArray(StandardCharsets.UTF_8))))
    val response: HttpResponse = response("text/html",
                                          Unpooled.wrappedBuffer(byteOut.internalBuffer, 0, byteOut.size()))
    response.addNoCache()
    response.headers()["X-Frame-Options"] = "Deny"
    return response
  }
}

private data class PluginInfo(val version: String?, val edutools: String?)