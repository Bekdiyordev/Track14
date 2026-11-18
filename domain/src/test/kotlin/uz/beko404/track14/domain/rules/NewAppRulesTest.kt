package uz.beko404.track14.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.beko404.track14.domain.model.AppStatus
import uz.beko404.track14.domain.model.CampaignSummary
import uz.beko404.track14.domain.model.OwnerAction
import uz.beko404.track14.domain.model.OwnerApp
import uz.beko404.track14.domain.model.Plan

class NewAppRulesTest {
    private val valid = NewApp(
        name = " Lug'at Pro ",
        packageName = "uz.app.lugat",
        category = "Ta'lim",
        shortDesc = "Inglizcha lug'at",
        optInUrl = NewAppRules.defaultOptInUrl("uz.app.lugat"),
    )

    @Test
    fun `valid input has no errors`() {
        assertTrue(NewAppRules.errors(valid).isEmpty())
        assertEquals("Lug'at Pro", NewAppRules.normalize(valid).name)
    }

    @Test
    fun `each field is checked`() {
        val bad = NewApp(name = "", packageName = "lugat", category = "Kazino", shortDesc = "x".repeat(301), optInUrl = "https://evil.io")
        assertEquals(NewAppField.entries.toSet(), NewAppRules.errors(bad))
    }

    @Test
    fun `owner actions follow the server transitions`() {
        fun app(status: AppStatus, campaign: AppStatus?) = OwnerApp(
            "a", "A", "uz.a.b", "Ta'lim", "", "", status,
            campaign?.let { CampaignSummary(Plan.STANDARD, it, 0, 12, 0, 0, 0, null) },
        )
        assertEquals(listOf(OwnerAction.PAUSE, OwnerAction.PUBLISH), app(AppStatus.ACTIVE, AppStatus.ACTIVE).availableActions)
        assertEquals(listOf(OwnerAction.RESUME, OwnerAction.PUBLISH), app(AppStatus.PAUSED, AppStatus.PAUSED).availableActions)
        assertTrue(app(AppStatus.PUBLISHED, AppStatus.PUBLISHED).availableActions.isEmpty())
        assertTrue(app(AppStatus.DRAFT, null).availableActions.isEmpty())
        assertTrue(app(AppStatus.SUSPENDED, AppStatus.ACTIVE).availableActions.isEmpty())
    }
}
