package com.focusfirst.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.focusfirst.data.model.AmbientSound
import com.focusfirst.util.ThemeModePolicy
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

val Context.focusFirstSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "focusfirst_settings",
)

class SettingsRepository(
    private val appContext: Context,
    private val dataStore: DataStore<Preferences>,
) {

    private val legacyPrefs =
        appContext.getSharedPreferences(FOCUS_FIRST_PREFS, Context.MODE_PRIVATE)

    companion object {
        const val FOCUS_FIRST_PREFS = "focusfirst_prefs"
        const val LEGACY_VIBRATE_ENABLED = "vibrate_enabled"
        const val LEGACY_SOUND_TYPE = "sound_type"

        val KEY_FOCUS_MINUTES = intPreferencesKey("KEY_FOCUS_MINUTES")
        val KEY_SHORT_BREAK = intPreferencesKey("KEY_SHORT_BREAK")
        val KEY_LONG_BREAK = intPreferencesKey("KEY_LONG_BREAK")
        val KEY_SESSIONS_BEFORE_LONG_BREAK = intPreferencesKey("KEY_SESSIONS_BEFORE_LONG_BREAK")
        val KEY_SOUND_TYPE = stringPreferencesKey("KEY_SOUND_TYPE")
        val KEY_VIBRATE = booleanPreferencesKey("KEY_VIBRATE")
        val KEY_THEME_MODE = stringPreferencesKey("KEY_THEME_MODE")
        val KEY_AMOLED_MODE = booleanPreferencesKey("KEY_AMOLED_MODE")
        val KEY_NOTIFICATION_PERMISSION_ASKED =
            booleanPreferencesKey("KEY_NOTIFICATION_PERMISSION_ASKED")
        val KEY_PRO_UNLOCKED = booleanPreferencesKey("KEY_PRO_UNLOCKED")
        val KEY_DAILY_GOAL = intPreferencesKey("KEY_DAILY_GOAL")
        val KEY_AUTO_START = booleanPreferencesKey("KEY_AUTO_START")
        val KEY_AMBIENT_SOUND = stringPreferencesKey("ambient_sound")
        val KEY_AMBIENT_VOLUME = floatPreferencesKey("ambient_volume")
        val KEY_DND_ENABLED  = booleanPreferencesKey("dnd_enabled")
        val KEY_EULA_ACCEPTED = booleanPreferencesKey("eula_accepted")

        // Badge storage
        /** Pipe-separated list of unlocked badge IDs, e.g. "first_step|on_a_roll" */
        val KEY_UNLOCKED_BADGES = stringPreferencesKey("unlocked_badges")
        /**
         * Serialised map of badgeId -> epochMillis, stored as "id:ms|id:ms|..."
         * This lets us show "Unlocked on …" without a separate entity.
         */
        val KEY_BADGE_TIMES = stringPreferencesKey("badge_unlock_times")
    }

    val focusMinutes: Flow<Int> = dataStore.data.map { it[KEY_FOCUS_MINUTES] ?: 25 }

    val shortBreakMinutes: Flow<Int> = dataStore.data.map { it[KEY_SHORT_BREAK] ?: 5 }

    val longBreakMinutes: Flow<Int> = dataStore.data.map { it[KEY_LONG_BREAK] ?: 15 }

    val sessionsBeforeLongBreak: Flow<Int> =
        dataStore.data.map { it[KEY_SESSIONS_BEFORE_LONG_BREAK] ?: 4 }

    val soundType: Flow<String> = dataStore.data.map { it[KEY_SOUND_TYPE] ?: "Bell" }

    val vibrate: Flow<Boolean> = dataStore.data.map { it[KEY_VIBRATE] ?: true }

    val themeMode: Flow<String> = dataStore.data.map {
        ThemeModePolicy.normalize(it[KEY_THEME_MODE])
    }

    val amoledMode: Flow<Boolean> = dataStore.data.map { it[KEY_AMOLED_MODE] ?: false }

    val notificationPermissionAsked: Flow<Boolean> =
        dataStore.data.map { it[KEY_NOTIFICATION_PERMISSION_ASKED] ?: false }

    val proUnlocked: Flow<Boolean> = dataStore.data.map { it[KEY_PRO_UNLOCKED] ?: false }

    val dailyGoal: Flow<Int> = dataStore.data.map { prefs ->
        (prefs[KEY_DAILY_GOAL] ?: 8).coerceIn(1, 20)
    }

    val autoStart: Flow<Boolean> = dataStore.data.map { it[KEY_AUTO_START] ?: false }

    val ambientSound: Flow<AmbientSound> = dataStore.data.map { prefs ->
        val name = prefs[KEY_AMBIENT_SOUND] ?: AmbientSound.NONE.name
        runCatching { AmbientSound.valueOf(name) }.getOrDefault(AmbientSound.NONE)
    }

    val ambientVolume: Flow<Float> = dataStore.data.map { prefs ->
        prefs[KEY_AMBIENT_VOLUME] ?: 0.5f
    }

    val dndEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_DND_ENABLED] ?: false }

    val eulaAccepted: Flow<Boolean> = dataStore.data.map { it[KEY_EULA_ACCEPTED] ?: false }

    /** Set of badge IDs that the user has already unlocked. */
    val unlockedBadges: Flow<Set<String>> = dataStore.data.map { prefs ->
        val raw = prefs[KEY_UNLOCKED_BADGES] ?: ""
        if (raw.isBlank()) emptySet() else raw.split("|").toSet()
    }

    /** Map of badgeId -> epochMillis when it was first unlocked. */
    val unlockedBadgeTimes: Flow<Map<String, Long>> = dataStore.data.map { prefs ->
        val raw = prefs[KEY_BADGE_TIMES] ?: ""
        if (raw.isBlank()) return@map emptyMap()
        raw.split("|").mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) parts[0] to (parts[1].toLongOrNull() ?: return@mapNotNull null)
            else null
        }.toMap()
    }

    suspend fun updateAmbientSound(sound: AmbientSound) {
        dataStore.edit { it[KEY_AMBIENT_SOUND] = sound.name }
    }

    suspend fun updateAmbientVolume(volume: Float) {
        dataStore.edit { it[KEY_AMBIENT_VOLUME] = volume }
    }

    suspend fun updateDndEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_DND_ENABLED] = enabled }
    }

    suspend fun acceptEula() {
        dataStore.edit { it[KEY_EULA_ACCEPTED] = true }
    }

    /**
     * Persist newly unlocked badge IDs (merged with any already-stored).
     * Also records the current timestamp for each new badge.
     */
    suspend fun unlockBadges(newIds: Set<String>) {
        if (newIds.isEmpty()) return
        val now = System.currentTimeMillis()
        dataStore.edit { prefs ->
            // Merge into existing set
            val existing = run {
                val raw = prefs[KEY_UNLOCKED_BADGES] ?: ""
                if (raw.isBlank()) emptySet() else raw.split("|").toSet()
            }
            prefs[KEY_UNLOCKED_BADGES] = (existing + newIds).joinToString("|")

            // Merge timestamps (only record first-unlock time)
            val existingTimes = run {
                val raw = prefs[KEY_BADGE_TIMES] ?: ""
                if (raw.isBlank()) mutableMapOf()
                else raw.split("|").mapNotNull { entry ->
                    val parts = entry.split(":")
                    if (parts.size == 2) parts[0] to (parts[1].toLongOrNull() ?: return@mapNotNull null)
                    else null
                }.toMap().toMutableMap()
            }
            for (id in newIds) {
                if (id !in existingTimes) existingTimes[id] = now
            }
            prefs[KEY_BADGE_TIMES] = existingTimes.entries.joinToString("|") { (k, v) -> "$k:$v" }
        }
    }

    suspend fun <T> update(key: Preferences.Key<T>, value: T) {
        dataStore.edit { preferences ->
            preferences[key] = value
        }
        when (key.name) {
            KEY_SOUND_TYPE.name ->
                legacyPrefs.edit().putString(LEGACY_SOUND_TYPE, value as String).apply()

            KEY_VIBRATE.name ->
                legacyPrefs.edit().putBoolean(LEGACY_VIBRATE_ENABLED, value as Boolean).apply()
        }
    }
}
