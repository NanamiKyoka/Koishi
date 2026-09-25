package com.nanami.koishi.feature.tools.decision_maker.engine

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DecisionPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    var storedTopics: List<DecisionTopic>
        get() {
            val raw = prefs.getString(KEY_TOPICS, null) ?: return emptyList()
            return try {
                json.decodeFromString<List<DecisionTopic>>(raw)
            } catch (e: Exception) {
                emptyList()
            }
        }
        set(value) {
            prefs.edit().putString(KEY_TOPICS, json.encodeToString(value)).apply()
        }

    var selectedTopicId: String
        get() = prefs.getString(KEY_SELECTED_TOPIC, null) ?: BuiltInPresets.defaultTopicId
        set(value) {
            prefs.edit().putString(KEY_SELECTED_TOPIC, value).apply()
        }

    var mode: DecisionMode
        get() {
            val name = prefs.getString(KEY_MODE, null) ?: return DecisionMode.WHEEL
            return DecisionMode.entries.firstOrNull { it.name == name } ?: DecisionMode.WHEEL
        }
        set(value) {
            prefs.edit().putString(KEY_MODE, value.name).apply()
        }

    var hapticsEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTICS, true)
        set(value) {
            prefs.edit().putBoolean(KEY_HAPTICS, value).apply()
        }

    var hiddenBuiltInIds: Set<String>
        get() = prefs.getStringSet(KEY_HIDDEN_BUILT_INS, emptySet()) ?: emptySet()
        set(value) {
            prefs.edit().putStringSet(KEY_HIDDEN_BUILT_INS, value).apply()
        }

    companion object {
        private const val PREFS_NAME = "koishi_decision_maker"
        private const val KEY_TOPICS = "topics_json"
        private const val KEY_SELECTED_TOPIC = "selected_topic_id"
        private const val KEY_MODE = "decision_mode"
        private const val KEY_HAPTICS = "haptics_enabled"
        private const val KEY_HIDDEN_BUILT_INS = "hidden_builtin_ids"
    }
}
