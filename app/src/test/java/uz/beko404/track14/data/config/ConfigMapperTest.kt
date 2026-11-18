package uz.beko404.track14.data.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigMapperTest {
    @Test
    fun `version keeps latest at least min`() {
        assertEquals(1L, versionFrom(null).minBuild)
        val v = versionFrom(mapOf("minBuild" to 7L, "latestBuild" to 3L))
        assertEquals(7L, v.minBuild)
        assertEquals(7L, v.latestBuild)
    }

    @Test
    fun `flags default to open`() {
        val f = flagsFrom(null)
        assertFalse(f.maintenance)
        assertTrue(f.newApps)
        assertTrue(flagsFrom(mapOf("maintenance" to true)).maintenance)
        assertFalse(flagsFrom(mapOf("purchases" to false)).purchases)
        assertFalse(flagsFrom(mapOf("maintenance" to "true")).maintenance)
    }
}
