package com.focusfirst.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import androidx.preference.PreferenceManager
import com.focusfirst.data.repository.FocusGuardRepository
import com.focusfirst.ui.screens.BlockedActivity
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.focusfirst.MainActivity
import com.focusfirst.R
import com.focusfirst.data.model.IntervalPreset
import com.focusfirst.data.model.TimerMode
import com.focusfirst.data.model.TimerPhase
import com.focusfirst.receiver.BootReceiver
import com.focusfirst.service.SoundManager
import com.focusfirst.util.DndManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

// ---------------------------------------------------------------------------
// TimerForegroundService
//
// Two-layer reliability model:
//   Layer 1 – This ForegroundService owns the live countdown coroutine and
//              broadcasts second-by-second ticks to the UI.
//   Layer 2 – WorkManager (TimerAlarmWorker) acts as a watchdog: if the
//              process is killed it reschedules completion.
//
// Also supports Flow mode (ACTION_START_FLOW) — count-up timer with no
// automatic phase transitions.
// ---------------------------------------------------------------------------

@AndroidEntryPoint
class TimerForegroundService : Service() {

    @Inject lateinit var soundManager: SoundManager
    @Inject lateinit var dndManager: DndManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var timerJob: Job? = null

    // ── Timer state ───────────────────────────────────────────────────────────
    private var remainingSeconds: Int    = 0
    private var totalSeconds: Int        = 0
    private var currentPhase: TimerPhase = TimerPhase.FOCUS
    private var isRunning: Boolean       = false
    private var endTimeMs: Long          = 0L
    private var sessionStartedAtMs: Long = 0L
    private var runStartedAtMs: Long     = 0L
    private var timerMode: TimerMode     = TimerMode.POMODORO
    private var preset: IntervalPreset   = IntervalPreset.CLASSIC

    // ── Flow mode ─────────────────────────────────────────────────────────────
    private var isFlowMode:     Boolean = false
    private var elapsedSeconds: Int     = 0
    private var elapsedBaseSeconds: Int = 0

    private var wakeLock: PowerManager.WakeLock? = null

    private val notificationManager: NotificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private val localBroadcastManager: LocalBroadcastManager by lazy {
        LocalBroadcastManager.getInstance(this)
    }

    private val completionPrefs by lazy {
        getSharedPreferences(FOCUS_FIRST_PREFS, Context.MODE_PRIVATE)
    }

