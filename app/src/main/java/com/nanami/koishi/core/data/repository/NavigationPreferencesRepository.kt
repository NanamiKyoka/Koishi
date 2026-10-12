package com.nanami.koishi.core.data.repository

import android.content.Context
import android.content.SharedPreferences

enum class LaunchTab {
    FAVORITES,
    TOOLBOX
}

interface NavigationPreferencesRepository {
    val lastMainTab: LaunchTab

    fun setLastMainTab(tab: LaunchTab)
}

class SharedPreferencesNavigationPreferencesRepository(
    context: Context
) : NavigationPreferencesRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override val lastMainTab: LaunchTab
        get() = prefs.getString(KEY_LAST_MAIN_TAB, null)
            ?.let { saved -> LaunchTab.entries.find { it.name == saved } }
            ?: LaunchTab.TOOLBOX

    override fun setLastMainTab(tab: LaunchTab) {
        prefs.edit().putString(KEY_LAST_MAIN_TAB, tab.name).apply()
    }

    companion object {
        private const val PREFS_NAME = "koishi_navigation"
        private const val KEY_LAST_MAIN_TAB = "last_main_tab"
    }
}
