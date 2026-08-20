package com.rumd3x.trilhossp.config

import kotlin.test.Test
import kotlin.test.assertEquals

class TransitApiPropertiesTest {

    private fun props(key: String) = TransitApiProperties(baseUrl = "https://example.com", key = key)

    @Test fun `normalizedKey returns key as-is when prefix already present`() =
        assertEquals("cci_metro_status_live_abc123", props("cci_metro_status_live_abc123").normalizedKey())

    @Test fun `normalizedKey prepends prefix when missing`() =
        assertEquals("cci_metro_status_live_abc123", props("abc123").normalizedKey())

    @Test fun `normalizedKey does not double-prefix`() {
        val fullKey = "cci_metro_status_live_cci_metro_status_live_abc"
        assertEquals(fullKey, props(fullKey).normalizedKey())
    }
}
