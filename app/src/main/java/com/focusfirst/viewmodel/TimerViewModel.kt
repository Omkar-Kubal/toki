package com.focusfirst.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import com.focusfirst.analytics.TokiAnalytics
import com.focusfirst.data.SettingsRepository
import com.focusfirst.data.db.DailySummary
import com.focusfirst.data.db.SessionDao
import com.focusfirst.data.db.SessionEntity
import com.focusfirst.data.model.AmbientSound
import com.focusfirst.data.model.AllBadges
import com.focusfirst.data.model.Badge
import com.focusfirst.data.model.IntervalPreset
import com.focusfirst.data.model.TimerMode
import com.focusfirst.data.model.TimerPhase
import com.focusfirst.data.model.TimerState
import com.focusfirst.data.remote.FirestoreRepository
import com.focusfirst.data.repository.FocusGuardRepository
import com.focusfirst.service.SoundManager
import com.focusfirst.service.TimerAlarmWorker
import com.focusfirst.service.TimerForegroundService
import com.focusfirst.receiver.BootReceiver
import com.focusfirst.service.cancelAlarm
import com.focusfirst.service.scheduleAlarm
import com.focusfirst.util.BadgeEvaluator
import com.focusfirst.util.DeepLinkDuration
import com.focusfirst.util.DndManager
import com.focusfirst.util.TimerRecoveryDecision
import com.focusfirst.util.TimerRecoveryPolicy
import com.focusfirst.util.TimerRecoverySnapshot
import com.focusfirst.util.TimerRunStatus
import com.focusfirst.util.TimerStartDurationPolicy
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

// ---------------------------------------------------------------------------
// TimerViewModel
//
// Single source of truth for all timer UI state.  Coordinates between:
//   - TimerForegroundService  (issues commands, receives tick/finish broadcasts)
//   - SettingsRepository      (drives break lengths, long-break cadence, etc.)
//   - Room / SessionDao        (persists completed + partial sessions)
//   - SoundManager            (ambient sound playback lifecycle)
//   - DndManager              (Do Not Disturb enable/restore lifecycle)
//   - Compose UI               (exposes StateFlows)
//
// Initial FOCUS duration when the user taps Start follows the selected
// [IntervalPreset]; automatic phase transitions use slider values from
// [SettingsRepository].
// ---------------------------------------------------------------------------

