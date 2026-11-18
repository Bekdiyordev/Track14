package uz.beko404.track14.admin.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminRulesTest {
    @Test
    fun `roles and permissions follow admin ts`() {
        assertEquals(Role.MODERATOR, Role.fromClaim("moderator"))
        assertNull(Role.fromClaim("admin"))
        assertNull(Role.fromClaim(null))
        assertTrue(Role.SUPERADMIN.canAdjustCoins)
        assertFalse(Role.MODERATOR.canAdjustCoins)
        assertTrue(Role.MODERATOR.canBlockUsers)
        assertFalse(Role.SUPPORT.canBlockUsers)
        assertTrue(Role.SUPPORT.canSendPush)
        assertFalse(Role.MODERATOR.canSendPush)
    }

    @Test
    fun `coin adjustments`() {
        assertEquals(500, AdminRules.adjustAmount(" 500 "))
        assertEquals(-1_000, AdminRules.adjustAmount("-1 000"))
        assertNull(AdminRules.adjustAmount("0"))
        assertNull(AdminRules.adjustAmount("100001"))
        assertNull(AdminRules.adjustAmount("abc"))
        assertTrue(AdminRules.isReason("Support #12"))
        assertFalse(AdminRules.isReason("ok"))
    }

    @Test
    fun `push targets and schedule`() {
        assertEquals(listOf("abc", "def"), AdminRules.parseUids("abc, def\nabc"))
        assertNull(AdminRules.parseUids(""))
        assertNull(AdminRules.parseUids("abc, ../x"))
        val now = 1_000_000_000L
        assertTrue(AdminRules.isValidSendAt(null, now))
        assertTrue(AdminRules.isValidSendAt(now + 3_600_000, now))
        assertFalse(AdminRules.isValidSendAt(now - 3_600_000, now))
        assertFalse(AdminRules.isValidSendAt(now + 31L * 86_400_000, now))
    }

    @Test
    fun `config values`() {
        assertTrue(AdminRules.isValidVersion(3, 5))
        assertFalse(AdminRules.isValidVersion(5, 3))
        assertFalse(AdminRules.isValidVersion(null, 3))
        assertTrue(AdminRules.isTelegram("https://t.me/track14"))
        assertFalse(AdminRules.isTelegram("https://evil.io/t.me"))
        assertTrue(AdminRules.isGroupUrl(""))
        assertTrue(AdminRules.isEmail("help@track14.uz"))
        assertFalse(AdminRules.isEmail("help@"))
        assertEquals(mapOf("standardFee" to 900), AdminRules.parseEconomy(mapOf("standardFee" to "900", "proFee" to "")))
        assertNull(AdminRules.parseEconomy(mapOf("standardFee" to "-1")))
        assertNull(AdminRules.parseEconomy(mapOf("maxActiveTests" to "0")))
    }
}
