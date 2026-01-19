package com.apneaalarm.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class PreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        // Alarm settings
        val ALARM_HOUR = intPreferencesKey("alarm_hour")
        val ALARM_MINUTE = intPreferencesKey("alarm_minute")
        val ALARM_ENABLED = booleanPreferencesKey("alarm_enabled")
        val FIRST_TIME_SETUP_COMPLETE = booleanPreferencesKey("first_time_setup_complete")

        // Breath hold settings
        val MAX_STATIC_BREATH_HOLD_DURATION = intPreferencesKey("max_static_breath_hold_duration_seconds")

        // Volume multipliers
        val INTRO_BOWL_VOLUME_MULTIPLIER = intPreferencesKey("intro_bowl_volume_multiplier")
        val BREATH_CHIME_VOLUME_MULTIPLIER = intPreferencesKey("breath_chime_volume_multiplier")
        val HOLD_CHIME_VOLUME_MULTIPLIER = intPreferencesKey("hold_chime_volume_multiplier")

        // Breathing interval settings
        val BREATHING_INTERVAL_DURATION_MAX = intPreferencesKey("breathing_interval_duration_max_seconds")
        val BREATHING_INTERVAL_DURATION_MIN = intPreferencesKey("breathing_interval_duration_min_seconds")
        val NUMBER_OF_INTERVALS = intPreferencesKey("number_of_intervals")
        val P_FACTOR = floatPreferencesKey("p_factor")

        // Custom sound URIs
        val CUSTOM_INTRO_BOWL_URI = stringPreferencesKey("custom_intro_bowl_uri")
        val CUSTOM_BREATH_CHIME_URI = stringPreferencesKey("custom_breath_chime_uri")
        val CUSTOM_HOLD_CHIME_URI = stringPreferencesKey("custom_hold_chime_uri")

        // Intro bowl fade-in
        val FADE_IN_INTRO_BOWL = booleanPreferencesKey("fade_in_intro_bowl")

        // Snooze
        val SNOOZE_DURATION_MINUTES = intPreferencesKey("snooze_duration_minutes")
    }

    val userPreferences: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        UserPreferences(
            alarmHour = preferences[PreferencesKeys.ALARM_HOUR] ?: 7,
            alarmMinute = preferences[PreferencesKeys.ALARM_MINUTE] ?: 0,
            alarmEnabled = preferences[PreferencesKeys.ALARM_ENABLED] ?: false,
            isFirstTimeSetupComplete = preferences[PreferencesKeys.FIRST_TIME_SETUP_COMPLETE] ?: false,
            maxStaticBreathHoldDurationSeconds = preferences[PreferencesKeys.MAX_STATIC_BREATH_HOLD_DURATION] ?: 60,
            introBowlVolumeMultiplier = preferences[PreferencesKeys.INTRO_BOWL_VOLUME_MULTIPLIER] ?: 10,
            breathChimeVolumeMultiplier = preferences[PreferencesKeys.BREATH_CHIME_VOLUME_MULTIPLIER] ?: 10,
            holdChimeVolumeMultiplier = preferences[PreferencesKeys.HOLD_CHIME_VOLUME_MULTIPLIER] ?: 10,
            breathingIntervalDurationMaxSeconds = preferences[PreferencesKeys.BREATHING_INTERVAL_DURATION_MAX] ?: 60,
            breathingIntervalDurationMinSeconds = preferences[PreferencesKeys.BREATHING_INTERVAL_DURATION_MIN] ?: 3,
            numberOfIntervals = preferences[PreferencesKeys.NUMBER_OF_INTERVALS] ?: 10,
            pFactor = preferences[PreferencesKeys.P_FACTOR] ?: 0.25f,
            customIntroBowlUri = preferences[PreferencesKeys.CUSTOM_INTRO_BOWL_URI],
            customBreathChimeUri = preferences[PreferencesKeys.CUSTOM_BREATH_CHIME_URI],
            customHoldChimeUri = preferences[PreferencesKeys.CUSTOM_HOLD_CHIME_URI],
            fadeInIntroBowl = preferences[PreferencesKeys.FADE_IN_INTRO_BOWL] ?: true,
            snoozeDurationMinutes = preferences[PreferencesKeys.SNOOZE_DURATION_MINUTES] ?: 5
        )
    }

    suspend fun updateAlarmTime(hour: Int, minute: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ALARM_HOUR] = hour
            preferences[PreferencesKeys.ALARM_MINUTE] = minute
        }
    }

    suspend fun updateAlarmEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ALARM_ENABLED] = enabled
        }
    }

    suspend fun updateMaxStaticBreathHoldDuration(durationSeconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MAX_STATIC_BREATH_HOLD_DURATION] = durationSeconds
        }
    }

    suspend fun updateVolumeMultipliers(
        introBowl: Int? = null,
        breathChime: Int? = null,
        holdChime: Int? = null
    ) {
        context.dataStore.edit { preferences ->
            introBowl?.let { preferences[PreferencesKeys.INTRO_BOWL_VOLUME_MULTIPLIER] = it.coerceIn(1, 10) }
            breathChime?.let { preferences[PreferencesKeys.BREATH_CHIME_VOLUME_MULTIPLIER] = it.coerceIn(1, 10) }
            holdChime?.let { preferences[PreferencesKeys.HOLD_CHIME_VOLUME_MULTIPLIER] = it.coerceIn(1, 10) }
        }
    }

    suspend fun updateBreathingIntervalSettings(
        durationMax: Int? = null,
        durationMin: Int? = null,
        numberOfIntervals: Int? = null,
        pFactor: Float? = null
    ) {
        context.dataStore.edit { preferences ->
            durationMax?.let { preferences[PreferencesKeys.BREATHING_INTERVAL_DURATION_MAX] = it }
            durationMin?.let { preferences[PreferencesKeys.BREATHING_INTERVAL_DURATION_MIN] = it }
            numberOfIntervals?.let { preferences[PreferencesKeys.NUMBER_OF_INTERVALS] = it.coerceAtLeast(1) }
            pFactor?.let { preferences[PreferencesKeys.P_FACTOR] = it.coerceIn(0.01f, 2.0f) }
        }
    }

    suspend fun updateSnoozeDuration(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SNOOZE_DURATION_MINUTES] = minutes.coerceIn(1, 60)
        }
    }

    suspend fun updateFadeInIntroBowl(fadeIn: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FADE_IN_INTRO_BOWL] = fadeIn
        }
    }

    suspend fun updateCustomSoundUri(
        introBowlUri: String? = null,
        breathChimeUri: String? = null,
        holdChimeUri: String? = null,
        clearIntroBowl: Boolean = false,
        clearBreathChime: Boolean = false,
        clearHoldChime: Boolean = false
    ) {
        context.dataStore.edit { preferences ->
            if (clearIntroBowl) {
                preferences.remove(PreferencesKeys.CUSTOM_INTRO_BOWL_URI)
            } else {
                introBowlUri?.let { preferences[PreferencesKeys.CUSTOM_INTRO_BOWL_URI] = it }
            }

            if (clearBreathChime) {
                preferences.remove(PreferencesKeys.CUSTOM_BREATH_CHIME_URI)
            } else {
                breathChimeUri?.let { preferences[PreferencesKeys.CUSTOM_BREATH_CHIME_URI] = it }
            }

            if (clearHoldChime) {
                preferences.remove(PreferencesKeys.CUSTOM_HOLD_CHIME_URI)
            } else {
                holdChimeUri?.let { preferences[PreferencesKeys.CUSTOM_HOLD_CHIME_URI] = it }
            }
        }
    }

    suspend fun completeFirstTimeSetup() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FIRST_TIME_SETUP_COMPLETE] = true
        }
    }

    suspend fun saveAllPreferences(prefs: UserPreferences) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ALARM_HOUR] = prefs.alarmHour
            preferences[PreferencesKeys.ALARM_MINUTE] = prefs.alarmMinute
            preferences[PreferencesKeys.ALARM_ENABLED] = prefs.alarmEnabled
            preferences[PreferencesKeys.FIRST_TIME_SETUP_COMPLETE] = prefs.isFirstTimeSetupComplete
            preferences[PreferencesKeys.MAX_STATIC_BREATH_HOLD_DURATION] = prefs.maxStaticBreathHoldDurationSeconds
            preferences[PreferencesKeys.INTRO_BOWL_VOLUME_MULTIPLIER] = prefs.introBowlVolumeMultiplier
            preferences[PreferencesKeys.BREATH_CHIME_VOLUME_MULTIPLIER] = prefs.breathChimeVolumeMultiplier
            preferences[PreferencesKeys.HOLD_CHIME_VOLUME_MULTIPLIER] = prefs.holdChimeVolumeMultiplier
            preferences[PreferencesKeys.BREATHING_INTERVAL_DURATION_MAX] = prefs.breathingIntervalDurationMaxSeconds
            preferences[PreferencesKeys.BREATHING_INTERVAL_DURATION_MIN] = prefs.breathingIntervalDurationMinSeconds
            preferences[PreferencesKeys.NUMBER_OF_INTERVALS] = prefs.numberOfIntervals
            preferences[PreferencesKeys.P_FACTOR] = prefs.pFactor

            prefs.customIntroBowlUri?.let { preferences[PreferencesKeys.CUSTOM_INTRO_BOWL_URI] = it }
                ?: preferences.remove(PreferencesKeys.CUSTOM_INTRO_BOWL_URI)
            prefs.customBreathChimeUri?.let { preferences[PreferencesKeys.CUSTOM_BREATH_CHIME_URI] = it }
                ?: preferences.remove(PreferencesKeys.CUSTOM_BREATH_CHIME_URI)
            prefs.customHoldChimeUri?.let { preferences[PreferencesKeys.CUSTOM_HOLD_CHIME_URI] = it }
                ?: preferences.remove(PreferencesKeys.CUSTOM_HOLD_CHIME_URI)
            preferences[PreferencesKeys.FADE_IN_INTRO_BOWL] = prefs.fadeInIntroBowl
            preferences[PreferencesKeys.SNOOZE_DURATION_MINUTES] = prefs.snoozeDurationMinutes
        }
    }
}
