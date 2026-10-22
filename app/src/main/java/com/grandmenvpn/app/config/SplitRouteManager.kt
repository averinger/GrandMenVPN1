package com.grandmenvpn.app.config

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.preferencesDataStore
import androidx.datastore.preferences.Preferences
import kotlinx.coroutines.flow.first

/**
 * Настройки сплит-туннелингa: авто-сплит RU-трафика и Kill Switch.
 * Состояние хранится в DataStore Preferences.
 */
object SplitRouteManager {

    private val AUTO_SPLIT = booleanPreferencesKey("auto_split")
    private val KILL_SWITCH = booleanPreferencesKey("kill_switch")

    private fun dataStore(context: Context) =
        context.preferencesDataStore(name = "split_route_prefs")

    suspend fun isAutoSplitEnabled(context: Context): Boolean =
        dataStore(context).data.first { it.containsKey(AUTO_SPLIT) }.let { it[AUTO_SPLIT] ?: false }

    suspend fun setAutoSplitEnabled(context: Context, enabled: Boolean) =
        dataStore(context).edit { it[AUTO_SPLIT] = enabled }

    suspend fun isKillSwitchEnabled(context: Context): Boolean =
        dataStore(context).data.first { it.containsKey(KILL_SWITCH) }.let { it[KILL_SWITCH] ?: false }

    suspend fun setKillSwitchEnabled(context: Context, enabled: Boolean) =
        dataStore(context).edit { it[KILL_SWITCH] = enabled }
}
