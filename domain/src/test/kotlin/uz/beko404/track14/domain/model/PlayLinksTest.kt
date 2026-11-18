package uz.beko404.track14.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayLinksTest {
    @Test
    fun `package names`() {
        assertTrue(PlayLinks.isValidPackageName("uz.beko404.track14"))
        assertFalse(PlayLinks.isValidPackageName("track14"))
        assertFalse(PlayLinks.isValidPackageName("uz..x"))
        assertFalse(PlayLinks.isValidPackageName("uz.x;rm"))
        assertFalse(PlayLinks.isValidPackageName(null))
    }

    @Test
    fun `only play testing links are accepted`() {
        assertTrue(PlayLinks.isValidOptInUrl("https://play.google.com/apps/testing/uz.app.lugat"))
        assertTrue(PlayLinks.isValidOptInUrl("https://play.google.com/apps/testing/uz.app.lugat/"))
        assertFalse(PlayLinks.isValidOptInUrl("http://play.google.com/apps/testing/uz.app.lugat"))
        assertFalse(PlayLinks.isValidOptInUrl("https://play.google.com.evil.io/apps/testing/uz.a.b"))
        assertFalse(PlayLinks.isValidOptInUrl("https://evil.io/?https://play.google.com/apps/testing/uz.a.b"))
        assertFalse(PlayLinks.isValidOptInUrl("https://play.google.com/apps/testing/"))
    }

    @Test
    fun `store url`() {
        assertEquals(
            "https://play.google.com/store/apps/details?id=uz.app.lugat",
            PlayLinks.storeUrl("uz.app.lugat"),
        )
    }
}
