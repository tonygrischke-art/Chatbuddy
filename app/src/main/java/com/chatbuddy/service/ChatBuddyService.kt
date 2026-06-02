package com.chatbuddy.service

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.*
import android.widget.*
import androidx.core.animation.doOnEnd
import androidx.lifecycle.LifecycleService
import com.chatbuddy.MainActivity
import com.chatbuddy.R

class ChatBuddyService : LifecycleService() {

    companion object {
        const val CHANNEL_ID = "ChatBuddyChannel"
        const val NOTIFICATION_ID = 1
        const val ACTION_SHOW_BUDDY = "com.chatbuddy.ACTION_SHOW_BUDDY"
        const val ACTION_HIDE_BUDDY = "com.chatbuddy.ACTION_HIDE_BUDDY"

        var isRunning = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, ChatBuddyService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ChatBuddyService::class.java))
        }
    }

    private lateinit var windowManager: WindowManager
    private var buddyView: View? = null
    private var buddyLayout: BuddyOverlayLayout? = null

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    private var batteryMonitor: BatteryMonitor? = null
    private var notificationReceiver: NotificationReceiver? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()

        batteryMonitor = BatteryMonitor().apply {
            onBatteryLow = { pct ->
                reactToNotification(NotificationType.LOW_BATTERY)
                updateStatusText("🔋 ${pct.toInt()}%")
            }
        }
        registerReceiver(batteryMonitor, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        notificationReceiver = NotificationReceiver().apply {
            onNotificationReceived = { type ->
                reactToNotification(type)
            }
        }
        registerReceiver(notificationReceiver, NotificationReceiver.FILTER)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startForeground(NOTIFICATION_ID, createNotification())

        when (intent?.action) {
            ACTION_SHOW_BUDDY -> showBuddy()
            ACTION_HIDE_BUDDY -> hideBuddy()
            else -> showBuddy()
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent): android.os.IBinder? {
        super.onBind(intent)
        return null
    }

    private fun createNotificationChannel() {
        val channel = android.app.NotificationChannel(
            CHANNEL_ID,
            "ChatBuddy Service",
            android.app.NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Chat Buddy overlay service"
            setShowBadge(false)
        }
        val notificationManager = getSystemService(android.app.NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): android.app.Notification {
        val mainIntent = Intent(this, MainActivity::class.java)
        val pendingMainIntent = android.app.PendingIntent.getActivity(
            this, 0, mainIntent,
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )

        val hideIntent = Intent(this, ChatBuddyService::class.java).apply {
            action = ACTION_HIDE_BUDDY
        }
        val pendingHideIntent = android.app.PendingIntent.getService(
            this, 1, hideIntent,
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )

        return androidx.core.app.NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ChatBuddy")
            .setContentText("Your buddy is active")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingMainIntent)
            .addAction(android.R.drawable.ic_menu_view, "Hide", pendingHideIntent)
            .setOngoing(true)
            .build()
    }

    @SuppressLint("InflateParams")
    private fun showBuddy() {
        if (buddyView != null) return

        buddyLayout = BuddyOverlayLayout(this) { action ->
            when (action) {
                BuddyOverlayLayout.OverlayAction.CUSTOMIZE -> showCustomization()
                BuddyOverlayLayout.OverlayAction.HIDE -> hideBuddy()
            }
        }
        buddyView = buddyLayout

        buddyView?.setOnTouchListener { _, event ->
            val params = buddyView?.layoutParams as? WindowManager.LayoutParams ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - initialTouchX
                    val deltaY = event.rawY - initialTouchY
                    if (kotlin.math.abs(deltaX) > 10 || kotlin.math.abs(deltaY) > 10) {
                        isDragging = true
                        params.x = (initialX + deltaX).toInt()
                        params.y = (initialY + deltaY).toInt()
                        windowManager.updateViewLayout(buddyView, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        buddyLayout?.performClick()
                    }
                    true
                }
                else -> false
            }
        }

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        buddyView?.let {
            windowManager.addView(it, params)
        }
    }

    private fun hideBuddy() {
        buddyView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
            buddyView = null
            buddyLayout = null
        }
    }

    private fun showCustomization() {
        buddyLayout?.showCustomization { config ->
            // Config applied via callback
        }
    }

    fun triggerAction(action: BuddyAction) {
        buddyLayout?.triggerAction(action)
    }

    fun triggerExpression(expression: BuddyExpression) {
        buddyLayout?.triggerExpression(expression)
    }

    fun reactToNotification(type: NotificationType) {
        buddyLayout?.reactToNotification(type)
    }

    fun updateStatusText(text: String) {
        buddyLayout?.updateStatusText(text)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        try {
            batteryMonitor?.let { unregisterReceiver(it) }
        } catch (_: Exception) {}
        try {
            notificationReceiver?.let { unregisterReceiver(it) }
        } catch (_: Exception) {}
        hideBuddy()
    }
}

enum class BuddyAction {
    YAWNING, KNOCKING, PLAYING_WITH_ICONS, WAVE, DANCE, SLEEP
}

enum class BuddyExpression {
    HAPPY, SAD, ANGRY, LAUGHING, CRYING, YAWNING, SLEEPY, SURPRISED, CONFUSED, LOVE
}

enum class NotificationType {
    MESSAGE, EMAIL, LOW_BATTERY, MISSED_CALL, SOCIAL_MENTION, REMINDER, ALARM, CUSTOM
}
