package uz.beko404.track14.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GateRulesTest {
    private val version = VersionConfig(minBuild = 5, latestBuild = 8, message = "Yangilang")

    @Test
    fun `update levels`() {
        assertEquals(UpdateState.REQUIRED, GateRules.update(4, version))
        assertEquals(UpdateState.OPTIONAL, GateRules.update(5, version))
        assertEquals(UpdateState.NONE, GateRules.update(8, version))
    }

    @Test
    fun `gate priority - update, maintenance, blocked, open`() {
        val off = FeatureFlags()
        val on = FeatureFlags(maintenance = true, maintenanceMessage = "Soat 14:00 gacha")
        assertEquals(GateState.UpdateRequired("Yangilang"), GateRules.gate(4, version, on, "spam"))
        assertEquals(GateState.Maintenance("Soat 14:00 gacha"), GateRules.gate(6, version, on, "spam"))
        assertEquals(GateState.Blocked("spam"), GateRules.gate(6, version, off, "spam"))
        assertEquals(GateState.Open(optionalUpdate = true), GateRules.gate(6, version, off, null))
        assertEquals(GateState.Open(optionalUpdate = false), GateRules.gate(9, version, off, null))
    }

    @Test
    fun `support links are filtered`() {
        val s = SupportConfig(telegram = "https://t.me/track14", email = "help@track14.uz", groupUrl = "https://evil.io", faqUrl = "http://x")
        assertEquals("https://t.me/track14", s.safeTelegram)
        assertEquals("help@track14.uz", s.safeEmail)
        assertNull(s.safeGroupUrl)
        assertNull(s.safeFaqUrl)
        assertNull(SupportConfig(email = "a@b").safeEmail)
    }
}
