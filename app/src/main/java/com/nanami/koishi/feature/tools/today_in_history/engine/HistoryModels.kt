package com.nanami.koishi.feature.tools.today_in_history.engine

enum class HistorySource {
    SHOW_API,
    XXAPI
}

data class HistoryEvent(
    val title: String,
    val year: Int,
    val month: Int,
    val day: Int,
    val content: String = "",
    val imageUrl: String = ""
) {
    val dateLabel: String
        get() = if (year > 0) "${year}年" else ""
}

data class HistoryDay(
    val month: Int,
    val day: Int,
    val events: List<HistoryEvent>,
    val source: HistorySource
) {
    val totalCount: Int get() = events.size
}

sealed class HistoryException(message: String?) : Exception(message) {
    class MissingApiKey : HistoryException(null)
    class InvalidApiKey : HistoryException(null)
    class QuotaExceeded : HistoryException(null)
    class ServerError(val statusCode: Int) : HistoryException(null)
    class Network : HistoryException(null)
    class Parse : HistoryException(null)
}
