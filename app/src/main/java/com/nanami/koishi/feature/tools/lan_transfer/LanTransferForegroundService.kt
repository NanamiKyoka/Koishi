package com.nanami.koishi.feature.tools.lan_transfer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.nanami.koishi.MainActivity
import com.nanami.koishi.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class LanTransferForegroundService : Service(), KoinComponent {

    private val engine by inject<LanTransferEngine>()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var started = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        engine.initialize()
        serviceScope.launch {
            engine.uiState.collect { state ->
                if (started) {
                    getSystemService(NotificationManager::class.java)
                        .notify(NOTIFICATION_ID, buildNotification(state))
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        engine.initialize()
        if (intent?.action == ACTION_CANCEL) {
            intent.getStringExtra(EXTRA_TRANSFER_ID)?.let { id ->
                engine.onEvent(LanTransferUiEvent.OnCancelTransfer(id))
            }
        }
        startForeground(NOTIFICATION_ID, buildNotification(engine.uiState.value))
        started = true
        return START_STICKY
    }

    override fun onDestroy() {
        started = false
        serviceScope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun buildNotification(state: LanTransferUiState): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.lan_transfer_title),
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val activeTransfer = state.transfers.firstOrNull { it.status == TransferItemStatus.IN_PROGRESS }
        val contentText = when {
            state.incomingRequest is IncomingRequestUi.Text ->
                getString(R.string.lan_transfer_notification_text)
            state.incomingRequest is IncomingRequestUi.Files -> getString(
                R.string.lan_transfer_notification_incoming,
                state.incomingRequest.fileCount
            )
            activeTransfer != null -> getString(
                R.string.lan_transfer_notification_progress,
                activeTransfer.fileName,
                (activeTransfer.progress * 100).toInt()
            )
            else -> getString(R.string.lan_transfer_receive_desc)
        }
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.lan_transfer_title))
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .setProgress(100, (activeTransfer?.progress?.times(100) ?: 0f).toInt(), activeTransfer == null)
        if (activeTransfer != null) {
            val cancelIntent = Intent(this, LanTransferForegroundService::class.java)
                .setAction(ACTION_CANCEL)
                .putExtra(EXTRA_TRANSFER_ID, activeTransfer.id)
            val cancelPendingIntent = PendingIntent.getService(
                this,
                1,
                cancelIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.addAction(
                R.drawable.ic_launcher_foreground,
                getString(R.string.lan_transfer_cancel),
                cancelPendingIntent
            )
        }
        return builder.build()
    }

    companion object {
        private const val CHANNEL_ID = "lan_transfer_receive"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_CANCEL = "com.nanami.koishi.lan_transfer.CANCEL"
        private const val EXTRA_TRANSFER_ID = "transfer_id"

        fun start(context: Context) {
            val intent = Intent(context, LanTransferForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LanTransferForegroundService::class.java))
        }
    }
}
