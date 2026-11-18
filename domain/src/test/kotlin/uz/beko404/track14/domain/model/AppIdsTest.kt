package uz.beko404.track14.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppIdsTest {
    @Test
    fun `plain firestore ids are accepted`() {
        assertTrue(AppIds.isValid("aB3_x-9"))
        assertTrue(AppIds.isValid("a".repeat(64)))
    }

    @Test
    fun `paths, blanks and long ids are rejected`() {
        assertFalse(AppIds.isValid(null))
        assertFalse(AppIds.isValid(""))
        assertFalse(AppIds.isValid("apps/x"))
        assertFalse(AppIds.isValid("../wallets"))
        assertFalse(AppIds.isValid("a b"))
        assertFalse(AppIds.isValid("a".repeat(65)))
    }
}
