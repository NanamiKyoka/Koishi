package com.nanami.koishi.feature.tools.postal_code

import androidx.annotation.StringRes
import com.nanami.koishi.feature.tools.postal_code.engine.PostalCountries
import com.nanami.koishi.feature.tools.postal_code.engine.PostalDirection
import com.nanami.koishi.feature.tools.postal_code.engine.PostalRecord
import com.nanami.koishi.feature.tools.postal_code.engine.PostalSource

sealed interface PostalLoadState {
    data object Idle : PostalLoadState
    data object Loading : PostalLoadState
    data class Success(
        val records: List<PostalRecord>,
        val source: PostalSource,
        val fromCache: Boolean
    ) : PostalLoadState
    data object Empty : PostalLoadState
    data class Error(@StringRes val messageRes: Int) : PostalLoadState
}

sealed interface PostalDatasetState {
    data object Absent : PostalDatasetState
    data class Downloading(val progress: Int) : PostalDatasetState
    data class Ready(val areaCount: Int) : PostalDatasetState
    data class Failed(@StringRes val messageRes: Int) : PostalDatasetState
}

data class PostalCodeUiState(
    val direction: PostalDirection = PostalDirection.CODE_TO_REGION,
    val countryCode: String = PostalCountries.CHINA_CODE,
    val queryInput: String = "",
    val loadState: PostalLoadState = PostalLoadState.Idle,
    val datasetState: PostalDatasetState = PostalDatasetState.Absent,
    val history: List<String> = emptyList(),
    @StringRes val userMessageRes: Int? = null
) {
    val records: List<PostalRecord>
        get() = (loadState as? PostalLoadState.Success)?.records.orEmpty()

    val isLoading: Boolean
        get() = loadState is PostalLoadState.Loading

    val canSubmit: Boolean
        get() = queryInput.isNotBlank() && !isLoading && !requiresDatasetDownload

    val isDomestic: Boolean
        get() = countryCode.equals(PostalCountries.CHINA_CODE, ignoreCase = true)

    val requiresDatasetDownload: Boolean
        get() = isDomestic && datasetState !is PostalDatasetState.Ready
}

sealed interface PostalCodeUiEvent {
    data class OnDirectionChange(val direction: PostalDirection) : PostalCodeUiEvent
    data class OnCountryChange(val countryCode: String) : PostalCodeUiEvent
    data class OnQueryInputChange(val input: String) : PostalCodeUiEvent
    data object OnSubmit : PostalCodeUiEvent
    data object OnRefresh : PostalCodeUiEvent
    data object OnDownloadDataset : PostalCodeUiEvent
    data object OnClearDataset : PostalCodeUiEvent
    data class OnHistoryPick(val keyword: String) : PostalCodeUiEvent
    data object OnClearHistory : PostalCodeUiEvent
    data class OnCopyPostalCode(val code: String) : PostalCodeUiEvent
    data object OnDismissMessage : PostalCodeUiEvent
}
