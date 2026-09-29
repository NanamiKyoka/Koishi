package com.nanami.koishi.feature.tools.postal_code.engine

import androidx.annotation.StringRes
import com.nanami.koishi.R

enum class PostalDirection {
    CODE_TO_REGION,
    REGION_TO_CODE
}

enum class PostalSource {
    OFFLINE_DATASET,
    ZIPPOPOTAM
}

data class PostalCountry(
    val code: String,
    @StringRes val nameRes: Int
)

object PostalCountries {

    const val CHINA_CODE = "CN"

    val china = PostalCountry(CHINA_CODE, R.string.postal_country_cn)

    val supported: List<PostalCountry> = listOf(
        china,
        PostalCountry("US", R.string.postal_country_us),
        PostalCountry("DE", R.string.postal_country_de),
        PostalCountry("FR", R.string.postal_country_fr),
        PostalCountry("IT", R.string.postal_country_it),
        PostalCountry("ES", R.string.postal_country_es),
        PostalCountry("NL", R.string.postal_country_nl),
        PostalCountry("AT", R.string.postal_country_at),
        PostalCountry("CH", R.string.postal_country_ch),
        PostalCountry("DK", R.string.postal_country_dk),
        PostalCountry("SE", R.string.postal_country_se),
        PostalCountry("RU", R.string.postal_country_ru),
        PostalCountry("JP", R.string.postal_country_jp),
        PostalCountry("IN", R.string.postal_country_in),
        PostalCountry("AU", R.string.postal_country_au),
        PostalCountry("MX", R.string.postal_country_mx)
    )

    fun of(code: String): PostalCountry =
        supported.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: china
}

data class PostalRecord(
    val postalCode: String,
    val countryName: String = "",
    val province: String = "",
    val city: String = "",
    val district: String = "",
    val latitude: String = "",
    val longitude: String = ""
) {
    val regionPath: List<String>
        get() = listOf(province, city, district).filter { it.isNotBlank() }.distinct()

    val hasCoordinates: Boolean
        get() = latitude.isNotBlank() && longitude.isNotBlank()
}

data class PostalQueryResult(
    val records: List<PostalRecord>,
    val source: PostalSource,
    val fromCache: Boolean = false
)

sealed class PostalException(message: String?) : Exception(message) {
    class DatasetUnavailable : PostalException(null)
    class NotFound : PostalException(null)
    class Network : PostalException(null)
    class Parse : PostalException(null)
    class ServerError : PostalException(null)
}
