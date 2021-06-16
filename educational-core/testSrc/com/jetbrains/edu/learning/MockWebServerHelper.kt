package com.jetbrains.edu.learning

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.ThreadTracker
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.assertEquals

typealias ResponseHandler = (RecordedRequest) -> MockResponse?

class MockWebServerHelper(parentDisposable: Disposable) {

  private val handlers = mutableSetOf<ResponseHandler>()
  val webSocketMockSever = MockWebServer()

  private val mockWebServer = MockWebServer().apply {
    dispatcher = object : Dispatcher() {
      override fun dispatch(request: RecordedRequest): MockResponse {
        if (expectEduToolsUserAgent(request)) {
          assertEquals(eduToolsUserAgent, request.getHeader(USER_AGENT))
        }
        for (handler in handlers) {
          val response = handler(request)
          if (response != null) return response
        }
        return MockResponseFactory.notFound()
      }
    }
  }

  init {
    Disposer.register(parentDisposable, Disposable { mockWebServer.shutdown() })
    Disposer.register(parentDisposable, Disposable { webSocketMockSever.shutdown() })
    ThreadTracker.longRunningThreadCreated(parentDisposable, "MockWebServer", "OkHttp ConnectionPool", "Okio Watchdog")
  }

  val baseUrl: String get() = mockWebServer.url("/").toString()

  fun addResponseHandler(disposable: Disposable, handler: ResponseHandler) {
    handlers += handler
    Disposer.register(disposable, Disposable { handlers -= handler })
  }

  // DownloadUtil.downloadAtomically(), used in com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector.loadCourseStructure(),
  // sets product name as user agent, so such requests are not expected to contain eduToolsUserAgent
  private fun expectEduToolsUserAgent(request: RecordedRequest): Boolean = !request.requestUrl.url().path.contains("plugin")
}
