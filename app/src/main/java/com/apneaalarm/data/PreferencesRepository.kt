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

        // Training mode
        val TRAINING_MODE = stringPreferencesKey("training_mode")

        // Breath hold settings
        val MAX_STATIC_BREATH_HOLD_DURATION = intPreferencesKey("max_static_breath_hold_duration_seconds")

        // Manual interval settings
        val USE_MANUAL_INTERVAL_SETTINGS = booleanPreferencesKey("use_manual_interval_settings")
        val MANUAL_BREATH_HOLD_DURATION = intPreferencesKey("manual_breath_hold_duration_seconds")
        val MANUAL_R0_SECONDS = intPreferencesKey("manual_r0_seconds")
        val MANUAL_RN_SECONDS = intPreferencesKey("manual_rn_seconds")
        val MANUAL_NUMBER_OF_INTERVALS = intPreferencesKey("manual_number_of_intervals")
        val MANUAL_P_FACTOR = floatPreferencesKey("manual_p_factor")

        // Volume multipliers
        val INTRO_BOWL_VOLUME_MULTIPLIER = intPreferencesKey("intro_bowl_volume_multiplier")
        val BREATH_CHIME_VOLUME_MULTIPLIER = intPreferencesKey("breath_chime_volume_multiplier")
        val HOLD_CHIME_VOLUME_MULTIPLIER = intPreferencesKey("hold_chime_volume_multiplier")

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
        val modeString = preferences[PreferencesKeys.TRAINING_MODE]
        val trainingMode = when (modeString) {
            "TRAINING", "INTENSE" -> TrainingMode.TRAINING  // Accept both for backwards compatibility
            else -> TrainingMode.RELAXATION
        }

        UserPreferences(
            alarmHour = preferences[PreferencesKeys.ALARM_HOUR] ?: 7,
            alarmMinute = preferences[PreferencesKeys.ALARM_MINUTE] ?: 0,
            alarmEnabled = preferences[PreferencesKeys.ALARM_ENABLED] ?: false,
            isFirstTimeSetupComplete = preferences[PreferencesKeys.FIRST_TIME_SETUP_COMPLETE] ?: false,
            trainingMode = trainingMode,
            maxStaticBreathHoldDurationSeconds = preferences[PreferencesKeys.MAX_STATIC_BREATH_HOLD_DURATION] ?: 60,
            useManualIntervalSettings = preferences[PreferencesKeys.USE_MANUAL_INTERVAL_SETTINGS] ?: false,
            manualBreathHoldDurationSeconds = preferences[PreferencesKeys.MANUAL_BREATH_HOLD_DURATION] ?: 36,
            manualR0Seconds = preferences[PreferencesKeys.MANUAL_R0_SECONDS] ?: 45,
            manualRnSeconds = preferences[PreferencesKeys.MANUAL_RN_SECONDS] ?: 9,
            manualNumberOfIntervals = preferences[PreferencesKeys.MANUAL_NUMBER_OF_INTERVALS] ?: 6,
            manualPFactor = preferences[PreferencesKeys.MANUAL_P_FACTOR] ?: 1.4f,
            introBowlVolumeMultiplier = preferences[PreferencesKeys.INTRO_BOWL_VOLUME_MULTIPLIER] ?: 10,
            breathChimeVolumeMultiplier = preferences[PreferencesKeys.BREATH_CHIME_VOLUME_MULTIPLIER] ?: 10,
            holdChimeVolumeMultiplier = preferences[PreferencesKeys.HOLD_CHIME_VOLUME_MULTIPLIER] ?: 10,
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

    suspend fun updateTrainingMode(mode: TrainingMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TRAINING_MODE] = mode.name
        }
    }

    suspend fun updateUseManualIntervalSettings(useManual: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_MANUAL_INTERVAL_SETTINGS] = useManual
        }
    }

    suspend fun updateManualIntervalSettings(
        breathHoldDuration: Int? = null,
        r0Seconds: Int? = null,
        rnSeconds: Int? = null,
        numberOfIntervals: Int? = null,
        pFactor: Float? = null
    ) {
        context.dataStore.edit { preferences ->
            breathHoldDuration?.let { preferences[PreferencesKeys.MANUAL_BREATH_HOLD_DURATION] = it.coerceAtLeast(5) }
            r0Seconds?.let { preferences[PreferencesKeys.MANUAL_R0_SECONDS] = it.coerceAtLeast(3) }
            rnSeconds?.let { preferences[PreferencesKeys.MANUAL_RN_SECONDS] = it.coerceAtLeast(3) }
            numberOfIntervals?.let { preferences[PreferencesKeys.MANUAL_NUMBER_OF_INTERVALS] = it.coerceIn(1, 20) }
            pFactor?.let { preferences[PreferencesKeys.MANUAL_P_FACTOR] = it.coerceIn(0.1f, 5.0f) }
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
            preferences[PreferencesKeys.TRAINING_MODE] = prefs.trainingMode.name
            preferences[PreferencesKeys.MAX_STATIC_BREATH_HOLD_DURATION] = prefs.maxStaticBreathHoldDurationSeconds
            preferences[PreferencesKeys.USE_MANUAL_INTERVAL_SETTINGS] = prefs.useManualIntervalSettings
            preferences[PreferencesKeys.MANUAL_BREATH_HOLD_DURATION] = prefs.manualBreathHoldDurationSeconds
            preferences[PreferencesKeys.MANUAL_R0_SECONDS] = prefs.manualR0Seconds
            preferences[PreferencesKeys.MANUAL_RN_SECONDS] = prefs.manualRnSeconds
            preferences[PreferencesKeys.MANUAL_NUMBER_OF_INTERVALS] = prefs.manualNumberOfIntervals
            preferences[PreferencesKeys.MANUAL_P_FACTOR] = prefs.manualPFactor
            preferences[PreferencesKeys.INTRO_BOWL_VOLUME_MULTIPLIER] = prefs.introBowlVolumeMultiplier
            preferences[PreferencesKeys.BREATH_CHIME_VOLUME_MULTIPLIER] = prefs.breathChimeVolumeMultiplier
            preferences[PreferencesKeys.HOLD_CHIME_VOLUME_MULTIPLIER] = prefs.holdChimeVolumeMultiplier

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

    suspend fun completeSetup(trainingMode: TrainingMode, maxStaticBreathHold: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TRAINING_MODE] = trainingMode.name
            preferences[PreferencesKeys.MAX_STATIC_BREATH_HOLD_DURATION] = maxStaticBreathHold
            preferences[PreferencesKeys.FIRST_TIME_SETUP_COMPLETE] = true
        }
    }
}
