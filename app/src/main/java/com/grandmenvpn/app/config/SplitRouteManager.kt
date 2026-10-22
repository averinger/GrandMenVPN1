package com.grandmenvpn.app.config

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

// Объявление DataStore должно быть на уровне файла, вне объекта!
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "split_route_prefs")

/**
 * Настройки сплит-туннелингa: авто-сплит RU-трафика и Kill Switch.
 * Состояние хранится в DataStore Preferences.
 */
object SplitRouteManager {

    private val AUTO_SPLIT = booleanPreferencesKey("auto_split")
    private val KILL_SWITCH = booleanPreferencesKey("kill_switch")

    suspend fun isAutoSplitEnabled(context: Context): Boolean {
        val prefs = context.dataStore.data.first()
        return prefs[AUTO_SPLIT] ?: true // было false
    }

    suspend fun setAutoSplitEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[AUTO_SPLIT] = enabled }
    }

    suspend fun isKillSwitchEnabled(context: Context): Boolean {
        val prefs = context.dataStore.data.first()
        return prefs[KILL_SWITCH] ?: true // было false
    }

    suspend fun setKillSwitchEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[KILL_SWITCH] = enabled }
    }
}