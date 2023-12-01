package xyz.malkki.neostumbler.utils

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test


class UserAgentInterceptorTest {
    private lateinit var mockServer: MockWebServer

    @Before
    fun setup() {
        mockServer = MockWebServer()
        mockServer.enqueue(MockResponse().setBody("test"))

        mockServer.start()
    }

    @After
    fun shutdown() {
        mockServer.shutdown()
    }

    @Test
    fun `Test user agent is added to the request`() {
        val userAgent = "test/1.0"

        val userAgentInterceptor = UserAgentInterceptor(userAgent)

        val httpClient = OkHttpClient.Builder().addInterceptor(userAgentInterceptor).build()

        httpClient.newCall(Request.Builder().url(mockServer.url("/")).build()).execute().close()

        assertEquals(userAgent, mockServer.takeRequest().headers["User-Agent"])
    }
}