package com.glassfiles.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.glassfiles.MainActivity
import com.glassfiles.R
import com.glassfiles.notifications.NotificationChannels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AiAgentService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var wakeLock: PowerManager.WakeLock? = null
    private var foreground = false

    private val _state = MutableStateFlow<AgentState>(AgentState.Idle)
    val state: StateFlow<AgentState> = _state.asStateFlow()

    inner class LocalBinder : Binder() {
        fun getService(): AiAgentService = this@AiAgentService
    }

    override fun onCreate() {
        super.onCreate()
        activeInstance = this
    }

    override fun onBind(intent: Intent?): IBinder = LocalBinder()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                cancelTask("Stopped by user")
                return START_STICKY
            }
            ACTION_INTERRUPTED -> {
                _state.value = AgentState.Interrupted
                startForegroundNotification(buildNotification("Task interrupted, please restart"))
                return START_STICKY
            }
        }
        if (pendingStart != null) {
            consumePendingStart()
        } else if (_state.value is AgentState.Running) {
            val running = _state.value as AgentState.Running
            startForegroundNotification(buildNotification(running.status, running.task, running.progress))
        } else {
            startForegroundNotification(buildNotification("Idle"))
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.coroutineContext.cancelChildren()
        releaseWakeLock()
        activeInstance = null
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (_state.value !is AgentState.Running) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    fun startTask(task: AgentTask, keepCpuAwake: Boolean, onCancel: () -> Unit) {
        if (_state.value is AgentState.Running) return
        activeCancel = onCancel
        _state.value = AgentState.Running(task = task, status = "Starting", progress = null)
        startForegroundNotification(buildNotification("Starting", task))
        if (keepCpuAwake) acquireWakeLockIfAllowed()
    }

    fun updateStatus(status: String, progress: AgentProgress? = null) {
        val running = _state.value as? AgentState.Running ?: return
        _state.value = running.copy(status = status, progress = progress)
        notificationManager.notify(NOTIFICATION_ID, buildNotification(status, running.task, progress))
    }

    fun completeTask(summary: String) {
        releaseWakeLock()
        activeCancel = null
        _state.value = AgentState.Completed(summary)
        notificationManager.notify(NOTIFICATION_ID, buildNotification(summary))
        scope.launch {
            delay(5_000)
            stopForegroundCompat()
        }
    }

    fun cancelTask(message: String = "Stopped by user") {
        scope.coroutineContext[Job]?.cancelChildren()
        val callback = activeCancel
        activeCancel = null
        callback?.invoke()
        releaseWakeLock()
        _state.value = AgentState.Stopped(message)
        notificationManager.notify(NOTIFICATION_ID, buildNotification(message))
        scope.launch {
            delay(3_000)
            stopForegroundCompat()
        }
    }

    private fun consumePendingStart() {
        val pending = pendingStart ?: return
        pendingStart = null
        startTask(
            task = pending.task,
            keepCpuAwake = pending.keepCpuAwake,
            onCancel = pending.onCancel,
        )
    }

    private fun acquireWakeLockIfAllowed() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        val batteryLow = batteryPercent() in 0..14
        val powerSave = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            pm.isPowerSaveMode
        } else {
            false
        }
        if (batteryLow || powerSave) {
            updateStatus(
                if (batteryLow) "WakeLock disabled due to low battery"
                else "WakeLock disabled due to power saving mode",
            )
            return
        }
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "GlassFiles:AiAgent",
        ).apply {
            setReferenceCounted(false)
            acquire(30 * 60 * 1000L)
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.takeIf { it.isHeld }?.release()
        } catch (_: Throwable) {
        }
        wakeLock = null
    }

    private fun batteryPercent(): Int {
        val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return -1
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return -1
        return ((level * 100f) / scale).toInt()
    }

    private fun buildNotification(
        text: String,
        task: AgentTask? = (_state.value as? AgentState.Running)?.task,
        progress: AgentProgress? = (_state.value as? AgentState.Running)?.progress,
    ): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_OPEN_AI_AGENT, true)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, AiAgentService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val title = task?.let { "AI Agent · ${it.repo}@${it.branch}" } ?: "AI Agent"
        val builder = NotificationCompat.Builder(this, NotificationChannels.CHANNEL_AI_AGENT_WORK)
            .setSmallIcon(R.drawable.ic_notification_glassfiles)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setOngoing(_state.value is AgentState.Running)
            .setContentIntent(openIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(R.drawable.ic_notification_glassfiles, "Open", openIntent)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopIntent)
        if (progress != null && progress.total > 0) {
            builder.setProgress(progress.total, progress.current.coerceAtMost(progress.total), false)
        }
        return builder.build()
    }

    private fun startForegroundNotification(notification: Notification) {
        if (!foreground) {
            startForeground(NOTIFICATION_ID, notification)
            foreground = true
        } else {
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun stopForegroundCompat() {
        if (!foreground) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        foreground = false
    }

    private val notificationManager: NotificationManager
        get() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val ACTION_STOP = "com.glassfiles.service.AI_AGENT_STOP"
        const val ACTION_INTERRUPTED = "com.glassfiles.service.AI_AGENT_INTERRUPTED"
        const val EXTRA_OPEN_AI_AGENT = "open_ai_agent"
        const val NOTIFICATION_ID = 4207

        @Volatile
        private var activeCancel: (() -> Unit)? = null

        @Volatile
        private var activeInstance: AiAgentService? = null

        @Volatile
        private var pendingStart: PendingStart? = null

        fun begin(
            context: Context,
            task: AgentTask,
            keepCpuAwake: Boolean,
            onCancel: () -> Unit,
        ) {
            pendingStart = PendingStart(task, keepCpuAwake, onCancel)
            val intent = Intent(context, AiAgentService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            activeInstance?.let {
                pendingStart = null
                it.startTask(task, keepCpuAwake, onCancel)
            }
        }

        fun update(status: String, progress: AgentProgress? = null) {
            activeInstance?.updateStatus(status, progress)
        }

        fun complete(summary: String) {
            activeInstance?.completeTask(summary)
        }

        fun stop(message: String = "Stopped by user") {
            activeInstance?.cancelTask(message)
        }
    }

    private data class PendingStart(
        val task: AgentTask,
        val keepCpuAwake: Boolean,
        val onCancel: () -> Unit,
    )
}

data class AgentTask(
    val repo: String,
    val branch: String,
    val prompt: String,
)

data class AgentProgress(
    val current: Int,
    val total: Int,
)

sealed class AgentState {
    data object Idle : AgentState()
    data class Running(
        val task: AgentTask,
        val status: String,
        val progress: AgentProgress?,
    ) : AgentState()
    data class Completed(val summary: String) : AgentState()
    data class Stopped(val reason: String) : AgentState()
    data object Interrupted : AgentState()
}
