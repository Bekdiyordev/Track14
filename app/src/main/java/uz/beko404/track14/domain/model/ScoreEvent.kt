package uz.beko404.track14.domain.model

data class ScoreEvent(
    val id: String,
    val userId: String,
    val sourceType: String,
    val sourceId: String,
    val points: Int,
    val reason: String,
    val createdAt: String,
)
