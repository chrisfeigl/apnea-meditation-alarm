package com.apneaalarm.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class PreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        // Global settings
        val MAX_STATIC_BREATH_HOLD_DURATION = intPreferencesKey("max_static_breath_hold_duration_seconds")
        val FIRST_TIME_SETUP_COMPLETE = booleanPreferencesKey("first_time_setup_complete")

        // JSON storage for new multi-alarm system
        val ALARMS_JSON = stringPreferencesKey("alarms_json")
        val SAVED_SESSIONS_JSON = stringPreferencesKey("saved_sessions_json")
        val LAST_SESSION_JSON = stringPreferencesKey("last_session_json")

        // Legacy keys (for migration)
        val ALARM_HOUR = intPreferencesKey("alarm_hour")
        val ALARM_MINUTE = intPreferencesKey("alarm_minute")
        val ALARM_ENABLED = booleanPreferencesKey("alarm_enabled")
        val ALARM_DAYS = stringPreferencesKey("alarm_days")
        val TRAINING_MODE = stringPreferencesKey("training_mode")
        val USE_MANUAL_INTERVAL_SETTINGS = booleanPreferencesKey("use_manual_interval_settings")
        val MANUAL_BREATH_HOLD_DURATION = intPreferencesKey("manual_breath_hold_duration_seconds")
        val MANUAL_R0_SECONDS = intPreferencesKey("manual_r0_seconds")
        val MANUAL_RN_SECONDS = intPreferencesKey("manual_rn_seconds")
        val MANUAL_NUMBER_OF_INTERVALS = intPreferencesKey("manual_number_of_intervals")
        val MANUAL_P_FACTOR = stringPreferencesKey("manual_p_factor_string")
        val INTRO_BOWL_VOLUME_MULTIPLIER = intPreferencesKey("intro_bowl_volume_multiplier")
        val BREATH_CHIME_VOLUME_MULTIPLIER = intPreferencesKey("breath_chime_volume_multiplier")
        val HOLD_CHIME_VOLUME_MULTIPLIER = intPreferencesKey("hold_chime_volume_multiplier")
        val CUSTOM_INTRO_BOWL_URI = stringPreferencesKey("custom_intro_bowl_uri")
        val CUSTOM_BREATH_CHIME_URI = stringPreferencesKey("custom_breath_chime_uri")
        val CUSTOM_HOLD_CHIME_URI = stringPreferencesKey("custom_hold_chime_uri")
        val FADE_IN_INTRO_BOWL = booleanPreferencesKey("fade_in_intro_bowl")
        val SNOOZE_DURATION_MINUTES = intPreferencesKey("snooze_duration_minutes")
        // Migration tracking
        val MIGRATION_V2_COMPLETE = booleanPreferencesKey("migration_v2_complete")
    }

    // JSON serialization/deserialization for SessionSettings
    private fun SessionSettings.toJson(): JSONObject = JSONObject().apply {
        put("trainingMode", trainingMode.name)
        put("useManualIntervalSettings", useManualIntervalSettings)
        put("manualBreathHoldDurationSeconds", manualBreathHoldDurationSeconds)
        put("manualR0Seconds", manualR0Seconds)
        put("manualRnSeconds", manualRnSeconds)
        put("manualNumberOfIntervals", manualNumberOfIntervals)
        put("manualPFactor", manualPFactor.toDouble())
        put("introBowlVolumeMultiplier", introBowlVolumeMultiplier)
        put("breathChimeVolumeMultiplier", breathChimeVolumeMultiplier)
        put("holdChimeVolumeMultiplier", holdChimeVolumeMultiplier)
        putOpt("customIntroBowlUri", customIntroBowlUri)
        putOpt("customBreathChimeUri", customBreathChimeUri)
        putOpt("customHoldChimeUri", customHoldChimeUri)
        put("fadeInIntroBowl", fadeInIntroBowl)
    }

    private fun JSONObject.toSessionSettings(): SessionSettings {
        val modeString = optString("trainingMode", "RELAXATION")
        val trainingMode = when (modeString) {
            "TRAINING", "INTENSE" -> TrainingMode.TRAINING
            else -> TrainingMode.RELAXATION
        }
        return SessionSettings(
            trainingMode = trainingMode,
            useManualIntervalSettings = optBoolean("useManualIntervalSettings", false),
            manualBreathHoldDurationSeconds = optInt("manualBreathHoldDurationSeconds", 36),
            manualR0Seconds = optInt("manualR0Seconds", 45),
            manualRnSeconds = optInt("manualRnSeconds", 9),
            manualNumberOfIntervals = optInt("manualNumberOfIntervals", 6),
            manualPFactor = optDouble("manualPFactor", 1.4).toFloat(),
            introBowlVolumeMultiplier = optInt("introBowlVolumeMultiplier", 10),
            breathChimeVolumeMultiplier = optInt("breathChimeVolumeMultiplier", 10),
            holdChimeVolumeMultiplier = optInt("holdChimeVolumeMultiplier", 10),
            customIntroBowlUri = optString("customIntroBowlUri").takeIf { it.isNotEmpty() },
            customBreathChimeUri = optString("customBreathChimeUri").takeIf { it.isNotEmpty() },
            customHoldChimeUri = optString("customHoldChimeUri").takeIf { it.isNotEmpty() },
            fadeInIntroBowl = optBoolean("fadeInIntroBowl", true)
        )
    }

    // JSON serialization/deserialization for Alarm
    private fun Alarm.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("hour", hour)
        put("minute", minute)
        put("enabled", enabled)
        put("days", JSONArray(days.toList()))
        put("snoozeEnabled", snoozeEnabled)
        put("snoozeDurationMinutes", snoozeDurationMinutes)
        put("sessionSettings", sessionSettings.toJson())
    }

    private fun JSONObject.toAlarm(): Alarm {
        val daysArray = optJSONArray("days") ?: JSONArray()
        val days = (0 until daysArray.length()).mapNotNull {
            daysArray.optInt(it, -1).takeIf { d -> d in 1..7 }
        }.toSet().ifEmpty { setOf(1, 2, 3, 4, 5, 6, 7) }

        return Alarm(
            id = optLong("id", System.currentTimeMillis()),
            hour = optInt("hour", 7),
            minute = optInt("minute", 0),
            enabled = optBoolean("enabled", true),
            days = days,
            snoozeEnabled = optBoolean("snoozeEnabled", true),
            snoozeDurationMinutes = optInt("snoozeDurationMinutes", 5),
            sessionSettings = optJSONObject("sessionSettings")?.toSessionSettings() ?: SessionSettings()
        )
    }

    // JSON serialization/deserialization for SavedSession
    private fun SavedSession.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("sessionSettings", sessionSettings.toJson())
    }

    private fun JSONObject.toSavedSession(): SavedSession = SavedSession(
        id = optLong("id", System.currentTimeMillis()),
        name = optString("name", "Untitled Session"),
        sessionSettings = optJSONObject("sessionSettings")?.toSessionSettings() ?: SessionSettings()
    )

    // Parse alarms from JSON
    private fun parseAlarms(json: String?): List<Alarm> {
        if (json.isNullOrEmpty()) return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { array.getJSONObject(it).toAlarm() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Parse saved sessions from JSON
    private fun parseSavedSessions(json: String?): List<SavedSession> {
        if (json.isNullOrEmpty()) return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { array.getJSONObject(it).toSavedSession() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Parse last session settings from JSON
    private fun parseLastSessionSettings(json: String?): SessionSettings? {
        if (json.isNullOrEmpty()) return null
        return try {
            JSONObject(json).toSessionSettings()
        } catch (e: Exception) {
            null
        }
    }

    // Migrate from old single-alarm format to new multi-alarm format
    private fun migrateFromLegacy(preferences: Preferences): Pair<List<Alarm>, SessionSettings?> {
        // Check if there's legacy data and no alarms yet
        val alarmsJson = preferences[PreferencesKeys.ALARMS_JSON]
        if (!alarmsJson.isNullOrEmpty()) {
            return Pair(parseAlarms(alarmsJson), null)
        }

        val legacyEnabled = preferences[PreferencesKeys.ALARM_ENABLED]
        if (legacyEnabled == null) {
            // No legacy data
            return Pair(emptyList(), null)
        }

        // Build session settings from legacy data
        val modeString = preferences[PreferencesKeys.TRAINING_MODE]
        val trainingMode = when (modeString) {
            "TRAINING", "INTENSE" -> TrainingMode.TRAINING
            else -> TrainingMode.RELAXATION
        }

        val legacyDaysString = preferences[PreferencesKeys.ALARM_DAYS]
        val legacyDays = if (legacyDaysString != null) {
            legacyDaysString.split(",")
                .mapNotNull { it.trim().toIntOrNull() }
                .filter { it in 1..7 }
                .toSet()
        } else {
            setOf(1, 2, 3, 4, 5, 6, 7)
        }

        val legacyPFactor = preferences[PreferencesKeys.MANUAL_P_FACTOR]?.toFloatOrNull() ?: 1.4f

        val sessionSettings = SessionSettings(
            trainingMode = trainingMode,
            useManualIntervalSettings = preferences[PreferencesKeys.USE_MANUAL_INTERVAL_SETTINGS] ?: false,
            manualBreathHoldDurationSeconds = preferences[PreferencesKeys.MANUAL_BREATH_HOLD_DURATION] ?: 36,
            manualR0Seconds = preferences[PreferencesKeys.MANUAL_R0_SECONDS] ?: 45,
            manualRnSeconds = preferences[PreferencesKeys.MANUAL_RN_SECONDS] ?: 9,
            manualNumberOfIntervals = preferences[PreferencesKeys.MANUAL_NUMBER_OF_INTERVALS] ?: 6,
            manualPFactor = legacyPFactor,
            introBowlVolumeMultiplier = preferences[PreferencesKeys.INTRO_BOWL_VOLUME_MULTIPLIER] ?: 10,
            breathChimeVolumeMultiplier = preferences[PreferencesKeys.BREATH_CHIME_VOLUME_MULTIPLIER] ?: 10,
            holdChimeVolumeMultiplier = preferences[PreferencesKeys.HOLD_CHIME_VOLUME_MULTIPLIER] ?: 10,
            customIntroBowlUri = preferences[PreferencesKeys.CUSTOM_INTRO_BOWL_URI],
            customBreathChimeUri = preferences[PreferencesKeys.CUSTOM_BREATH_CHIME_URI],
            customHoldChimeUri = preferences[PreferencesKeys.CUSTOM_HOLD_CHIME_URI],
            fadeInIntroBowl = preferences[PreferencesKeys.FADE_IN_INTRO_BOWL] ?: true
        )

        val alarm = Alarm(
            id = 1L,  // Fixed ID for migrated alarm
            hour = preferences[PreferencesKeys.ALARM_HOUR] ?: 7,
            minute = preferences[PreferencesKeys.ALARM_MINUTE] ?: 0,
            enabled = legacyEnabled,
            days = legacyDays,
            snoozeEnabled = true,
            snoozeDurationMinutes = preferences[PreferencesKeys.SNOOZE_DURATION_MINUTES] ?: 5,
            sessionSettings = sessionSettings
        )

        return Pair(listOf(alarm), sessionSettings)
    }

    // User preferences flow (simplified - just global M and setup flag)
    val userPreferences: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        val lastSessionJson = preferences[PreferencesKeys.LAST_SESSION_JSON]
        UserPreferences(
            maxStaticBreathHoldDurationSeconds = preferences[PreferencesKeys.MAX_STATIC_BREATH_HOLD_DURATION] ?: 60,
            isFirstTimeSetupComplete = preferences[PreferencesKeys.FIRST_TIME_SETUP_COMPLETE] ?: false,
            lastSessionSettings = parseLastSessionSettings(lastSessionJson)
        )
    }

    // Alarms flow
    val alarmsFlow: Flow<List<Alarm>> = context.dataStore.data.map { preferences ->
        val (alarms, _) = migrateFromLegacy(preferences)
        alarms
    }

    // Saved sessions flow
    val savedSessionsFlow: Flow<List<SavedSession>> = context.dataStore.data.map { preferences ->
        parseSavedSessions(preferences[PreferencesKeys.SAVED_SESSIONS_JSON])
    }

    // Get alarm by ID
    suspend fun getAlarmById(id: Long): Alarm? {
        var result: Alarm? = null
        context.dataStore.edit { preferences ->
            val (alarms, _) = migrateFromLegacy(preferences)
            result = alarms.find { it.id == id }
        }
        return result
    }

    // Add or update alarm
    suspend fun saveAlarm(alarm: Alarm) {
        context.dataStore.edit { preferences ->
            val (existingAlarms, migratedSettings) = migrateFromLegacy(preferences)

            // If migrating and this is a new save, also save the alarms to JSON
            val alarms = existingAlarms.toMutableList()
            val index = alarms.indexOfFirst { it.id == alarm.id }
            if (index >= 0) {
                alarms[index] = alarm
            } else {
                alarms.add(alarm)
            }

            val jsonArray = JSONArray()
            alarms.forEach { jsonArray.put(it.toJson()) }
            preferences[PreferencesKeys.ALARMS_JSON] = jsonArray.toString()

            // Clear legacy keys after migration
            if (migratedSettings != null) {
                preferences.remove(PreferencesKeys.ALARM_HOUR)
                preferences.remove(PreferencesKeys.ALARM_MINUTE)
                preferences.remove(PreferencesKeys.ALARM_ENABLED)
                preferences.remove(PreferencesKeys.ALARM_DAYS)
                preferences.remove(PreferencesKeys.TRAINING_MODE)
                preferences.remove(PreferencesKeys.USE_MANUAL_INTERVAL_SETTINGS)
                preferences.remove(PreferencesKeys.MANUAL_BREATH_HOLD_DURATION)
                preferences.remove(PreferencesKeys.MANUAL_R0_SECONDS)
                preferences.remove(PreferencesKeys.MANUAL_RN_SECONDS)
                preferences.remove(PreferencesKeys.MANUAL_NUMBER_OF_INTERVALS)
                preferences.remove(PreferencesKeys.MANUAL_P_FACTOR)
                preferences.remove(PreferencesKeys.INTRO_BOWL_VOLUME_MULTIPLIER)
                preferences.remove(PreferencesKeys.BREATH_CHIME_VOLUME_MULTIPLIER)
                preferences.remove(PreferencesKeys.HOLD_CHIME_VOLUME_MULTIPLIER)
                preferences.remove(PreferencesKeys.CUSTOM_INTRO_BOWL_URI)
                preferences.remove(PreferencesKeys.CUSTOM_BREATH_CHIME_URI)
                preferences.remove(PreferencesKeys.CUSTOM_HOLD_CHIME_URI)
                preferences.remove(PreferencesKeys.FADE_IN_INTRO_BOWL)
                preferences.remove(PreferencesKeys.SNOOZE_DURATION_MINUTES)
            }
        }
    }

    // Delete alarm
    suspend fun deleteAlarm(alarmId: Long) {
        context.dataStore.edit { preferences ->
            val (existingAlarms, _) = migrateFromLegacy(preferences)
            val alarms = existingAlarms.filter { it.id != alarmId }

            val jsonArray = JSONArray()
            alarms.forEach { jsonArray.put(it.toJson()) }
            preferences[PreferencesKeys.ALARMS_JSON] = jsonArray.toString()
        }
    }

    // Update alarm enabled state
    suspend fun updateAlarmEnabled(alarmId: Long, enabled: Boolean) {
        context.dataStore.edit { preferences ->
            val (existingAlarms, _) = migrateFromLegacy(preferences)
            val alarms = existingAlarms.map {
                if (it.id == alarmId) it.copy(enabled = enabled) else it
            }

            val jsonArray = JSONArray()
            alarms.forEach { jsonArray.put(it.toJson()) }
            preferences[PreferencesKeys.ALARMS_JSON] = jsonArray.toString()
        }
    }

    // Save session
    suspend fun saveSession(session: SavedSession) {
        context.dataStore.edit { preferences ->
            val existingSessions = parseSavedSessions(preferences[PreferencesKeys.SAVED_SESSIONS_JSON]).toMutableList()
            val index = existingSessions.indexOfFirst { it.id == session.id }
            if (index >= 0) {
                existingSessions[index] = session
            } else {
                existingSessions.add(session)
            }

            val jsonArray = JSONArray()
            existingSessions.forEach { jsonArray.put(it.toJson()) }
            preferences[PreferencesKeys.SAVED_SESSIONS_JSON] = jsonArray.toString()
        }
    }

    // Delete saved session
    suspend fun deleteSavedSession(sessionId: Long) {
        context.dataStore.edit { preferences ->
            val existingSessions = parseSavedSessions(preferences[PreferencesKeys.SAVED_SESSIONS_JSON])
                .filter { it.id != sessionId }

            val jsonArray = JSONArray()
            existingSessions.forEach { jsonArray.put(it.toJson()) }
            preferences[PreferencesKeys.SAVED_SESSIONS_JSON] = jsonArray.toString()
        }
    }

    // Update last session settings (for manual sessions)
    suspend fun updateLastSession(settings: SessionSettings) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_SESSION_JSON] = settings.toJson().toString()
        }
    }

    // Clear last session
    suspend fun clearLastSession() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.LAST_SESSION_JSON)
        }
    }

    // Update global M
    suspend fun updateMaxStaticBreathHoldDuration(durationSeconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MAX_STATIC_BREATH_HOLD_DURATION] = durationSeconds
        }
    }

    // Complete first time setup
    suspend fun completeFirstTimeSetup() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FIRST_TIME_SETUP_COMPLETE] = true
        }
    }

    // Complete setup with training mode and max breath hold
    @Suppress("UNUSED_PARAMETER")
    suspend fun completeSetup(trainingMode: TrainingMode, maxStaticBreathHold: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MAX_STATIC_BREATH_HOLD_DURATION] = maxStaticBreathHold
            preferences[PreferencesKeys.FIRST_TIME_SETUP_COMPLETE] = true
        }
    }

    // Helper function to serialize session settings to JSON string (for Intent extras)
    fun sessionSettingsToJson(settings: SessionSettings): String = settings.toJson().toString()

    // Helper function to deserialize session settings from JSON string
    fun sessionSettingsFromJson(json: String): SessionSettings? {
        return try {
            JSONObject(json).toSessionSettings()
        } catch (e: Exception) {
            null
        }
    }
}