    companion object {
        const val FOCUS_FIRST_PREFS    = "focusfirst_prefs"
        const val PREF_VIBRATE_ENABLED = "vibrate_enabled"
        const val PREF_SOUND_TYPE      = "sound_type"

        const val ACTION_START      = "com.focusfirst.ACTION_START"
        const val ACTION_START_FLOW = "com.focusfirst.ACTION_START_FLOW"
        const val ACTION_PAUSE      = "com.focusfirst.ACTION_PAUSE"
        const val ACTION_RESUME     = "com.focusfirst.ACTION_RESUME"
        const val ACTION_STOP       = "com.focusfirst.ACTION_STOP"
        const val ACTION_SKIP       = "com.focusfirst.ACTION_SKIP"

        const val BROADCAST_TICK     = "com.focusfirst.TICK"
        const val BROADCAST_FINISHED = "com.focusfirst.FINISHED"

        const val EXTRA_DURATION_SECONDS  = "extra_duration_seconds"
        const val EXTRA_PHASE             = "extra_phase"
        const val EXTRA_REMAINING_SECONDS = "extra_remaining_seconds"
        const val EXTRA_ELAPSED_SECONDS   = "extra_elapsed_seconds"
        const val EXTRA_IS_RUNNING        = "extra_is_running"
        const val EXTRA_IS_FLOW_MODE      = "extra_is_flow_mode"
        const val EXTRA_IS_STOPPED        = "extra_is_stopped"
        const val EXTRA_TOTAL_SECONDS     = "extra_total_seconds"
        const val EXTRA_SESSION_STARTED_AT_MS = "extra_session_started_at_ms"
        const val EXTRA_PRESET            = "extra_preset"
        const val EXTRA_TIMER_MODE        = "extra_timer_mode"
        const val EXTRA_END_TIME_MS       = "extra_end_time_ms"

        const val NOTIFICATION_ID    = 1001
        const val NOTIFY_ID_COMPLETE = 1003
        const val CHANNEL_ID         = "focusfirst_timer"
        private const val CHANNEL_ALERTS = "focusfirst_alerts"

        private const val TAG = "TimerForegroundService"

        fun buildStartIntent(
            context:         Context,
            durationSeconds: Int,
            phase:           TimerPhase,
            totalSeconds: Int = durationSeconds,
            sessionStartedAtMs: Long = System.currentTimeMillis(),
            preset: IntervalPreset = IntervalPreset.CLASSIC,
            endTimeMs: Long = 0L,
        ): Intent = Intent(context, TimerForegroundService::class.java).apply {
            action = ACTION_START
            putExtra(EXTRA_DURATION_SECONDS, durationSeconds)
            putExtra(EXTRA_PHASE, phase.name)
            putExtra(EXTRA_TOTAL_SECONDS, totalSeconds)
            putExtra(EXTRA_SESSION_STARTED_AT_MS, sessionStartedAtMs)
            putExtra(EXTRA_PRESET, preset.name)
            putExtra(EXTRA_TIMER_MODE, TimerMode.POMODORO.name)
            putExtra(EXTRA_END_TIME_MS, endTimeMs)
        }

        fun buildStartFlowIntent(
            context: Context,
            elapsedSeconds: Int,
            sessionStartedAtMs: Long,
        ): Intent = Intent(context, TimerForegroundService::class.java).apply {
            action = ACTION_START_FLOW
            putExtra(EXTRA_ELAPSED_SECONDS, elapsedSeconds)
            putExtra(EXTRA_SESSION_STARTED_AT_MS, sessionStartedAtMs)
            putExtra(EXTRA_PRESET, IntervalPreset.FLOW.name)
        }

        private fun ceilSeconds(milliseconds: Long): Int =
            if (milliseconds <= 0L) 0 else ((milliseconds + 999L) / 1_000L)
                .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand action=${intent?.action}")
        when (intent?.action) {
            ACTION_START      -> handleStart(intent)
            ACTION_START_FLOW -> handleStartFlow(intent)
            ACTION_PAUSE      -> handlePause()
            ACTION_RESUME     -> handleResume()
            ACTION_STOP       -> handleStop()
            ACTION_SKIP       -> handleSkip()
            null              -> restoreAfterServiceRestart()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
        serviceScope.cancel()
        Log.d(TAG, "onDestroy — scope cancelled")
    }

    // ========================================================================
    // Handlers
    // ========================================================================

    private fun handleStart(intent: Intent) {
        val durationSeconds = intent.getIntExtra(EXTRA_DURATION_SECONDS, 25 * 60).coerceAtLeast(1)
        val phaseName       = intent.getStringExtra(EXTRA_PHASE) ?: TimerPhase.FOCUS.name
        currentPhase     = runCatching { TimerPhase.valueOf(phaseName) }.getOrDefault(TimerPhase.FOCUS)
        val now = System.currentTimeMillis()
        val requestedEndTimeMs = intent.getLongExtra(EXTRA_END_TIME_MS, 0L)
        endTimeMs = if (requestedEndTimeMs > now) requestedEndTimeMs else now + durationSeconds * 1_000L
        remainingSeconds = ceilSeconds(endTimeMs - now).coerceAtMost(durationSeconds)
        totalSeconds     = intent.getIntExtra(EXTRA_TOTAL_SECONDS, durationSeconds).coerceAtLeast(durationSeconds)
        sessionStartedAtMs = intent.getLongExtra(EXTRA_SESSION_STARTED_AT_MS, System.currentTimeMillis())
        preset = intent.getStringExtra(EXTRA_PRESET)?.let {
            runCatching { IntervalPreset.valueOf(it) }.getOrNull()
        } ?: IntervalPreset.CLASSIC
        timerMode        = TimerMode.POMODORO
        isFlowMode       = false
        isRunning        = true
        runStartedAtMs   = now

        Log.d(TAG, "handleStart phase=$currentPhase duration=${durationSeconds}s")

        saveBootPrefs(status = "RUNNING")
        startForegroundCompat()
        acquireWakeLock(durationSeconds * 1000L)
        startTimerCoroutine()
    }

    private fun handleStartFlow(intent: Intent) {
        isFlowMode       = true
        timerMode        = TimerMode.FLOW
        preset           = IntervalPreset.FLOW
        totalSeconds     = 0
        remainingSeconds = 0
        elapsedSeconds   = intent.getIntExtra(EXTRA_ELAPSED_SECONDS, 0).coerceAtLeast(0)
        elapsedBaseSeconds = elapsedSeconds
        currentPhase     = TimerPhase.FOCUS
        isRunning        = true
        sessionStartedAtMs = intent.getLongExtra(EXTRA_SESSION_STARTED_AT_MS, System.currentTimeMillis())
        runStartedAtMs   = System.currentTimeMillis()

        Log.d(TAG, "handleStartFlow")

        saveBootPrefs(status = "RUNNING")
        startForegroundCompat()
        startFlowCoroutine()
    }

    private fun handlePause() {
        Log.d(TAG, "handlePause remaining=${remainingSeconds}s elapsed=${elapsedSeconds}s")
        if (!isRunning && sessionStartedAtMs == 0L &&
            completionPrefs.getString(BootReceiver.PREF_STATUS, "") == "PAUSED"
        ) {
            stopSelf()
            return
        }
        timerJob?.cancel()
        timerJob  = null
        if (isFlowMode) {
            elapsedSeconds = elapsedAt(System.currentTimeMillis())
            elapsedBaseSeconds = elapsedSeconds
            runStartedAtMs = 0L
        } else {
            remainingSeconds = ceilSeconds(endTimeMs - System.currentTimeMillis())
            endTimeMs = 0L
        }
        isRunning = false
        saveBootPrefs(status = "PAUSED")
        PreferenceManager.getDefaultSharedPreferences(this).edit()
            .putBoolean(FocusGuardRepository.PREF_FOCUS_GUARD_ACTIVE, false)
            .putInt(FocusGuardRepository.PREF_REMAINING_SECONDS, 0)
            .apply()
        soundManager.pause()
        dndManager.disableDnd()
        updateNotification()
        if (isFlowMode) sendFlowTickBroadcast(isRunning = false)
        else sendTickBroadcast(isRunning = false)
    }

    private fun handleResume() {
        Log.d(TAG, "handleResume")
        if (isFlowMode) {
            elapsedBaseSeconds = elapsedSeconds
            runStartedAtMs = System.currentTimeMillis()
            isRunning = true
            saveBootPrefs(status = "RUNNING")
            startForegroundCompat()
            setFocusGuardForResume()
            if (currentPhase == TimerPhase.FOCUS) soundManager.resume() else soundManager.pause()
            startFlowCoroutine()
        } else {
            if (remainingSeconds <= 0) return
            endTimeMs = System.currentTimeMillis() + remainingSeconds * 1_000L
            runStartedAtMs = System.currentTimeMillis()
            isRunning = true
            saveBootPrefs(status = "RUNNING")
            startForegroundCompat()
            setFocusGuardForResume()
            if (currentPhase == TimerPhase.FOCUS) soundManager.resume() else soundManager.pause()
            startTimerCoroutine()
        }
    }

    private fun handleStop() {
        Log.d(TAG, "handleStop")
        timerJob?.cancel()
        timerJob         = null
        isRunning        = false
        remainingSeconds = 0
        totalSeconds     = 0
        isFlowMode       = false
        elapsedSeconds   = 0
        elapsedBaseSeconds = 0
        endTimeMs        = 0L
        sessionStartedAtMs = 0L
        runStartedAtMs   = 0L
        clearBootPrefs()
        PreferenceManager.getDefaultSharedPreferences(this).edit()
            .putBoolean(FocusGuardRepository.PREF_FOCUS_GUARD_ACTIVE, false)
            .putInt(FocusGuardRepository.PREF_REMAINING_SECONDS, 0)
            .apply()
        soundManager.stop()
        dndManager.disableDnd()
        sendStoppedBroadcast()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun handleSkip() {
        Log.d(TAG, "handleSkip phase=$currentPhase")
        timerJob?.cancel()
        timerJob  = null
        isRunning = false
        sendFinishedBroadcast()
        serviceScope.launch(Dispatchers.Main) { onPhaseFinished() }
    }

    // ========================================================================
    // Coroutines
    // ========================================================================

    private fun startTimerCoroutine() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            updateNotification()
            while (isActive) {
                remainingSeconds = ceilSeconds(endTimeMs - System.currentTimeMillis())
                persistRuntimeState()
                sendTickBroadcast(isRunning = true)
                updateNotification()
                if (remainingSeconds <= 0) break
                if (remainingSeconds % 2 == 0) pollFocusGuard()
                delay(minOf(1_000L, (endTimeMs - System.currentTimeMillis()).coerceAtLeast(1L)))
            }
            if (isActive) {
                isRunning = false
                remainingSeconds = 0
                persistRuntimeState()
                sendTickBroadcast(isRunning = false)
                sendFinishedBroadcast()
                withContext(Dispatchers.Main) { onPhaseFinished() }
            }
        }
    }