@HiltViewModel
class TimerViewModel @Inject constructor(
    private val application: Application,
    private val sessionDao: SessionDao,
    private val settingsRepository: SettingsRepository,
    private val soundManager: SoundManager,
    private val dndManager: DndManager,
    private val focusGuardRepository: FocusGuardRepository,
    private val firestoreRepository: FirestoreRepository,
) : ViewModel() {

    // ── LocalBroadcastManager ─────────────────────────────────────────────────
    private val localBroadcastManager: LocalBroadcastManager =
        LocalBroadcastManager.getInstance(application)

    // ========================================================================
    // Settings — Eagerly started so .value is always up-to-date
    // ========================================================================

    private val focusMinutesFlow = settingsRepository.focusMinutes
        .stateIn(viewModelScope, SharingStarted.Eagerly, 25)

    private val shortBreakMinutesFlow = settingsRepository.shortBreakMinutes
        .stateIn(viewModelScope, SharingStarted.Eagerly, 5)

    private val longBreakMinutesFlow = settingsRepository.longBreakMinutes
        .stateIn(viewModelScope, SharingStarted.Eagerly, 15)

    private val sessionsBeforeLongBreakFlow = settingsRepository.sessionsBeforeLongBreak
        .stateIn(viewModelScope, SharingStarted.Eagerly, 4)

    // ========================================================================
    // Exposed StateFlows
    // ========================================================================

    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    /** Emits Unit each time a FOCUS phase (Pomodoro or Flow) completes successfully. */
    private val _focusSessionCompleted = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val focusSessionCompleted: SharedFlow<Unit> = _focusSessionCompleted.asSharedFlow()

    /** Emits list of newly unlocked badges after a session completes. */
    private val _newBadges = MutableSharedFlow<List<Badge>>(extraBufferCapacity = 4)
    val newBadges: SharedFlow<List<Badge>> = _newBadges.asSharedFlow()

    private val todayStartFlow = flow {
        while (true) {
            val start = todayStartMs()
            emit(start)
            val msUntilMidnight = start + 86_400_000L - System.currentTimeMillis()
            delay(msUntilMidnight.coerceAtLeast(1_000L))
        }
    }

    val todayCount: StateFlow<Int> = todayStartFlow
        .flatMapLatest { sessionDao.observeTodayCount(it) }
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000L),
            initialValue = 0,
        )

    val totalCompleted: StateFlow<Int> = sessionDao
        .observeTotalCompleted()
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.Eagerly,
            initialValue = 0,
        )

    /** Five most-recent sessions (any status), newest first. */
    val recentSessions: StateFlow<List<SessionEntity>> = sessionDao
        .observeRecent()
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyList(),
        )

    val weeklySummary: StateFlow<List<DailySummary>> = sessionDao
        .observeWeeklySummary(sinceEpochMs = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L)
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyList(),
        )

    /** Last 14 calendar buckets for week-over-week stats trends. */
    val dailySummaries14d: StateFlow<List<DailySummary>> = sessionDao
        .observeWeeklySummary(sinceEpochMs = System.currentTimeMillis() - 14L * 24 * 60 * 60 * 1000L)
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyList(),
        )

    /** All sessions ever recorded, newest first. Used by the heatmap in StatsScreen. */
    val allSessions: StateFlow<List<SessionEntity>> = sessionDao
        .observeAll()
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyList(),
        )

    /**
     * Current consecutive-day focus streak.
     */
    val streakDays: StateFlow<Int> = sessionDao
        .observeCompletedDays()
        .map { days -> computeStreak(days) }
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.Eagerly,
            initialValue = 0,
        )

    val ambientSound: StateFlow<AmbientSound> = settingsRepository.ambientSound
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.Eagerly,
            initialValue = AmbientSound.NONE,
        )

    val ambientVolume: StateFlow<Float> = settingsRepository.ambientVolume
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.Eagerly,
            initialValue = 0.5f,
        )

    val dndEnabled: StateFlow<Boolean> = settingsRepository.dndEnabled
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.Eagerly,
            initialValue = false,
        )

    /** Whether the user has an active Pro purchase. Used to gate auto-sync. */
    val isPro: StateFlow<Boolean> = settingsRepository.proUnlocked
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.Eagerly,
            initialValue = false,
        )

    // ========================================================================
    // Private session-tracking fields
    // ========================================================================

    private var sessionStartMs: Long = 0L
    private var runStartedAtMs: Long = 0L
    private val timerPrefs by lazy {
        application.getSharedPreferences(BootReceiver.PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Cumulative count of FOCUS phases completed in the current Pomodoro cycle.
     * Reset to 0 after every LONG_BREAK so the cycle repeats correctly.
     */
    private var sessionCount: Int = 0

    // ========================================================================
    // Broadcast receiver
    // ========================================================================

    private val timerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                TimerForegroundService.BROADCAST_TICK     -> handleTick(intent)
                TimerForegroundService.BROADCAST_FINISHED -> {
                    val phaseString =
                        intent.getStringExtra(TimerForegroundService.EXTRA_PHASE) ?: return
                    onPhaseFinished(phaseString)
                }
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(TimerForegroundService.BROADCAST_TICK)
            addAction(TimerForegroundService.BROADCAST_FINISHED)
        }
        localBroadcastManager.registerReceiver(timerReceiver, filter)
        Log.d(TAG, "init: BroadcastReceiver registered")

        // Keep the idle timer display in sync when the user changes duration in Settings.
        viewModelScope.launch {
            focusMinutesFlow.collect { minutes ->
                val state = _timerState.value
                if (!state.isRunning && !state.isPaused && state.phase == TimerPhase.FOCUS) {
                    val seconds = minutes * 60
                    _timerState.update { it.copy(totalSeconds = seconds, remainingSeconds = seconds) }
                }
            }
        }
        restorePersistedTimer()
    }

    // ========================================================================
    // Public API — timer commands
    // ========================================================================

    /**
     * Starts a fresh FOCUS session in Pomodoro (count-down) mode.
     */
    fun start(
        preset: IntervalPreset = _timerState.value.preset,
        durationMinutesOverride: Int? = null,
    ) {
        val durationSeconds = TimerStartDurationPolicy.secondsForStart(
            configuredDurationSeconds = _timerState.value.totalSeconds,
            requestedDurationMinutes = durationMinutesOverride,
        )
        val now = System.currentTimeMillis()
        sessionStartMs = now
        runStartedAtMs = now
        val runningState = _timerState.value.copy(
            phase = TimerPhase.FOCUS,
            preset = preset,
            timerMode = TimerMode.POMODORO,
            totalSeconds = durationSeconds,
            remainingSeconds = durationSeconds,
            elapsedSeconds = 0,
            isRunning = true,
            isPaused = false,
        )
        _timerState.value = runningState
        persistTimerSnapshot(
            runningState,
            TimerRunStatus.RUNNING,
            endTimeMs = now + durationSeconds * 1_000L,
            runStartedAtMs = now,
        )

        setFocusGuardActive(true, durationSeconds)
        TimerAlarmWorker.resetCompletionFlag(application)
        scheduleAlarm(application, durationSeconds, TimerPhase.FOCUS)

        ContextCompat.startForegroundService(
            application,
            TimerForegroundService.buildStartIntent(
                application, durationSeconds, TimerPhase.FOCUS,
                totalSeconds = durationSeconds,
                sessionStartedAtMs = sessionStartMs,
                preset = preset,
            ),
        )

        startSoundAndDnd()

        // Analytics
        TokiAnalytics.logSessionStarted(
            durationMinutes = durationSeconds / 60,
            soundEnabled    = ambientSound.value != AmbientSound.NONE,
            dndEnabled      = dndEnabled.value,
            hasTask         = false,
        )
        Firebase.crashlytics.log("Timer started: $durationSeconds seconds")

        Log.d(TAG, "start preset=$preset duration=${durationSeconds}s")
    }

    /** Starts the Flow stopwatch; it counts up until the user stops it. */
    fun startFlow() {
        val now = System.currentTimeMillis()
        sessionStartMs = now
        runStartedAtMs = now
        val state = _timerState.value.copy(
            phase = TimerPhase.FOCUS,
            preset = IntervalPreset.FLOW,
            timerMode = TimerMode.FLOW,
            totalSeconds = 0,
            remainingSeconds = 0,
            elapsedSeconds = 0,
            isRunning = true,
            isPaused = false,
        )
        _timerState.value = state
        persistTimerSnapshot(state, TimerRunStatus.RUNNING, endTimeMs = 0L, runStartedAtMs = now)
        setFocusGuardActive(true, IntervalPreset.FLOW.focusMinutes * 60)
        cancelAlarm(application)
        TimerAlarmWorker.resetCompletionFlag(application)
        ContextCompat.startForegroundService(
            application,
            TimerForegroundService.buildStartFlowIntent(application, 0, sessionStartMs),
        )
        startSoundAndDnd()
        TokiAnalytics.logSessionStarted(
            durationMinutes = IntervalPreset.FLOW.focusMinutes,
            soundEnabled = ambientSound.value != AmbientSound.NONE,
            dndEnabled = dndEnabled.value,
            hasTask = false,
        )
        Log.d(TAG, "startFlow stopwatch")
    }

    fun pause() {
        val state = _timerState.value
        if (!state.isRunning) return
        val now = System.currentTimeMillis()
        val pausedState = if (state.timerMode == TimerMode.FLOW) {
            state.copy(elapsedSeconds = currentFlowElapsed(now), isRunning = false, isPaused = true)
        } else {
            val end = timerPrefs.getLong(BootReceiver.PREF_END_TIME_MS, 0L)
            val remaining = if (end > 0L) ceilSeconds(end - now) else state.remainingSeconds
            state.copy(
                remainingSeconds = remaining.coerceAtMost(state.totalSeconds).coerceAtLeast(0),
                isRunning = false,
                isPaused = true,
            )
        }
        _timerState.value = pausedState
        runStartedAtMs = 0L
        persistTimerSnapshot(pausedState, TimerRunStatus.PAUSED, endTimeMs = 0L, runStartedAtMs = 0L)
        Log.d(TAG, "pause")
        setFocusGuardActive(false, 0)
        cancelAlarm(application)
        soundManager.pause()
        dndManager.disableDnd()
        sendServiceAction(TimerForegroundService.ACTION_PAUSE)
    }

    fun resume() {
        val state = _timerState.value
        if (!state.isPaused) return
        val now = System.currentTimeMillis()
        if (state.timerMode == TimerMode.FLOW) {
            val resumed = state.copy(isRunning = true, isPaused = false)
            _timerState.value = resumed
            runStartedAtMs = now
            persistTimerSnapshot(resumed, TimerRunStatus.RUNNING, endTimeMs = 0L, runStartedAtMs = now)
            ContextCompat.startForegroundService(
                application,
                TimerForegroundService.buildStartFlowIntent(application, state.elapsedSeconds, sessionStartMs),
            )
        } else {
            if (state.remainingSeconds <= 0) return
            val end = now + state.remainingSeconds * 1_000L
            val resumed = state.copy(isRunning = true, isPaused = false)
            _timerState.value = resumed
            runStartedAtMs = now
            persistTimerSnapshot(resumed, TimerRunStatus.RUNNING, endTimeMs = end, runStartedAtMs = now)
            scheduleAlarm(application, state.remainingSeconds, state.phase)
            ContextCompat.startForegroundService(
                application,
                TimerForegroundService.buildStartIntent(
                    application,
                    state.remainingSeconds,
                    state.phase,
                    totalSeconds = state.totalSeconds,
                    sessionStartedAtMs = sessionStartMs,
                    preset = state.preset,
                ),
            )
        }
        val guardRemaining = if (state.timerMode == TimerMode.FLOW) {
            IntervalPreset.FLOW.focusMinutes * 60
        } else state.remainingSeconds
        setFocusGuardActive(state.phase == TimerPhase.FOCUS, guardRemaining)
        if (state.phase == TimerPhase.FOCUS) startSoundAndDnd() else {
            soundManager.pause()
            dndManager.disableDnd()
        }
        Log.d(TAG, "resume")
    }

    /**
     * Stops the timer unconditionally.
     *
     * Pomodoro: saves a partial session if the user focused for > 30 s.
     * Flow: saves a completed session with actual elapsed time if > 30 s.
     */
    fun stop() = endSession(saveProgress = true)

    /** Discards the current session without recording focus time. */
    fun cancel() = endSession(saveProgress = false)

    private fun endSession(saveProgress: Boolean) {
        val state = _timerState.value
        if (state.isIdle) return
        val elapsed = if (state.timerMode == TimerMode.FLOW) {
            currentFlowElapsed(System.currentTimeMillis())
        } else {
            val end = timerPrefs.getLong(BootReceiver.PREF_END_TIME_MS, 0L)
            val remaining = if (state.isRunning && end > 0L) {
                ceilSeconds(end - System.currentTimeMillis()).coerceAtMost(state.totalSeconds)
            } else state.remainingSeconds
            (state.totalSeconds - remaining).coerceAtLeast(0)
        }

        if (saveProgress && state.timerMode == TimerMode.FLOW) {
            if (elapsed > 30) {
                saveSession(wasCompleted = true, phase = TimerPhase.FOCUS, durationSeconds = elapsed)
            } else if (elapsed > 0) {
                TokiAnalytics.logSessionAbandoned(durationMinutes = 0, elapsedMinutes = elapsed / 60)
                sessionStartMs = 0L
            }
        } else if (saveProgress && state.phase == TimerPhase.FOCUS && sessionStartMs > 0L && elapsed > 30) {
            saveSession(wasCompleted = false, phase = state.phase, durationSeconds = elapsed)
            TokiAnalytics.logSessionAbandoned(
                durationMinutes = state.totalSeconds / 60,
                elapsedMinutes = elapsed / 60,
            )
        }

        if (!saveProgress && elapsed > 0) {
            TokiAnalytics.logSessionAbandoned(
                durationMinutes = if (state.timerMode == TimerMode.FLOW) 0 else state.totalSeconds / 60,
                elapsedMinutes = elapsed / 60,
            )
        }

        sessionStartMs = 0L
        runStartedAtMs = 0L
        clearRecoveryPrefs()
        setFocusGuardActive(false, 0)
        soundManager.stop()
        dndManager.disableDnd()
        cancelAlarm(application)
        sendServiceAction(TimerForegroundService.ACTION_STOP)
        val configuredSeconds = focusMinutesFlow.value * 60
        _timerState.value = TimerState(
            preset = state.preset,
            totalSeconds = configuredSeconds,
            remainingSeconds = configuredSeconds,
            sessionsCompleted = state.sessionsCompleted,
            customFocusMinutes = focusMinutesFlow.value,
        )
        Firebase.crashlytics.log(if (saveProgress) "Timer stopped" else "Timer cancelled")
        Log.d(TAG, if (saveProgress) "stop" else "cancel")
    }

    /**
     * Switches the active preset when the timer is idle.
     */
    fun selectPreset(preset: IntervalPreset) {
        val state = _timerState.value
        if (state.isRunning || state.isPaused) return

        // Update the active preset
        _timerState.update { current ->
            if (preset == IntervalPreset.FLOW) current.copy(preset = preset)
            else current.copy(
                preset = preset,
                totalSeconds = preset.focusMinutes * 60,
                remainingSeconds = preset.focusMinutes * 60,
                customFocusMinutes = preset.focusMinutes,
            )
        }
        
        // Update the underlying settings so the slider and timer display sync
        if (preset != IntervalPreset.FLOW) {
            viewModelScope.launch {
                settingsRepository.update(SettingsRepository.KEY_FOCUS_MINUTES, preset.focusMinutes)
            }
        }
        
        Log.d(TAG, "selectPreset $preset")
    }

    /** Updates the idle Home dial in five-minute steps, without changing a live timer. */
    fun selectDuration(minutes: Int) {
        if (minutes !in 5..60 || minutes % 5 != 0) return
        val state = _timerState.value
        if (!state.isIdle || state.totalSeconds == minutes * 60) return
        _timerState.value = state.copy(
            totalSeconds = minutes * 60,
            remainingSeconds = minutes * 60,
            customFocusMinutes = minutes,
        )
        viewModelScope.launch {
            settingsRepository.update(SettingsRepository.KEY_FOCUS_MINUTES, minutes)
        }
    }

    fun updateSound(sound: AmbientSound, volume: Float) {
        viewModelScope.launch {
            settingsRepository.updateAmbientSound(sound)
            val state = _timerState.value
            if (state.isRunning && state.phase == TimerPhase.FOCUS) {
                if (sound == AmbientSound.NONE) soundManager.stop()
                else soundManager.play(sound, volume)
            }
        }
    }

    fun updateVolume(volume: Float) {
        viewModelScope.launch {
            settingsRepository.updateAmbientVolume(volume)
            soundManager.setVolume(volume)
        }
    }

    // ========================================================================
    // Private — broadcast handling
    // ========================================================================

    private fun handleTick(intent: Intent) {
        if (intent.getBooleanExtra(TimerForegroundService.EXTRA_IS_STOPPED, false)) {
            sessionStartMs = 0L
            runStartedAtMs = 0L
            _timerState.value = TimerState()
            return
        }
        if (_timerState.value.isIdle) return

        val isFlowMode = intent.getBooleanExtra(TimerForegroundService.EXTRA_IS_FLOW_MODE, false)

        if (isFlowMode) {
            val elapsed = intent.getIntExtra(TimerForegroundService.EXTRA_ELAPSED_SECONDS, 0)
            val running = intent.getBooleanExtra(TimerForegroundService.EXTRA_IS_RUNNING, false)
            _timerState.update { current ->
                current.copy(
                    elapsedSeconds = elapsed,
                    totalSeconds = 0,
                    remainingSeconds = 0,
                    isRunning      = running,
                    isPaused       = !running && timerPrefs.getString(BootReceiver.PREF_STATUS, "") == "PAUSED",
                    timerMode      = TimerMode.FLOW,
                    phase          = TimerPhase.FOCUS,
                )
            }
        } else {
            val remaining = intent.getIntExtra(TimerForegroundService.EXTRA_REMAINING_SECONDS, 0)
            val running   = intent.getBooleanExtra(TimerForegroundService.EXTRA_IS_RUNNING, false)
            val phase     = intent.getStringExtra(TimerForegroundService.EXTRA_PHASE)
                ?.let { runCatching { TimerPhase.valueOf(it) }.getOrNull() }
                ?: _timerState.value.phase

            _timerState.update { current ->
                val tickMode =
                    if (current.timerMode == TimerMode.FLOW && current.preset == IntervalPreset.FLOW) {
                        TimerMode.FLOW
                    } else {
                        TimerMode.POMODORO
                    }
                current.copy(
                    remainingSeconds = remaining,
                    isRunning        = running,
                    isPaused         = !running && remaining > 0,
                    phase            = phase,
                    timerMode        = tickMode,
                )
            }
        }
    }

    // ========================================================================
    // Private — Pomodoro phase lifecycle
    // ========================================================================

    private fun onPhaseFinished(phaseString: String) {
        val phase = runCatching { TimerPhase.valueOf(phaseString) }.getOrElse {
            Log.e(TAG, "Unknown phase in BROADCAST_FINISHED: '$phaseString'")
            return
        }

        if (phase == TimerPhase.FOCUS) {
            val completedFlowTimer = _timerState.value.timerMode == TimerMode.FLOW
            val completedDuration = _timerState.value.totalSeconds
            saveSession(wasCompleted = true, phase = phase, durationSeconds = completedDuration)
            sessionCount++
            _timerState.update { it.copy(sessionsCompleted = it.sessionsCompleted + 1) }
            if (completedFlowTimer) {
                val flowSeconds = IntervalPreset.FLOW.focusMinutes * 60
                sessionStartMs = 0L
                setFocusGuardActive(false, 0)
                soundManager.stop()
                dndManager.disableDnd()
                cancelAlarm(application)
                _timerState.update {
                    TimerState(
                        preset = IntervalPreset.FLOW,
                        timerMode = TimerMode.FLOW,
                        totalSeconds = flowSeconds,
                        remainingSeconds = flowSeconds,
                        sessionsCompleted = it.sessionsCompleted,
                    )
                }
                Log.d(TAG, "onPhaseFinished flow complete")
                return
            }
        }

        val sessionsBeforeLong = sessionsBeforeLongBreakFlow.value

        val nextPhase = when (phase) {
            TimerPhase.FOCUS ->
                if (sessionCount % sessionsBeforeLong == 0) TimerPhase.LONG_BREAK
                else TimerPhase.SHORT_BREAK

            TimerPhase.SHORT_BREAK -> TimerPhase.FOCUS

            TimerPhase.LONG_BREAK  -> {
                sessionCount = 0
                TimerPhase.FOCUS
            }
        }

        val nextDurationSeconds = when (nextPhase) {
            TimerPhase.FOCUS       -> focusMinutesFlow.value * 60
            TimerPhase.SHORT_BREAK -> shortBreakMinutesFlow.value * 60
            TimerPhase.LONG_BREAK  -> longBreakMinutesFlow.value * 60
        }

        if (nextPhase == TimerPhase.FOCUS) {
            startSoundAndDnd()
            setFocusGuardActive(true, nextDurationSeconds)
        } else {
            soundManager.pause()
            dndManager.disableDnd()
            setFocusGuardActive(false, 0)
        }

        Log.d(TAG, "onPhaseFinished phase=$phase → next=$nextPhase (${nextDurationSeconds}s) sessionCount=$sessionCount")
        launchPhase(nextPhase, nextDurationSeconds)
    }

    private fun launchPhase(phase: TimerPhase, durationSeconds: Int) {
        sessionStartMs = System.currentTimeMillis()
        runStartedAtMs = sessionStartMs

        val runningState = _timerState.value.copy(
                phase            = phase,
                timerMode        = TimerMode.POMODORO,
                totalSeconds     = durationSeconds,
                remainingSeconds = durationSeconds,
                elapsedSeconds   = 0,
                isRunning        = true,
                isPaused         = false,
            )
        _timerState.value = runningState
        val endTime = sessionStartMs + durationSeconds * 1_000L
        persistTimerSnapshot(runningState, TimerRunStatus.RUNNING, endTime, runStartedAtMs)

        TimerAlarmWorker.resetCompletionFlag(application)
        scheduleAlarm(application, durationSeconds, phase)

        ContextCompat.startForegroundService(
            application,
            TimerForegroundService.buildStartIntent(
                application,
                durationSeconds,
                phase,
                totalSeconds = durationSeconds,
                sessionStartedAtMs = sessionStartMs,
                preset = runningState.preset,
            ),
        )
    }

    // ========================================================================
    // Private — persistence
    // ========================================================================

    private fun saveSession(
        wasCompleted: Boolean,
        phase: TimerPhase,
        durationSeconds: Int? = null,
    ) {
        val startMs = sessionStartMs
        sessionStartMs = 0L
        runStartedAtMs = 0L
        if (startMs == 0L) return

        val actualDurationSeconds = durationSeconds
            ?: ((System.currentTimeMillis() - startMs) / 1_000L).toInt()
        val tag = if (_timerState.value.timerMode == TimerMode.FLOW) "Flow" else when (phase) {
            TimerPhase.FOCUS       -> "Focus"
            TimerPhase.SHORT_BREAK -> "Short Break"
            TimerPhase.LONG_BREAK  -> "Long Break"
        }

        viewModelScope.launch {
            runCatching {
                sessionDao.insert(
                    SessionEntity(
                        startedAt       = startMs,
                        durationSeconds = actualDurationSeconds,
                        wasCompleted    = wasCompleted,
                        tag             = tag,
                    )
                )
            }.onFailure { Log.e(TAG, "Failed to save session", it) }
             .onSuccess {
                 Log.d(TAG, "Saved session tag=$tag completed=$wasCompleted duration=${actualDurationSeconds}s")
                 if (wasCompleted && phase == TimerPhase.FOCUS) {
                     _focusSessionCompleted.tryEmit(Unit)
                     evaluateBadgesAfterSave()
                     autoSyncIfPro()
                     TokiAnalytics.logSessionCompleted(
                         durationMinutes = actualDurationSeconds / 60,
                         sessionNumber   = totalCompleted.value,
                     )
                 }
             }
        }
    }

    private fun persistTimerSnapshot(
        state: TimerState,
        status: TimerRunStatus,
        endTimeMs: Long,
        runStartedAtMs: Long,
    ) {
        timerPrefs.edit()
            .putBoolean(BootReceiver.PREF_SESSION_ACTIVE, true)
            .putBoolean(BootReceiver.PREF_WAS_RUNNING, status == TimerRunStatus.RUNNING)
            .putString(BootReceiver.PREF_STATUS, status.name)
            .putLong(BootReceiver.PREF_END_TIME_MS, endTimeMs)
            .putString(BootReceiver.PREF_PHASE, state.phase.name)
            .putString(BootReceiver.PREF_MODE, state.timerMode.name)
            .putString(BootReceiver.PREF_PRESET, state.preset.name)
            .putInt(BootReceiver.PREF_TOTAL_SECONDS, state.totalSeconds)
            .putInt(BootReceiver.PREF_REMAINING_SECONDS, state.remainingSeconds)
            .putInt(BootReceiver.PREF_ELAPSED_SECONDS, state.elapsedSeconds)
            .putLong(BootReceiver.PREF_SESSION_STARTED_AT_MS, sessionStartMs)
            .putLong(BootReceiver.PREF_RUN_STARTED_AT_MS, runStartedAtMs)
            .putInt(BootReceiver.PREF_SESSIONS_COMPLETED, state.sessionsCompleted)
            .putInt(BootReceiver.PREF_SESSIONS_IN_CYCLE, sessionCount)
            .commit()
    }

    private fun restorePersistedTimer() {
        if (!timerPrefs.getBoolean(BootReceiver.PREF_SESSION_ACTIVE, false)) {
            if (timerPrefs.getBoolean(BootReceiver.PREF_WAS_RUNNING, false)) clearStaleTimer()
            return
        }

        val mode = timerPrefs.getString(BootReceiver.PREF_MODE, null)
            ?.let { runCatching { TimerMode.valueOf(it) }.getOrNull() }
        val phase = timerPrefs.getString(BootReceiver.PREF_PHASE, null)
            ?.let { runCatching { TimerPhase.valueOf(it) }.getOrNull() }
        val preset = timerPrefs.getString(BootReceiver.PREF_PRESET, null)
            ?.let { runCatching { IntervalPreset.valueOf(it) }.getOrNull() }
        val status = timerPrefs.getString(BootReceiver.PREF_STATUS, null)
            ?.let { runCatching { TimerRunStatus.valueOf(it) }.getOrNull() }
        if (mode == null || phase == null || preset == null || status == null) {
            Log.w(TAG, "Discarding malformed saved timer snapshot")
            clearStaleTimer()
            return
        }

        val snapshot = TimerRecoverySnapshot(
            mode = mode,
            phase = phase,
            preset = preset,
            status = status,
            totalSeconds = timerPrefs.getInt(BootReceiver.PREF_TOTAL_SECONDS, 0),
            remainingSeconds = timerPrefs.getInt(BootReceiver.PREF_REMAINING_SECONDS, 0),
            elapsedSeconds = timerPrefs.getInt(BootReceiver.PREF_ELAPSED_SECONDS, 0),
            sessionStartedAtMs = timerPrefs.getLong(BootReceiver.PREF_SESSION_STARTED_AT_MS, 0L),
            runStartedAtMs = timerPrefs.getLong(BootReceiver.PREF_RUN_STARTED_AT_MS, 0L),
            endTimeMs = timerPrefs.getLong(BootReceiver.PREF_END_TIME_MS, 0L),
            sessionsCompleted = timerPrefs.getInt(BootReceiver.PREF_SESSIONS_COMPLETED, 0),
            sessionsInCycle = timerPrefs.getInt(BootReceiver.PREF_SESSIONS_IN_CYCLE, 0),
        )
        when (val decision = TimerRecoveryPolicy.recover(snapshot, System.currentTimeMillis())) {
            is TimerRecoveryDecision.Running -> {
                sessionStartMs = decision.sessionStartedAtMs
                sessionCount = decision.sessionsInCycle
                val now = System.currentTimeMillis()
                runStartedAtMs = now
                _timerState.value = decision.timerState
                persistTimerSnapshot(
                    decision.timerState,
                    TimerRunStatus.RUNNING,
                    endTimeMs = if (mode == TimerMode.FLOW) 0L else snapshot.endTimeMs,
                    runStartedAtMs = now,
                )
                if (mode == TimerMode.FLOW) {
                    cancelAlarm(application)
                    ContextCompat.startForegroundService(
                        application,
                        TimerForegroundService.buildStartFlowIntent(
                            application,
                            decision.timerState.elapsedSeconds,
                            sessionStartMs,
                        ),
                    )
                } else {
                    scheduleAlarm(application, decision.timerState.remainingSeconds, phase)
                    ContextCompat.startForegroundService(
                        application,
                        TimerForegroundService.buildStartIntent(
                            application,
                            decision.timerState.remainingSeconds,
                            phase,
                            totalSeconds = decision.timerState.totalSeconds,
                            sessionStartedAtMs = sessionStartMs,
                            preset = preset,
                            endTimeMs = snapshot.endTimeMs,
                        ),
                    )
                }
                setFocusGuardActive(phase == TimerPhase.FOCUS, decision.timerState.remainingSeconds)
                if (phase == TimerPhase.FOCUS) startSoundAndDnd()
            }
            is TimerRecoveryDecision.Paused -> {
                sessionStartMs = decision.sessionStartedAtMs
                sessionCount = decision.sessionsInCycle
                runStartedAtMs = 0L
                _timerState.value = decision.timerState
                persistTimerSnapshot(decision.timerState, TimerRunStatus.PAUSED, 0L, 0L)
                cancelAlarm(application)
                setFocusGuardActive(false, 0)
                soundManager.stop()
                dndManager.disableDnd()
            }
            TimerRecoveryDecision.Expired, TimerRecoveryDecision.Invalid -> {
                Log.i(TAG, "Clearing expired or invalid saved timer")
                clearStaleTimer()
            }
        }
    }

    private fun clearStaleTimer() {
        clearRecoveryPrefs()
        cancelAlarm(application)
        setFocusGuardActive(false, 0)
        soundManager.stop()
        dndManager.disableDnd()
    }

    private fun clearRecoveryPrefs() {
        timerPrefs.edit()
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

    private fun currentFlowElapsed(nowMs: Long): Int {
        val base = timerPrefs.getInt(BootReceiver.PREF_ELAPSED_SECONDS, _timerState.value.elapsedSeconds)
            .coerceAtLeast(0)
        val start = timerPrefs.getLong(BootReceiver.PREF_RUN_STARTED_AT_MS, runStartedAtMs)
        val extra = if (timerPrefs.getString(BootReceiver.PREF_STATUS, "") == TimerRunStatus.RUNNING.name &&
            start in 1L..nowMs
        ) ((nowMs - start) / 1_000L) else 0L
        return (base.toLong() + extra).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    private fun ceilSeconds(milliseconds: Long): Int =
        if (milliseconds <= 0L) 0 else ((milliseconds + 999L) / 1_000L)
            .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

    // ========================================================================
    // Private — helpers
    // ========================================================================

    private fun startSoundAndDnd() {
        val sound = ambientSound.value
        if (sound != AmbientSound.NONE) soundManager.play(sound, ambientVolume.value)
        if (dndEnabled.value && dndManager.isDndPermissionGranted()) dndManager.enableDnd()
    }

    // ── Focus Guard helpers ───────────────────────────────────────────────────

    private fun setFocusGuardActive(active: Boolean, remainingSeconds: Int) {
        PreferenceManager.getDefaultSharedPreferences(application)
            .edit()
            .putBoolean(FocusGuardRepository.PREF_FOCUS_GUARD_ACTIVE, active)
            .putInt(FocusGuardRepository.PREF_REMAINING_SECONDS, remainingSeconds)
            .apply()
    }

    // ── Badge evaluation ──────────────────────────────────────────────────────

    private fun evaluateBadgesAfterSave() {
        viewModelScope.launch {
            runCatching {
                val sessions        = sessionDao.observeAll().first()
                val streak          = streakDays.value
                val alreadyUnlocked = settingsRepository.unlockedBadges.first()
                val newIds          = BadgeEvaluator.evaluate(sessions, streak, alreadyUnlocked)
                if (newIds.isNotEmpty()) {
                    settingsRepository.unlockBadges(newIds.toSet())
                    val newBadgeObjects = newIds.mapNotNull { AllBadges.byId[it] }
                    _newBadges.tryEmit(newBadgeObjects)
                    Log.d(TAG, "Unlocked badges: $newIds")
                }
                if (streak in listOf(3, 7, 14, 30, 60, 100)) {
                    TokiAnalytics.logStreakMilestone(streak)
                }
            }.onFailure { Log.e(TAG, "Badge evaluation failed", it) }
        }
    }

    // ── Cloud auto-sync ───────────────────────────────────────────────────

    /** Automatically syncs unsynced sessions after a completed session. Pro only. */
    private fun autoSyncIfPro() {
        if (!isPro.value) return
        viewModelScope.launch {
            runCatching {
                firestoreRepository.ensureAuthenticated()
                firestoreRepository.syncSessionsToCloud()
            }.onFailure { e ->
                Firebase.crashlytics.recordException(e)
                Log.e(TAG, "Auto-sync failed: ${e.message}")
            }
        }
    }

    // ── Deep link entry point ─────────────────────────────────────────────────

    /**
     * Start a focus session from a deep link or home-screen shortcut.
     * Selects the closest matching preset, or uses a custom duration.
     */
    fun startFromDeepLink(durationMinutes: Int) {
        if (!DeepLinkDuration.isSupported(durationMinutes)) {
            Log.w(TAG, "Ignoring unsupported deep-link duration: ${durationMinutes}m")
            return
        }

        // A running or paused session owns its current task association.
        val state = _timerState.value
        if (state.isRunning || state.isPaused) return

        // Find the closest built-in preset or fall back to the default 25-min one
        val matchingPreset = IntervalPreset.entries.firstOrNull {
            it.focusMinutes == durationMinutes
        }

        if (matchingPreset != null) {
            start(matchingPreset, durationMinutesOverride = durationMinutes)
        } else {
            // Custom duration — start with the default preset but override the duration
            val durationSeconds = durationMinutes * 60
            val now = System.currentTimeMillis()
            sessionStartMs = now
            runStartedAtMs = now
            val preset = _timerState.value.preset

            val runningState = _timerState.value.copy(
                phase = TimerPhase.FOCUS,
                preset = preset,
                timerMode = TimerMode.POMODORO,
                totalSeconds = durationSeconds,
                remainingSeconds = durationSeconds,
                elapsedSeconds = 0,
                isRunning = true,
                isPaused = false,
            )
            _timerState.value = runningState
            persistTimerSnapshot(
                runningState,
                TimerRunStatus.RUNNING,
                endTimeMs = now + durationSeconds * 1_000L,
                runStartedAtMs = now,
            )

            setFocusGuardActive(true, durationSeconds)
            TimerAlarmWorker.resetCompletionFlag(application)
            scheduleAlarm(application, durationSeconds, TimerPhase.FOCUS)
            ContextCompat.startForegroundService(
                application,
                TimerForegroundService.buildStartIntent(
                    application, durationSeconds, TimerPhase.FOCUS,
                    totalSeconds = durationSeconds,
                    sessionStartedAtMs = sessionStartMs,
                    preset = preset,
                ),
            )
            startSoundAndDnd()
        }

        Log.d(TAG, "startFromDeepLink duration=${durationMinutes}m")
    }

    private fun sendServiceAction(action: String) {
        val intent = Intent(application, TimerForegroundService::class.java).apply {
            this.action = action
        }
        application.startService(intent)
    }

    // ========================================================================
    // Lifecycle
    // ========================================================================

    override fun onCleared() {
        localBroadcastManager.unregisterReceiver(timerReceiver)
        soundManager.release()
        dndManager.disableDnd()
        // Snapshot user properties so the final state is recorded before ViewModel dies
        TokiAnalytics.setUserProperties(
            isPro         = isPro.value,
            totalSessions = totalCompleted.value,
            streakDays    = streakDays.value,
        )
        Log.d(TAG, "onCleared: receiver unregistered, sound + DND released")
        super.onCleared()
    }

    // ========================================================================
    // Private — streak computation
    // ========================================================================

    private fun computeStreak(epochDays: List<Long>): Int {
        if (epochDays.isEmpty()) return 0
        val todayEpochDay     = System.currentTimeMillis() / 86_400_000L
        val yesterdayEpochDay = todayEpochDay - 1
        val sortedDays        = epochDays.sortedDescending()
        val mostRecent        = sortedDays.first()

        if (mostRecent < yesterdayEpochDay) return 0

        var streak   = 0
        var expected = mostRecent
        for (day in sortedDays) {
            if (day == expected) {
                streak++
                expected--
            } else {
                break
            }
        }
        return streak
    }

    // ========================================================================
    // Companion
    // ========================================================================

    companion object {
        private const val TAG = "TimerViewModel"

        fun todayStartMs(): Long = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE,      0)
            set(Calendar.SECOND,      0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
