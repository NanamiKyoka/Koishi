package com.nanami.koishi.feature.tools.postal_code.engine

import java.util.Collections

sealed interface PostalLoadResult {
    data class Success(val result: PostalQueryResult) : PostalLoadResult
    data object Empty : PostalLoadResult
    data class Failure(val reason: PostalException) : PostalLoadResult
}

class PostalRepository(
    private val dataset: PostalDatasetRepository
) {

    private val cache = Collections.synchronizedMap(
        object : LinkedHashMap<String, List<PostalRecord>>(CACHE_CAPACITY, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<PostalRecord>>): Boolean =
                size > CACHE_CAPACITY
        }
    )

    suspend fun query(
        direction: PostalDirection,
        keyword: String,
        countryCode: String,
        forceRefresh: Boolean = false
    ): PostalLoadResult {
        val country = PostalCountries.of(countryCode)
        val normalized = normalize(direction, keyword)
        if (normalized.isEmpty()) return PostalLoadResult.Empty

        val isDomestic = country.code == PostalCountries.CHINA_CODE
        val source = if (isDomestic) PostalSource.OFFLINE_DATASET else PostalSource.ZIPPOPOTAM
        val cacheKey = "${direction.name}|${country.code}|$normalized"

        if (!forceRefresh) {
            cache[cacheKey]?.let { cached ->
                return PostalLoadResult.Success(PostalQueryResult(cached, source, fromCache = true))
            }
        }

        val records = try {
            if (isDomestic) {
                val data = dataset.load() ?: throw PostalException.DatasetUnavailable()
                if (direction == PostalDirection.CODE_TO_REGION) {
                    data.findByPostalCode(normalized).map { it.toRecord() }
                } else {
                    data.search(normalized).map { it.toRecord() }
                }
            } else {
                PostalEngines.fetchFromZippopotam(country.code, normalized)
            }
        } catch (error: PostalException) {
            return PostalLoadResult.Failure(error)
        }

        if (records.isEmpty()) return PostalLoadResult.Empty

        cache[cacheKey] = records
        return PostalLoadResult.Success(PostalQueryResult(records, source))
    }

    private fun PostalArea.toRecord() = PostalRecord(
        postalCode = postalCode,
        province = province,
        city = city,
        district = district
    )

    private fun normalize(direction: PostalDirection, keyword: String): String {
        val trimmed = keyword.trim()
        return when (direction) {
            PostalDirection.CODE_TO_REGION -> trimmed.filter { it.isDigit() }
            PostalDirection.REGION_TO_CODE -> trimmed
        }
    }

    private companion object {
        const val CACHE_CAPACITY = 48
    }
}
