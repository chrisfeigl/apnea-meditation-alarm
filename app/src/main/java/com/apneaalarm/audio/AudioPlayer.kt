package com.apneaalarm.audio

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.SystemClock
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

class AudioPlayer(private val context: Context) {

    private val soundGenerator = SoundGenerator()
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var continuousPlayJob: Job? = null

    // Use SupervisorJob so child coroutine failures don't cancel the entire scope
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        android.util.Log.e("AudioPlayer", "Coroutine exception", throwable)
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)

    // Custom sound MediaPlayers
    private var customIntroBowlPlayer: MediaPlayer? = null
    private var customBreathChimePlayer: MediaPlayer? = null
    private var customHoldChimePlayer: MediaPlayer? = null

    // Preview player (for stop functionality)
    private var previewPlayer: MediaPlayer? = null
    private var previewJob: Job? = null

    // Pause state for continuous/looping audio
    private var isPausedState = false
    private var pausedPosition: Int = 0

    // Get resource ID by name (returns 0 if not found)
    private fun getRawResourceId(name: String): Int {
        val resId = context.resources.getIdentifier(name, "raw", context.packageName)
        android.util.Log.d("AudioPlayer", "getRawResourceId($name) = $resId, package = ${context.packageName}")
        return resId
    }

    // Check if bundled audio resource exists by name
    private fun hasRawResource(name: String): Boolean {
        val has = getRawResourceId(name) != 0
        android.util.Log.d("AudioPlayer", "hasRawResource($name) = $has")
        return has
    }

    // Play a bundled raw resource by name
    private suspend fun playRawResource(name: String, volume: Float): Boolean {
        val resId = getRawResourceId(name)
        android.util.Log.d("AudioPlayer", "playRawResource($name, $volume) resId=$resId")
        if (resId == 0) return false

        return try {
            // Use AssetFileDescriptor approach instead of MediaPlayer.create
            val afd = context.resources.openRawResourceFd(resId)
            if (afd == null) {
                android.util.Log.e("AudioPlayer", "Failed to open raw resource fd for $name")
                return false
            }

            val player = MediaPlayer()
            player.setAudioStreamType(AudioManager.STREAM_ALARM)
            player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            player.prepare()
            player.setVolume(volume, volume)

            android.util.Log.d("AudioPlayer", "Player prepared for $name, duration=${player.duration}ms")

            // Use suspendCancellableCoroutine to wait for completion
            suspendCancellableCoroutine { continuation ->
                player.setOnCompletionListener {
                    android.util.Log.d("AudioPlayer", "Completed playing $name")
                    player.release()
                    if (continuation.isActive) {
                        continuation.resume(Unit) {}
                    }
                }
                player.setOnErrorListener { _, what, extra ->
                    android.util.Log.e("AudioPlayer", "Error playing $name: what=$what, extra=$extra")
                    player.release()
                    if (continuation.isActive) {
                        continuation.resume(Unit) {}
                    }
                    true
                }
                continuation.invokeOnCancellation {
                    player.release()
                }
                player.start()
                android.util.Log.d("AudioPlayer", "Started playing $name")
            }
            true
        } catch (e: Exception) {
            android.util.Log.e("AudioPlayer", "Failed to play raw resource: $name", e)
            false
        }
    }

    // Create a looping MediaPlayer for a raw resource by name
    private fun createLoopingPlayer(name: String): MediaPlayer? {
        val resId = getRawResourceId(name)
        android.util.Log.d("AudioPlayer", "createLoopingPlayer($name) resId=$resId")
        if (resId == 0) return null

        return try {
            val afd = context.resources.openRawResourceFd(resId)
            if (afd == null) {
                android.util.Log.e("AudioPlayer", "createLoopingPlayer: Failed to open raw resource fd")
                return null
            }

            val player = MediaPlayer()
            player.setAudioStreamType(AudioManager.STREAM_ALARM)
            player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            player.prepare()
            player.isLooping = true
            android.util.Log.d("AudioPlayer", "createLoopingPlayer: prepared, duration=${player.duration}ms")
            player
        } catch (e: Exception) {
            android.util.Log.e("AudioPlayer", "createLoopingPlayer failed", e)
            null
        }
    }

    // Get system alarm volume as a fraction (0.0 to 1.0)
    private fun getSystemAlarmVolumeFraction(): Float {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
        return if (maxVolume > 0) currentVolume.toFloat() / maxVolume.toFloat() else 1.0f
    }

    // Calculate actual volume from multiplier (1-10) and system volume
    private fun calculateVolume(multiplier: Int): Float {
        val systemVolume = getSystemAlarmVolumeFraction()
        val multiplierFraction = multiplier.coerceIn(1, 10) / 10.0f
        return systemVolume * multiplierFraction
    }

    /**
     * Play intro bowl with optional fade-in/hold/fade-out envelope
     * If fadeIn=true: 0-48s fade in, 48-51s hold, 51-54s fade out
     * If fadeIn=false: play at full volume for 54 seconds, then stop
     */
    suspend fun playIntroBowl(
        volumeMultiplier: Int,
        customSoundUri: String? = null,
        fadeIn: Boolean = true,
        isPausedCheck: () -> Boolean = { false },
        onPauseWait: suspend () -> Unit = {},
        onProgress: (elapsedSeconds: Int) -> Unit = {}
    ) {
        val maxVolume = calculateVolume(volumeMultiplier)

        if (customSoundUri != null) {
            playCustomIntroBowl(customSoundUri, maxVolume, fadeIn, isPausedCheck, onPauseWait, onProgress)
        } else if (hasRawResource("bowl")) {
            // Use bundled bowl sound
            playBundledIntroBowl("bowl", maxVolume, fadeIn, isPausedCheck, onPauseWait, onProgress)
        } else {
            soundGenerator.playIntroBowlSequence(maxVolume, onProgress)
        }
    }

    private suspend fun playBundledIntroBowl(
        resourceName: String,
        maxVolume: Float,
        fadeIn: Boolean,
        isPausedCheck: () -> Boolean,
        onPauseWait: suspend () -> Unit,
        onProgress: (elapsedSeconds: Int) -> Unit
    ) {
        try {
            customIntroBowlPlayer?.release()
            customIntroBowlPlayer = createLoopingPlayer(resourceName)

            customIntroBowlPlayer?.let { player ->
                try {
                    val startTime = SystemClock.elapsedRealtime()
                    var pausedDuration = 0L
                    var lastElapsed = -1

                    if (fadeIn) {
                        // Fade-in mode
                        player.setVolume(0f, 0f)
                        player.start()

                        // Use real time for 54 second sequence
                        while (scope.isActive) {
                            if (isPausedCheck()) {
                                val pauseStart = SystemClock.elapsedRealtime()
                                onPauseWait()
                                pausedDuration += SystemClock.elapsedRealtime() - pauseStart
                            }
                            if (!scope.isActive) break

                            val elapsed = ((SystemClock.elapsedRealtime() - startTime - pausedDuration) / 1000).toInt()
                            if (elapsed >= 54) break

                            if (elapsed != lastElapsed) {
                                lastElapsed = elapsed
                                onProgress(elapsed)

                                // Update volume based on phase
                                val volume = when {
                                    elapsed < 48 -> {
                                        // Fade in with accelerated curve (sqrt)
                                        val progress = elapsed / 48.0
                                        (kotlin.math.sqrt(progress) * maxVolume).toFloat()
                                    }
                                    elapsed < 51 -> maxVolume // Hold at max
                                    else -> {
                                        // Fade out
                                        val fadeProgress = (elapsed - 51) / 3.0f
                                        maxVolume * (1.0f - fadeProgress)
                                    }
                                }
                                if (!isPausedCheck()) {
                                    try { player.setVolume(volume, volume) } catch (e: Exception) { /* ignore */ }
                                }
                            }
                            delay(100)
                        }
                    } else {
                        // No fade - play at full volume for 54 seconds
                        player.setVolume(maxVolume, maxVolume)
                        player.start()

                        while (scope.isActive) {
                            if (isPausedCheck()) {
                                val pauseStart = SystemClock.elapsedRealtime()
                                onPauseWait()
                                pausedDuration += SystemClock.elapsedRealtime() - pauseStart
                            }
                            if (!scope.isActive) break

                            val elapsed = ((SystemClock.elapsedRealtime() - startTime - pausedDuration) / 1000).toInt()
                            if (elapsed >= 54) break

                            if (elapsed != lastElapsed) {
                                lastElapsed = elapsed
                                onProgress(elapsed)
                            }
                            delay(100)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AudioPlayer", "Error during bundled intro bowl playback", e)
                } finally {
                    try {
                        player.stop()
                        player.release()
                    } catch (e: Exception) { /* ignore */ }
                    customIntroBowlPlayer = null
                }
            }
        } catch (e: Exception) {
            // Fall back to synthesized sound
            soundGenerator.playIntroBowlSequence(maxVolume, onProgress)
        }
    }

    private suspend fun playCustomIntroBowl(
        uri: String,
        maxVolume: Float,
        fadeIn: Boolean,
        isPausedCheck: () -> Boolean,
        onPauseWait: suspend () -> Unit,
        onProgress: (elapsedSeconds: Int) -> Unit
    ) {
        try {
            customIntroBowlPlayer?.release()
            customIntroBowlPlayer = MediaPlayer().apply {
                setDataSource(context, Uri.parse(uri))
                setAudioStreamType(AudioManager.STREAM_ALARM)
                isLooping = true
                prepare()
            }

            customIntroBowlPlayer?.let { player ->
                try {
                    val startTime = SystemClock.elapsedRealtime()
                    var pausedDuration = 0L
                    var lastElapsed = -1

                    if (fadeIn) {
                        // Fade-in mode
                        player.setVolume(0f, 0f)
                        player.start()

                        // Use real time for 54 second sequence
                        while (scope.isActive) {
                            if (isPausedCheck()) {
                                val pauseStart = SystemClock.elapsedRealtime()
                                onPauseWait()
                                pausedDuration += SystemClock.elapsedRealtime() - pauseStart
                            }
                            if (!scope.isActive) break

                            val elapsed = ((SystemClock.elapsedRealtime() - startTime - pausedDuration) / 1000).toInt()
                            if (elapsed >= 54) break

                            if (elapsed != lastElapsed) {
                                lastElapsed = elapsed
                                onProgress(elapsed)

                                // Update volume based on phase
                                val volume = when {
                                    elapsed < 48 -> {
                                        // Fade in with accelerated curve (sqrt)
                                        val progress = elapsed / 48.0
                                        (kotlin.math.sqrt(progress) * maxVolume).toFloat()
                                    }
                                    elapsed < 51 -> maxVolume // Hold at max
                                    else -> {
                                        // Fade out
                                        val fadeProgress = (elapsed - 51) / 3.0f
                                        maxVolume * (1.0f - fadeProgress)
                                    }
                                }
                                if (!isPausedCheck()) {
                                    try { player.setVolume(volume, volume) } catch (e: Exception) { /* ignore */ }
                                }
                            }
                            delay(100)
                        }
                    } else {
                        // No fade - play at full volume for 54 seconds
                        player.setVolume(maxVolume, maxVolume)
                        player.start()

                        while (scope.isActive) {
                            if (isPausedCheck()) {
                                val pauseStart = SystemClock.elapsedRealtime()
                                onPauseWait()
                                pausedDuration += SystemClock.elapsedRealtime() - pauseStart
                            }
                            if (!scope.isActive) break

                            val elapsed = ((SystemClock.elapsedRealtime() - startTime - pausedDuration) / 1000).toInt()
                            if (elapsed >= 54) break

                            if (elapsed != lastElapsed) {
                                lastElapsed = elapsed
                                onProgress(elapsed)
                            }
                            delay(100)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AudioPlayer", "Error during custom intro bowl playback", e)
                } finally {
                    try {
                        player.stop()
                        player.release()
                    } catch (e: Exception) { /* ignore */ }
                    customIntroBowlPlayer = null
                }
            }
        } catch (e: Exception) {
            // Fall back to synthesized sound
            soundGenerator.playIntroBowlSequence(maxVolume, onProgress)
        }
    }

    /**
     * Play a preview of the intro bowl sound (full audio file)
     * Returns a callback to be notified when playback completes
     */
    fun playIntroBowlPreview(volumeMultiplier: Int, customSoundUri: String? = null, onComplete: () -> Unit = {}) {
        stopPreview()
        val volume = calculateVolume(volumeMultiplier)

        previewJob = scope.launch {
            if (customSoundUri != null) {
                try {
                    previewPlayer = MediaPlayer().apply {
                        setDataSource(context, Uri.parse(customSoundUri))
                        setAudioStreamType(AudioManager.STREAM_ALARM)
                        setVolume(volume, volume)
                        prepare()
                    }
                    previewPlayer?.let { player ->
                        player.setOnCompletionListener {
                            player.release()
                            previewPlayer = null
                            onComplete()
                        }
                        player.start()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AudioPlayer", "Failed to preview custom intro bowl: $customSoundUri", e)
                    playDefaultBowlPreview(volume, onComplete)
                }
            } else {
                playDefaultBowlPreview(volume, onComplete)
            }
        }
    }

    private fun playDefaultBowlPreview(volume: Float, onComplete: () -> Unit) {
        android.util.Log.d("AudioPlayer", "playDefaultBowlPreview volume=$volume")
        val resId = getRawResourceId("bowl")
        if (resId != 0) {
            try {
                val afd = context.resources.openRawResourceFd(resId)
                if (afd == null) {
                    android.util.Log.w("AudioPlayer", "playDefaultBowlPreview: failed to open resource")
                    onComplete()
                    return
                }

                previewPlayer = MediaPlayer()
                previewPlayer?.apply {
                    setAudioStreamType(AudioManager.STREAM_ALARM)
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                    prepare()
                    setVolume(volume, volume)
                    setOnCompletionListener {
                        release()
                        previewPlayer = null
                        onComplete()
                    }
                    start()
                    android.util.Log.d("AudioPlayer", "playDefaultBowlPreview: started, duration=${duration}ms")
                }
            } catch (e: Exception) {
                android.util.Log.e("AudioPlayer", "playDefaultBowlPreview failed", e)
                onComplete()
            }
        } else {
            android.util.Log.w("AudioPlayer", "playDefaultBowlPreview: no resource")
            onComplete()
        }
    }

    /**
     * Play a preview of the hold chime sound (full audio file)
     */
    fun playHoldChimePreview(volumeMultiplier: Int, customSoundUri: String? = null, onComplete: () -> Unit = {}) {
        stopPreview()
        val volume = calculateVolume(volumeMultiplier)

        previewJob = scope.launch {
            if (customSoundUri != null) {
                val success = playCustomPreview(customSoundUri, volume, onComplete)
                if (!success) {
                    playDefaultChimePreview("chime_hold", volume, SoundGenerator.SoundType.LOW_GONG, onComplete)
                }
            } else {
                playDefaultChimePreview("chime_hold", volume, SoundGenerator.SoundType.LOW_GONG, onComplete)
            }
        }
    }

    /**
     * Play a preview of the breath chime sound (full audio file)
     */
    fun playBreathChimePreview(volumeMultiplier: Int, customSoundUri: String? = null, onComplete: () -> Unit = {}) {
        stopPreview()
        val volume = calculateVolume(volumeMultiplier)

        previewJob = scope.launch {
            if (customSoundUri != null) {
                val success = playCustomPreview(customSoundUri, volume, onComplete)
                if (!success) {
                    playDefaultChimePreview("chime_breath", volume, SoundGenerator.SoundType.HIGH_GONG, onComplete)
                }
            } else {
                playDefaultChimePreview("chime_breath", volume, SoundGenerator.SoundType.HIGH_GONG, onComplete)
            }
        }
    }

    private fun playCustomPreview(uri: String, volume: Float, onComplete: () -> Unit): Boolean {
        return try {
            previewPlayer = MediaPlayer().apply {
                setDataSource(context, Uri.parse(uri))
                setAudioStreamType(AudioManager.STREAM_ALARM)
                setVolume(volume, volume)
                prepare()
            }
            previewPlayer?.let { player ->
                player.setOnCompletionListener {
                    player.release()
                    previewPlayer = null
                    onComplete()
                }
                player.start()
            }
            true
        } catch (e: Exception) {
            android.util.Log.e("AudioPlayer", "Failed to play custom preview: $uri", e)
            false
        }
    }

    private fun playDefaultChimePreview(resourceName: String, volume: Float, fallbackType: SoundGenerator.SoundType, onComplete: () -> Unit) {
        val resId = getRawResourceId(resourceName)
        if (resId != 0) {
            try {
                val afd = context.resources.openRawResourceFd(resId)
                if (afd == null) {
                    android.util.Log.w("AudioPlayer", "playDefaultChimePreview: failed to open resource $resourceName")
                    onComplete()
                    return
                }

                previewPlayer = MediaPlayer()
                previewPlayer?.apply {
                    setAudioStreamType(AudioManager.STREAM_ALARM)
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                    prepare()
                    setVolume(volume, volume)
                    setOnCompletionListener {
                        release()
                        previewPlayer = null
                        onComplete()
                    }
                    start()
                    android.util.Log.d("AudioPlayer", "playDefaultChimePreview: started $resourceName, duration=${duration}ms")
                }
            } catch (e: Exception) {
                android.util.Log.e("AudioPlayer", "playDefaultChimePreview failed for $resourceName", e)
                onComplete()
            }
        } else {
            android.util.Log.w("AudioPlayer", "playDefaultChimePreview: no resource $resourceName")
            onComplete()
        }
    }

    /**
     * Stop any currently playing preview
     */
    fun stopPreview() {
        previewJob?.cancel()
        previewJob = null
        try {
            previewPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            }
        } catch (e: Exception) {
            android.util.Log.e("AudioPlayer", "Error stopping preview", e)
        }
        previewPlayer = null
    }

    /**
     * Check if preview is currently playing
     */
    fun isPreviewPlaying(): Boolean {
        return try {
            previewPlayer?.isPlaying == true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Play a single breath chime (signals breathing)
     */
    suspend fun playBreathChime(volumeMultiplier: Int, customSoundUri: String? = null) {
        val volume = calculateVolume(volumeMultiplier)

        if (customSoundUri != null) {
            val success = playCustomSound(customSoundUri, volume)
            if (!success) {
                playDefaultBreathChime(volume)
            }
        } else {
            playDefaultBreathChime(volume)
        }
    }

    private suspend fun playDefaultBreathChime(volume: Float) {
        // Try bundled audio first, fall back to synthesized
        if (hasRawResource("chime_breath")) {
            val success = playRawResource("chime_breath", volume)
            if (!success) {
                soundGenerator.playSound(SoundGenerator.SoundType.HIGH_GONG, volume)
            }
        } else {
            soundGenerator.playSound(SoundGenerator.SoundType.HIGH_GONG, volume)
        }
    }

    /**
     * Play a single hold chime (signals breath hold)
     */
    suspend fun playHoldChime(volumeMultiplier: Int, customSoundUri: String? = null) {
        val volume = calculateVolume(volumeMultiplier)

        if (customSoundUri != null) {
            val success = playCustomSound(customSoundUri, volume)
            if (!success) {
                playDefaultHoldChime(volume)
            }
        } else {
            playDefaultHoldChime(volume)
        }
    }

    private suspend fun playDefaultHoldChime(volume: Float) {
        // Try bundled audio first, fall back to synthesized
        if (hasRawResource("chime_hold")) {
            val success = playRawResource("chime_hold", volume)
            if (!success) {
                soundGenerator.playSound(SoundGenerator.SoundType.LOW_GONG, volume)
            }
        } else {
            soundGenerator.playSound(SoundGenerator.SoundType.LOW_GONG, volume)
        }
    }

    /**
     * Returns true if custom sound played successfully, false otherwise
     */
    private suspend fun playCustomSound(uri: String, volume: Float): Boolean {
        return try {
            val player = MediaPlayer().apply {
                setDataSource(context, Uri.parse(uri))
                setAudioStreamType(AudioManager.STREAM_ALARM)
                setVolume(volume, volume)
                prepare()
            }
            player.start()

            // Wait for playback to complete
            while (player.isPlaying) {
                delay(100)
            }

            player.release()
            true
        } catch (e: Exception) {
            android.util.Log.e("AudioPlayer", "Failed to play custom sound: $uri", e)
            false
        }
    }

    /**
     * Play continuous bowl sound until stopped (for session completion)
     */
    fun startContinuousBowl(volumeMultiplier: Int, customSoundUri: String? = null) {
        stopContinuousBowl()
        val volume = calculateVolume(volumeMultiplier)

        continuousPlayJob = scope.launch {
            if (customSoundUri != null) {
                // Try custom sound first, fall back to bundled/synthesized if it fails
                val success = playCustomSound(customSoundUri, volume)
                if (success) {
                    // Custom sound worked, continue looping it
                    while (isActive) {
                        playCustomSound(customSoundUri, volume)
                        delay(500)
                    }
                } else {
                    playDefaultContinuousBowl(volume)
                }
            } else {
                playDefaultContinuousBowl(volume)
            }
        }
    }

    private suspend fun playDefaultContinuousBowl(volume: Float) {
        if (hasRawResource("bowl")) {
            // Loop bundled bowl
            while (scope.isActive) {
                val success = playRawResource("bowl", volume)
                if (!success) {
                    // Fall back to synthesized if bundled fails
                    soundGenerator.playSound(SoundGenerator.SoundType.BOWL, volume)
                }
                delay(500)
            }
        } else {
            // Loop synthesized bowl
            while (scope.isActive) {
                soundGenerator.playSound(SoundGenerator.SoundType.BOWL, volume)
                delay(500)
            }
        }
    }

    fun stopContinuousBowl() {
        continuousPlayJob?.cancel()
        continuousPlayJob = null
    }

    /**
     * Play continuous hold chime at max volume until stopped (for session completion escalation)
     */
    fun startContinuousHoldChime(customSoundUri: String? = null) {
        stopContinuousBowl()
        val volume = 1.0f  // Max volume

        continuousPlayJob = scope.launch {
            while (isActive) {
                if (customSoundUri != null) {
                    val success = playCustomSound(customSoundUri, volume)
                    if (!success) {
                        playDefaultHoldChimeOnce(volume)
                    }
                } else {
                    playDefaultHoldChimeOnce(volume)
                }
                delay(2000)  // 2 second gap between chimes
            }
        }
    }

    private suspend fun playDefaultHoldChimeOnce(volume: Float) {
        if (hasRawResource("chime_hold")) {
            val success = playRawResource("chime_hold", volume)
            if (!success) {
                soundGenerator.playSound(SoundGenerator.SoundType.LOW_GONG, volume)
            }
        } else {
            soundGenerator.playSound(SoundGenerator.SoundType.LOW_GONG, volume)
        }
    }

    /**
     * Pause all currently playing audio
     */
    fun pauseAudio() {
        isPausedState = true
        try {
            customIntroBowlPlayer?.let { player ->
                if (player.isPlaying) {
                    pausedPosition = player.currentPosition
                    player.pause()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AudioPlayer", "Error pausing audio", e)
        }
        // Stop continuous bowl when pausing
        continuousPlayJob?.cancel()
        continuousPlayJob = null
    }

    /**
     * Resume paused audio
     */
    fun resumeAudio() {
        isPausedState = false
        try {
            customIntroBowlPlayer?.let { player ->
                player.seekTo(pausedPosition)
                player.start()
            }
        } catch (e: Exception) {
            android.util.Log.e("AudioPlayer", "Error resuming audio", e)
        }
    }

    fun release() {
        stopContinuousBowl()
        stopPreview()
        isPausedState = false
        pausedPosition = 0
        customIntroBowlPlayer?.release()
        customBreathChimePlayer?.release()
        customHoldChimePlayer?.release()
        customIntroBowlPlayer = null
        customBreathChimePlayer = null
        customHoldChimePlayer = null
    }
}
