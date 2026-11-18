package uz.beko404.track14.data.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import uz.beko404.track14.data.profile.statsFrom
import uz.beko404.track14.data.published.publishedFrom

class PublishedMapperTest {
    @Test
    fun `published entries are sanitised`() {
        val p = publishedFrom("a", mapOf("name" to "Lug'at", "packageName" to "x;y", "testers" to -3, "rating" to 7.0))!!
        assertEquals("", p.packageName)
        assertEquals(0, p.testers)
        assertNull(p.rating)
        assertNull(publishedFrom("b", mapOf("testers" to 3)))
    }

    @Test
    fun `stats default to zero`() {
        assertEquals(0, statsFrom(null).testsCompleted)
        assertEquals(4, statsFrom(mapOf("testsCompleted" to 4L)).testsCompleted)
    }
}
