package uz.beko404.track14.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileModelsTest {
    @Test
    fun `reliability matches the server`() {
        assertNull(TesterStats(testsStarted = 3).reliability)
        assertEquals(75, TesterStats(testsCompleted = 3, testsFailed = 1).reliability)
        assertEquals(33, TesterStats(testsCompleted = 1, testsLeft = 2).reliability)
    }

    @Test
    fun `feature prices`() {
        val e = EconomyConfig()
        assertEquals(Coins(150), e.featurePrice(1))
        assertEquals(Coins(700), e.featurePrice(7))
    }
}
