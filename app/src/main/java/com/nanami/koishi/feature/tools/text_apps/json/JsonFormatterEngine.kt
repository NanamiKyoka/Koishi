package com.nanami.koishi.feature.tools.text_apps.json

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

object JsonFormatterEngine {

    fun format(input: String, indentSize: Int = 4): Result<String> = runCatching {
        require(indentSize in 1..8)
        render(Json.parseToJsonElement(input), indentSize, 0)
    }

    fun minify(input: String): Result<String> = runCatching {
        Json.parseToJsonElement(input).toString()
    }

    private fun render(element: JsonElement, indentSize: Int, depth: Int): String = when (element) {
        is JsonObject -> {
            if (element.isEmpty()) {
                "{}"
            } else {
                val childIndent = indent(depth + 1, indentSize)
                val closingIndent = indent(depth, indentSize)
                element.entries.joinToString(
                    separator = ",\n",
                    prefix = "{\n",
                    postfix = "\n$closingIndent}"
                ) { (key, value) ->
                    "$childIndent${JsonPrimitive(key)}: ${render(value, indentSize, depth + 1)}"
                }
            }
        }

        is JsonArray -> {
            if (element.isEmpty()) {
                "[]"
            } else {
                val childIndent = indent(depth + 1, indentSize)
                val closingIndent = indent(depth, indentSize)
                element.joinToString(
                    separator = ",\n",
                    prefix = "[\n",
                    postfix = "\n$closingIndent]"
                ) { value ->
                    "$childIndent${render(value, indentSize, depth + 1)}"
                }
            }
        }

        is JsonPrimitive -> element.toString()
    }

    private fun indent(depth: Int, indentSize: Int): String = " ".repeat(depth * indentSize)
}
