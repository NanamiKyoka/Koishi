package com.nanami.koishi.feature.tools.text_apps.diff

import java.util.Locale

enum class DiffLineKind {
    EQUAL,
    INSERT,
    DELETE
}

data class DiffLine(
    val kind: DiffLineKind,
    val oldLineNumber: Int?,
    val newLineNumber: Int?,
    val text: String
)

data class DiffResult(
    val lines: List<DiffLine>,
    val additions: Int,
    val deletions: Int
)

class DiffLimitExceededException(
    val oldLineCount: Int,
    val newLineCount: Int
) : IllegalArgumentException("Text diff supports at most $MAX_DIFF_LINES lines per side")

object DiffEngine {

    fun diff(
        oldText: String,
        newText: String,
        ignoreCase: Boolean = false,
        ignoreWhitespace: Boolean = false
    ): Result<DiffResult> = runCatching {
        val oldLines = oldText.lines()
        val newLines = newText.lines()
        if (oldLines.size > MAX_DIFF_LINES || newLines.size > MAX_DIFF_LINES) {
            throw DiffLimitExceededException(oldLines.size, newLines.size)
        }

        val lcs = Array(oldLines.size + 1) { IntArray(newLines.size + 1) }
        for (oldIndex in oldLines.indices.reversed()) {
            for (newIndex in newLines.indices.reversed()) {
                lcs[oldIndex][newIndex] = if (
                    equalLines(oldLines[oldIndex], newLines[newIndex], ignoreCase, ignoreWhitespace)
                ) {
                    lcs[oldIndex + 1][newIndex + 1] + 1
                } else {
                    maxOf(lcs[oldIndex + 1][newIndex], lcs[oldIndex][newIndex + 1])
                }
            }
        }

        val resultLines = mutableListOf<DiffLine>()
        var oldIndex = 0
        var newIndex = 0
        var additions = 0
        var deletions = 0

        while (oldIndex < oldLines.size || newIndex < newLines.size) {
            when {
                oldIndex < oldLines.size &&
                    newIndex < newLines.size &&
                    equalLines(oldLines[oldIndex], newLines[newIndex], ignoreCase, ignoreWhitespace) -> {
                    resultLines += DiffLine(
                        kind = DiffLineKind.EQUAL,
                        oldLineNumber = oldIndex + 1,
                        newLineNumber = newIndex + 1,
                        text = oldLines[oldIndex]
                    )
                    oldIndex++
                    newIndex++
                }

                newIndex < newLines.size &&
                    (oldIndex == oldLines.size || lcs[oldIndex][newIndex + 1] > lcs[oldIndex + 1][newIndex]) -> {
                    resultLines += DiffLine(
                        kind = DiffLineKind.INSERT,
                        oldLineNumber = null,
                        newLineNumber = newIndex + 1,
                        text = newLines[newIndex]
                    )
                    additions++
                    newIndex++
                }

                else -> {
                    resultLines += DiffLine(
                        kind = DiffLineKind.DELETE,
                        oldLineNumber = oldIndex + 1,
                        newLineNumber = null,
                        text = oldLines[oldIndex]
                    )
                    deletions++
                    oldIndex++
                }
            }
        }

        DiffResult(
            lines = resultLines,
            additions = additions,
            deletions = deletions
        )
    }

    private fun equalLines(
        left: String,
        right: String,
        ignoreCase: Boolean,
        ignoreWhitespace: Boolean
    ): Boolean = normalize(left, ignoreCase, ignoreWhitespace) == normalize(right, ignoreCase, ignoreWhitespace)

    private fun normalize(line: String, ignoreCase: Boolean, ignoreWhitespace: Boolean): String {
        var normalized = line
        if (ignoreWhitespace) normalized = normalized.trim().replace(WHITESPACE_REGEX, " ")
        if (ignoreCase) normalized = normalized.lowercase(Locale.ROOT)
        return normalized
    }

    private val WHITESPACE_REGEX = Regex("\\s+")
}

const val MAX_DIFF_LINES = 800
