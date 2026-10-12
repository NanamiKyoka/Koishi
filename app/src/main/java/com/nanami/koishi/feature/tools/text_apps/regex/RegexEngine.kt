package com.nanami.koishi.feature.tools.text_apps.regex

data class RegexOptions(
    val ignoreCase: Boolean = false,
    val multiline: Boolean = false,
    val dotAll: Boolean = false,
    val literal: Boolean = false
)

data class RegexGroupResult(
    val index: Int,
    val value: String?,
    val start: Int,
    val end: Int
)

data class RegexMatchResult(
    val value: String,
    val start: Int,
    val end: Int,
    val groups: List<RegexGroupResult>
)

data class RegexExtractionResult(
    val matches: List<RegexMatchResult>
) {
    val matchCount: Int get() = matches.size
}

object RegexEngine {

    fun extract(
        input: String,
        pattern: String,
        options: RegexOptions = RegexOptions()
    ): Result<RegexExtractionResult> = runCatching {
        val regex = compile(pattern, options)
        RegexExtractionResult(
            matches = regex.findAll(input).map { match ->
                RegexMatchResult(
                    value = match.value,
                    start = match.range.first,
                    end = match.range.last + 1,
                    groups = match.groups.mapIndexed { index, group ->
                        RegexGroupResult(
                            index = index,
                            value = group?.value,
                            start = group?.range?.first ?: -1,
                            end = group?.range?.last?.plus(1) ?: -1
                        )
                    }
                )
            }.toList()
        )
    }

    fun replace(
        input: String,
        pattern: String,
        replacement: String,
        replaceAll: Boolean = true,
        options: RegexOptions = RegexOptions()
    ): Result<String> = runCatching {
        val regex = compile(pattern, options)
        if (replaceAll) regex.replace(input, replacement) else regex.replaceFirst(input, replacement)
    }

    private fun compile(pattern: String, options: RegexOptions): Regex {
        val regexOptions = buildSet {
            if (options.ignoreCase) add(RegexOption.IGNORE_CASE)
            if (options.multiline) add(RegexOption.MULTILINE)
            if (options.dotAll) add(RegexOption.DOT_MATCHES_ALL)
            if (options.literal) add(RegexOption.LITERAL)
        }
        return Regex(pattern, regexOptions)
    }
}
