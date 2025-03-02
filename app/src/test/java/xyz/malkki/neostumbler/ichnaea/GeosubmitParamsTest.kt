package xyz.malkki.neostumbler.ichnaea

import org.junit.Assert.assertEquals
import org.junit.Test

class GeosubmitParamsTest {
    @Test
    fun `Test creating an URL`() {
        val params = IchnaeaParams("http://example.com", "/geosubmit", null)

        assertEquals("http://example.com/geosubmit", params.toSubmissionUrl().toString())
    }

    @Test
    fun `Test creating an URL when both base URL and path contain a slash`() {
        val params = IchnaeaParams("https://example.com/api/", "/v2/geosubmit", null)

        assertEquals("https://example.com/api/v2/geosubmit", params.toSubmissionUrl().toString())
    }
}
