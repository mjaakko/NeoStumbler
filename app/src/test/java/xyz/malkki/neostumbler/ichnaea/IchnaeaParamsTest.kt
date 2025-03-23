package xyz.malkki.neostumbler.ichnaea

import org.junit.Assert.assertEquals
import org.junit.Test

class IchnaeaParamsTest {
    @Test
    fun `Test creating an URL`() {
        val params =
            IchnaeaParams(
                baseUrl = "http://example.com",
                submissionPath = "/geosubmit",
                locatePath = null,
                apiKey = null,
            )

        assertEquals("http://example.com/geosubmit", params.toSubmissionUrl().toString())
    }

    @Test
    fun `Test creating an URL when both base URL and path contain a slash`() {
        val params =
            IchnaeaParams(
                baseUrl = "https://example.com/api/",
                submissionPath = "/v2/geosubmit",
                locatePath = null,
                apiKey = null,
            )

        assertEquals("https://example.com/api/v2/geosubmit", params.toSubmissionUrl().toString())
    }
}
