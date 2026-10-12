package com.nanami.koishi.feature.tools.text_apps.text_process

import java.util.Locale

enum class CaseMode {
    LOWERCASE,
    UPPERCASE,
    TITLE,
    SENTENCE,
    SWAP
}

enum class SortMode {
    ASCENDING,
    DESCENDING,
    LENGTH_ASCENDING,
    LENGTH_DESCENDING,
    NATURAL
}

data class TextProcessOptions(
    val trimLines: Boolean = false,
    val removeEmpty: Boolean = false,
    val ignoreCase: Boolean = false
)

data class TextStatistics(
    val characters: Int,
    val charactersWithoutWhitespace: Int,
    val letters: Int,
    val digits: Int,
    val cjkCharacters: Int,
    val whitespaceCharacters: Int,
    val words: Int,
    val lines: Int,
    val paragraphs: Int,
    val utf8Bytes: Int
)

object TextProcessEngine {

    fun transformCase(input: String, mode: CaseMode): String = when (mode) {
        CaseMode.LOWERCASE -> input.lowercase(Locale.ROOT)
        CaseMode.UPPERCASE -> input.uppercase(Locale.ROOT)
        CaseMode.TITLE -> toTitleCase(input)
        CaseMode.SENTENCE -> toSentenceCase(input)
        CaseMode.SWAP -> input.map { char ->
            when {
                char.isUpperCase() -> char.lowercaseChar()
                char.isLowerCase() -> char.uppercaseChar()
                else -> char
            }
        }.joinToString("")
    }

    fun deduplicate(input: String, options: TextProcessOptions): String =
        processLines(input, options).distinctBy { line ->
            if (options.ignoreCase) line.lowercase(Locale.ROOT) else line
        }.joinToString("\n")

    fun sortLines(input: String, mode: SortMode, options: TextProcessOptions): String {
        val lines = processLines(input, options)
        val comparator = Comparator<String> { left, right -> compareLines(left, right, mode, options.ignoreCase) }
        return lines.sortedWith(comparator).joinToString("\n")
    }

    fun statistics(input: String): TextStatistics {
        val codePoints = input.toCodePoints()
        val whitespace = codePoints.count(Character::isWhitespace)
        return TextStatistics(
            characters = codePoints.size,
            charactersWithoutWhitespace = codePoints.size - whitespace,
            letters = codePoints.count(Character::isLetter),
            digits = codePoints.count(Character::isDigit),
            cjkCharacters = codePoints.count(::isCjk),
            whitespaceCharacters = whitespace,
            words = WORD_REGEX.findAll(input).count(),
            lines = if (input.isEmpty()) 0 else input.lines().size,
            paragraphs = if (input.isBlank()) 0 else input.trim().split(PARAGRAPH_REGEX).count { it.isNotBlank() },
            utf8Bytes = input.toByteArray(Charsets.UTF_8).size
        )
    }

    private fun processLines(input: String, options: TextProcessOptions): List<String> =
        input.lines()
            .map { line -> if (options.trimLines) line.trim() else line }
            .filter { line -> !options.removeEmpty || line.isNotEmpty() }

    private fun compareLines(left: String, right: String, mode: SortMode, ignoreCase: Boolean): Int = when (mode) {
        SortMode.ASCENDING -> compareText(left, right, ignoreCase)
        SortMode.DESCENDING -> compareText(right, left, ignoreCase)
        SortMode.LENGTH_ASCENDING -> left.length.compareTo(right.length)
            .takeIf { it != 0 } ?: compareText(left, right, ignoreCase)
        SortMode.LENGTH_DESCENDING -> right.length.compareTo(left.length)
            .takeIf { it != 0 } ?: compareText(left, right, ignoreCase)
        SortMode.NATURAL -> naturalCompare(left, right, ignoreCase)
    }

    private fun compareText(left: String, right: String, ignoreCase: Boolean): Int =
        if (ignoreCase) left.compareTo(right, ignoreCase = true) else left.compareTo(right)

    private fun naturalCompare(left: String, right: String, ignoreCase: Boolean): Int {
        var leftIndex = 0
        var rightIndex = 0
        while (leftIndex < left.length && rightIndex < right.length) {
            val leftChar = left[leftIndex]
            val rightChar = right[rightIndex]
            if (leftChar.isDigit() && rightChar.isDigit()) {
                var leftEnd = leftIndex
                var rightEnd = rightIndex
                while (leftEnd < left.length && left[leftEnd].isDigit()) leftEnd++
                while (rightEnd < right.length && right[rightEnd].isDigit()) rightEnd++

                var leftStart = leftIndex
                var rightStart = rightIndex
                while (leftStart < leftEnd - 1 && left[leftStart] == '0') leftStart++
                while (rightStart < rightEnd - 1 && right[rightStart] == '0') rightStart++

                val leftDigits = leftEnd - leftStart
                val rightDigits = rightEnd - rightStart
                if (leftDigits != rightDigits) return leftDigits.compareTo(rightDigits)
                for (offset in 0 until leftDigits) {
                    val comparison = left[leftStart + offset].compareTo(right[rightStart + offset])
                    if (comparison != 0) return comparison
                }
                val runLengthComparison = (leftEnd - leftIndex).compareTo(rightEnd - rightIndex)
                if (runLengthComparison != 0) return runLengthComparison
                leftIndex = leftEnd
                rightIndex = rightEnd
            } else {
                val comparison = compareText(leftChar.toString(), rightChar.toString(), ignoreCase)
                if (comparison != 0) return comparison
                leftIndex++
                rightIndex++
            }
        }
        return (left.length - leftIndex).compareTo(right.length - rightIndex)
    }

    private fun toTitleCase(input: String): String = buildString(input.length) {
        var wordStart = true
        input.forEach { char ->
            when {
                wordStart && char.isLetter() -> append(char.titlecaseChar())
                char.isLetter() -> append(char.lowercaseChar())
                else -> append(char)
            }
            wordStart = !char.isLetterOrDigit()
        }
    }

    private fun toSentenceCase(input: String): String = buildString(input.length) {
        var sentenceStart = true
        input.forEach { char ->
            when {
                char.isLetter() -> {
                    append(if (sentenceStart) char.titlecaseChar() else char.lowercaseChar())
                    sentenceStart = false
                }

                else -> {
                    append(char)
                    if (char in SENTENCE_ENDINGS) sentenceStart = true
                }
            }
        }
    }

    private fun isCjk(codePoint: Int): Boolean =
        codePoint in 0x3400..0x4DBF ||
            codePoint in 0x4E00..0x9FFF ||
            codePoint in 0xF900..0xFAFF ||
            codePoint in 0x20000..0x2FA1F

    private fun String.toCodePoints(): List<Int> {
        val result = ArrayList<Int>(length)
        var index = 0
        while (index < length) {
            val codePoint = codePointAt(index)
            result += codePoint
            index += Character.charCount(codePoint)
        }
        return result
    }

    private val WORD_REGEX = Regex("[\\p{L}\\p{N}_]+")
    private val PARAGRAPH_REGEX = Regex("\\n\\s*\\n")
    private val SENTENCE_ENDINGS = setOf('.', '!', '?', '。', '！', '？', '\n')
}
