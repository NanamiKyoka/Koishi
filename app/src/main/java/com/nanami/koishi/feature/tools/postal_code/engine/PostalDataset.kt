package com.nanami.koishi.feature.tools.postal_code.engine

import kotlinx.serialization.Serializable

data class PostalArea(
    val province: String,
    val city: String,
    val district: String,
    val postalCode: String,
    val pinyin: String
)

@Serializable
private data class RawPostalArea(
    val province: String = "",
    val city: String = "",
    val name: String = "",
    val zipCode: String = "",
    val pinyin: String = ""
)

class PostalDataset private constructor(
    private val areas: List<PostalArea>
) {

    private val byPostalCode: Map<String, List<PostalArea>> = areas.groupBy { it.postalCode }
    private val searchKeys: List<String> = areas.map { buildSearchKey(it) }

    val size: Int get() = areas.size

    fun findByPostalCode(postalCode: String): List<PostalArea> =
        byPostalCode[postalCode].orEmpty()

    fun search(keyword: String, limit: Int = SEARCH_LIMIT): List<PostalArea> {
        val query = keyword.trim().lowercase()
        if (query.isEmpty()) return emptyList()

        val ranked = ArrayList<Pair<Int, PostalArea>>(SEARCH_LIMIT)
        areas.forEachIndexed { index, area ->
            val score = scoreOf(area, searchKeys[index], query)
            if (score != NO_MATCH) ranked += score to area
        }
        ranked.sortBy { it.first }
        return ranked.asSequence().map { it.second }.take(limit).toList()
    }

    private fun scoreOf(area: PostalArea, searchKey: String, query: String): Int {
        val district = area.district.lowercase()
        val name = area.district.dropLastWhile { it in SUFFIX_BOUNDARY }
        return when {
            district == query || name == query -> 0
            district.startsWith(query) || name.startsWith(query) -> 1
            searchKey.startsWith(query) -> 2
            searchKey.contains(query) -> 3
            else -> NO_MATCH
        }
    }

    private fun buildSearchKey(area: PostalArea): String =
        (area.province + area.city + area.district + area.pinyin)
            .lowercase()
            .filter { it.isLetterOrDigit() }

    companion object {
        private const val SEARCH_LIMIT = 40
        private const val NO_MATCH = Int.MAX_VALUE
        private val SUFFIX_BOUNDARY = setOf('区', '县', '市', '旗', '盟')

        fun fromRaw(json: String): PostalDataset {
            val raw = PostalDatasetJson.decodeFromString<List<RawPostalArea>>(json)
            val areas = raw.mapNotNull { item ->
                val code = item.zipCode.trim()
                if (!POSTAL_CODE_PATTERN.matches(code)) return@mapNotNull null
                PostalArea(
                    province = item.province.trim(),
                    city = item.city.trim(),
                    district = item.name.trim(),
                    postalCode = code,
                    pinyin = item.pinyin.filter { it.isLetter() }.lowercase()
                )
            }
            return if (areas.isEmpty()) empty() else PostalDataset(areas)
        }

        fun fromCompact(text: String): PostalDataset {
            val lines = text.lineSequence().filter { it.isNotBlank() }.toList()
            if (lines.size < 5) return empty()

            val provinces = lines[1].split(DELIMITER)
            val cities = lines[3].split(DELIMITER)
            val areas = lines.drop(5).mapNotNull { line ->
                val parts = line.split(DELIMITER)
                if (parts.size < 5) return@mapNotNull null
                val provinceIndex = parts[0].toIntOrNull() ?: return@mapNotNull null
                val cityIndex = parts[1].toIntOrNull() ?: return@mapNotNull null
                if (!POSTAL_CODE_PATTERN.matches(parts[3])) return@mapNotNull null
                PostalArea(
                    province = provinces.getOrElse(provinceIndex) { "" },
                    city = cities.getOrElse(cityIndex) { "" },
                    district = parts[2],
                    postalCode = parts[3],
                    pinyin = parts[4]
                )
            }
            return if (areas.isEmpty()) empty() else PostalDataset(areas)
        }

        fun toCompact(dataset: PostalDataset): String = buildString {
            val areas = dataset.areas
            val provinces = areas.map { it.province }.distinct()
            val cities = areas.map { it.city }.distinct()
            val provinceIndex = provinces.withIndex().associate { it.value to it.index }
            val cityIndex = cities.withIndex().associate { it.value to it.index }

            append(LINE_PROVINCES).append('\n')
            append(provinces.joinToString(DELIMITER)).append('\n')
            append(LINE_CITIES).append('\n')
            append(cities.joinToString(DELIMITER)).append('\n')
            append(LINE_AREAS).append('\n')
            areas.forEach { area ->
                append(provinceIndex[area.province] ?: 0)
                append(DELIMITER)
                append(cityIndex[area.city] ?: 0)
                append(DELIMITER)
                append(area.district)
                append(DELIMITER)
                append(area.postalCode)
                append(DELIMITER)
                append(area.pinyin)
                append('\n')
            }
        }

        fun empty(): PostalDataset = PostalDataset(emptyList())

        private const val DELIMITER = "|"
        private const val LINE_PROVINCES = "P"
        private const val LINE_CITIES = "C"
        private const val LINE_AREAS = "R"
        private val POSTAL_CODE_PATTERN = Regex("^\\d{6}$")
    }
}

private val PostalDatasetJson = kotlinx.serialization.json.Json {
    ignoreUnknownKeys = true
    isLenient = true
}