    private fun startFlowCoroutine() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            updateNotification()
            while (isActive) {
                elapsedSeconds = elapsedAt(System.currentTimeMillis())
                persistRuntimeState()
                sendFlowTickBroadcast(isRunning = true)
                delay(1_000L)
                updateNotification()
                if (elapsedSeconds % 2 == 0) pollFocusGuard()
            }
        }
    }

    // ========================================================================
    // Focus Guard Poller
    // ========================================================================

    private fun pollFocusGuard() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val isActive = prefs.getBoolean(FocusGuardRepository.PREF_FOCUS_GUARD_ACTIVE, false)
        if (!isActive) return

        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 1000 * 10 // Look back 10 seconds
        val events = usageStatsManager.queryEvents(startTime, endTime)
        
        val event = UsageEvents.Event()
        var foregroundApp: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                foregroundApp = event.packageName
            }
        }

        if (foregroundApp != null && foregroundApp != packageName && foregroundApp != "com.android.systemui" && foregroundApp != "android") {
            val blockedApps = prefs.getStringSet(FocusGuardRepository.PREF_BLOCKED_APPS, emptySet()) ?: emptySet()
            if (foregroundApp in blockedApps) {
                Log.d("FocusGuard", "Blocked app detected: $foregroundApp")
                // Show blocked overlay
                val intent = Intent(this, BlockedActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(BlockedActivity.EXTRA_BLOCKED_PACKAGE, foregroundApp)
                }
                startActivity(intent)
                
                // Kick to home screen
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(homeIntent)
            }
        }
    }

    // ========================================================================
    // Phase completion
    // ========================================================================

    private fun onPhaseFinished() {
        Log.d(TAG, "onPhaseFinished phase=$currentPhase")
        TimerAlarmWorker.setCompleted(this)
        clearBootPrefs()
        PreferenceManager.getDefaultSharedPreferences(this).edit()
            .putBoolean(FocusGuardRepository.PREF_FOCUS_GUARD_ACTIVE, false)
            .putInt(FocusGuardRepository.PREF_REMAINING_SECONDS, 0)
            .apply()
        soundManager.stop()
        dndManager.disableDnd()
        wakeScreen()
        postCompletionNotification(currentPhase)
        vibrate()
        playSound()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ========================================================================
    // Broadcasts
    // ========================================================================

    private fun sendTickBroadcast(isRunning: Boolean) {
        val intent = Intent(BROADCAST_TICK).apply {
            putExtra(EXTRA_REMAINING_SECONDS, remainingSeconds)
            putExtra(EXTRA_IS_RUNNING, isRunning)
            putExtra(EXTRA_PHASE, currentPhase.name)
            putExtra(EXTRA_IS_FLOW_MODE, false)
        }
        localBroadcastManager.sendBroadcast(intent)
    }

    private fun sendFlowTickBroadcast(isRunning: Boolean) {
        val intent = Intent(BROADCAST_TICK).apply {
            putExtra(EXTRA_ELAPSED_SECONDS, elapsedSeconds)
            putExtra(EXTRA_IS_RUNNING, isRunning)
            putExtra(EXTRA_PHASE, currentPhase.name)
            putExtra(EXTRA_IS_FLOW_MODE, true)
        }
        localBroadcastManager.sendBroadcast(intent)
    }

    private fun sendStoppedBroadcast() {
        localBroadcastManager.sendBroadcast(
            Intent(BROADCAST_TICK).putExtra(EXTRA_IS_STOPPED, true),
        )
    }

    private fun sendFinishedBroadcast() {
        val intent = Intent(BROADCAST_FINISHED).apply {
            putExtra(EXTRA_PHASE, currentPhase.name)
        }
        localBroadcastManager.sendBroadcast(intent)
    }

    // ========================================================================
    // Boot persistence
    // ========================================================================

    private fun saveBootPrefs(status: String) {
        val active = status != "IDLE"
        completionPrefs.edit()
            .putBoolean(BootReceiver.PREF_SESSION_ACTIVE, active)
            .putBoolean(BootReceiver.PREF_WAS_RUNNING, status == "RUNNING")
            .putString(BootReceiver.PREF_STATUS, status)
            .putLong(BootReceiver.PREF_END_TIME_MS, if (isFlowMode) 0L else endTimeMs)
            .putString(BootReceiver.PREF_PHASE, currentPhase.name)
            .putString(BootReceiver.PREF_MODE, timerMode.name)
            .putString(BootReceiver.PREF_PRESET, preset.name)
            .putInt(BootReceiver.PREF_TOTAL_SECONDS, totalSeconds)
            .putInt(BootReceiver.PREF_REMAINING_SECONDS, remainingSeconds)
            .putInt(BootReceiver.PREF_ELAPSED_SECONDS, elapsedBaseSeconds)
            .putLong(BootReceiver.PREF_SESSION_STARTED_AT_MS, sessionStartedAtMs)
            .putLong(BootReceiver.PREF_RUN_STARTED_AT_MS, runStartedAtMs)
            .commit()
    }

    private fun persistRuntimeState() {
        completionPrefs.edit()
            .putBoolean(BootReceiver.PREF_SESSION_ACTIVE, true)
            .putBoolean(BootReceiver.PREF_WAS_RUNNING, isRunning)
            .putString(BootReceiver.PREF_STATUS, if (isRunning) "RUNNING" else "PAUSED")
            .putLong(BootReceiver.PREF_END_TIME_MS, if (isFlowMode) 0L else endTimeMs)
            .putInt(BootReceiver.PREF_REMAINING_SECONDS, remainingSeconds)
            .putInt(BootReceiver.PREF_ELAPSED_SECONDS, elapsedBaseSeconds)
            .putLong(BootReceiver.PREF_RUN_STARTED_AT_MS, runStartedAtMs)
            .apply()
    }

    private fun clearBootPrefs() {
        completionPrefs.edit()
            .putBoolean(BootReceiver.PREF_SESSION_ACTIVE, false)
            .putBoolean(BootReceiver.PREF_WAS_RUNNING, false)
            .putString(BootReceiver.PREF_STATUS, "IDLE")
            .putLong(BootReceiver.PREF_END_TIME_MS, 0L)
            .putInt(BootReceiver.PREF_TOTAL_SECONDS, 0)
            .putInt(BootReceiver.PREF_REMAINING_SECONDS, 0)
            .putInt(BootReceiver.PREF_ELAPSED_SECONDS, 0)
            .putLong(BootReceiver.PREF_SESSION_STARTED_AT_MS, 0L)
            .putLong(BootReceiver.PREF_RUN_STARTED_AT_MS, 0L)
            .putInt(BootReceiver.PREF_SESSIONS_COMPLETED, 0)
            .putInt(BootReceiver.PREF_SESSIONS_IN_CYCLE, 0)
            .commit()
    }

    private fun elapsedAt(nowMs: Long): Int {
        val activeRunStart = runStartedAtMs
        val extra = if (isRunning && activeRunStart in 1L..nowMs) {
            ((nowMs - activeRunStart) / 1_000L).coerceAtMost(Int.MAX_VALUE.toLong())
        } else 0L
        return (elapsedBaseSeconds.toLong() + extra).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    private fun setFocusGuardForResume() {
        PreferenceManager.getDefaultSharedPreferences(this).edit()
            .putBoolean(
                FocusGuardRepository.PREF_FOCUS_GUARD_ACTIVE,
                currentPhase == TimerPhase.FOCUS,
            )
            .putInt(
                FocusGuardRepository.PREF_REMAINING_SECONDS,
                if (isFlowMode) IntervalPreset.FLOW.focusMinutes * 60 else remainingSeconds,
            )
            .apply()
    }

    private fun restoreAfterServiceRestart() {
        val prefs = completionPrefs
        val active = prefs.getBoolean(BootReceiver.PREF_SESSION_ACTIVE, false)
        val status = prefs.getString(BootReceiver.PREF_STATUS, "IDLE")
        if (!active || status != "RUNNING") {
            stopSelf()
            return
        }
        val phase = prefs.getString(BootReceiver.PREF_PHASE, null)
            ?.let { runCatching { TimerPhase.valueOf(it) }.getOrNull() }
        val mode = prefs.getString(BootReceiver.PREF_MODE, null)
            ?.let { runCatching { TimerMode.valueOf(it) }.getOrNull() }
        val savedPreset = prefs.getString(BootReceiver.PREF_PRESET, null)
            ?.let { runCatching { IntervalPreset.valueOf(it) }.getOrNull() }
        val start = prefs.getLong(BootReceiver.PREF_SESSION_STARTED_AT_MS, 0L)
        if (phase == null || mode == null || savedPreset == null || start <= 0L) {
            clearBootPrefs()
            stopSelf()
            return
        }

        currentPhase = phase
        preset = savedPreset
        sessionStartedAtMs = start
        if (mode == TimerMode.FLOW && phase == TimerPhase.FOCUS) {
            isFlowMode = true
            timerMode = TimerMode.FLOW
            totalSeconds = 0
            remainingSeconds = 0
            elapsedBaseSeconds = prefs.getInt(BootReceiver.PREF_ELAPSED_SECONDS, 0).coerceAtLeast(0)
            runStartedAtMs = prefs.getLong(BootReceiver.PREF_RUN_STARTED_AT_MS, 0L)
            if (runStartedAtMs <= 0L) {
                clearBootPrefs()
                stopSelf()
                return
            }
            isRunning = true
            elapsedSeconds = elapsedAt(System.currentTimeMillis())
            elapsedBaseSeconds = elapsedSeconds
            runStartedAtMs = System.currentTimeMillis()
            saveBootPrefs("RUNNING")
            setFocusGuardForResume()
            startForegroundCompat()
            startFlowCoroutine()
            return
        }

        timerMode = TimerMode.POMODORO
        isFlowMode = false
        totalSeconds = prefs.getInt(BootReceiver.PREF_TOTAL_SECONDS, 0)
        endTimeMs = prefs.getLong(BootReceiver.PREF_END_TIME_MS, 0L)
        remainingSeconds = ceilSeconds(endTimeMs - System.currentTimeMillis())
        if (totalSeconds <= 0 || remainingSeconds <= 0) {
            clearBootPrefs()
            stopSelf()
            return
        }
        isRunning = true
        saveBootPrefs("RUNNING")
        setFocusGuardForResume()
        startForegroundCompat()
        startTimerCoroutine()
    }

    // ========================================================================
    // Notification
    // ========================================================================

    private fun startForegroundCompat() {
        val notification = buildNotification()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            else
                0,
        )
    }

    private fun updateNotification() {
        notificationManager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val pauseResumePendingIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, TimerForegroundService::class.java).apply {
                action = if (isRunning) ACTION_PAUSE else ACTION_RESUME
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val stopPendingIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, TimerForegroundService::class.java).apply {
                action = ACTION_STOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title   = if (isFlowMode) "Flow" else currentPhase.displayName()
        val content = if (isFlowMode) formatTime(elapsedSeconds) else formatTime(remainingSeconds)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(0, if (isRunning) "Pause" else "Resume", pauseResumePendingIntent)
            .addAction(0, "Stop", stopPendingIntent)
            .build()
    }

    // ========================================================================
    // Vibration / sound
    // ========================================================================

    private fun isVibrateEnabled(): Boolean =
        completionPrefs.getBoolean(PREF_VIBRATE_ENABLED, true)

    @Suppress("DEPRECATION")
    private fun vibrate() {
        if (!isVibrateEnabled()) return
        val effect = VibrationEffect.createWaveform(longArrayOf(0, 300, 100, 300), -1)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator.vibrate(effect)
        } else {
            val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            v.vibrate(effect)
        }
    }

    private fun playSound() {
        val type = completionPrefs.getString(PREF_SOUND_TYPE, "Bell") ?: "Bell"
        if (type.equals("None", ignoreCase = true)) return
        try {
            val uri      = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(this, uri)
            ringtone?.play()
        } catch (_: Exception) {
            Log.w(TAG, "playSound failed")
        }
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    @Suppress("DEPRECATION")
    private fun wakeScreen() {
        try {
            val pm = getSystemService(PowerManager::class.java)
            pm.newWakeLock(
                PowerManager.FULL_WAKE_LOCK or
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "Toki:SessionEndWake",
            ).acquire(5_000L) // auto-released after 5 s
        } catch (e: Exception) {
            Log.w(TAG, "wakeScreen failed: ${e.message}")
        }
    }

    private fun postCompletionNotification(phase: TimerPhase) {
        val title = when (phase) {
            TimerPhase.FOCUS       -> "Focus session complete 🍅"
            TimerPhase.SHORT_BREAK -> "Break over — back to work!"
            TimerPhase.LONG_BREAK  -> "Long break done!"
        }
        val openIntent = PendingIntent.getActivity(
            this, 10,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(this, CHANNEL_ALERTS)
        builder.setSmallIcon(R.drawable.ic_launcher_foreground)
        builder.setContentTitle(title)
        builder.setContentText("Tap to start the next interval.")
        builder.setContentIntent(openIntent)
        builder.setAutoCancel(true)
        builder.setPriority(NotificationCompat.PRIORITY_HIGH)
        builder.setCategory(NotificationCompat.CATEGORY_ALARM)
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        builder.setFullScreenIntent(openIntent, true)
        val notification = builder.build()
        notificationManager.notify(NOTIFY_ID_COMPLETE, notification)
    }

    private fun acquireWakeLock(timeoutMs: Long) {
        releaseWakeLock()
        val pm = getSystemService(PowerManager::class.java)
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Toki:TimerWakeLock",
        ).also { it.acquire(timeoutMs) }
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) wakeLock?.release()
        wakeLock = null
    }

    private fun TimerPhase.displayName(): String = when (this) {
        TimerPhase.FOCUS       -> "Focus"
        TimerPhase.SHORT_BREAK -> "Short Break"
        TimerPhase.LONG_BREAK  -> "Long Break"
    }

    private fun formatTime(totalSeconds: Int): String {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d".format(minutes, seconds)
    }
}
