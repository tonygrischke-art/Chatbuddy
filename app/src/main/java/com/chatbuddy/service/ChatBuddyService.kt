package com.chatbuddy.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.FrameLayout
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.chatbuddy.MainActivity
import com.chatbuddy.R
import kotlinx.coroutines.launch
import kotlin.math.abs

class ChatBuddyService : LifecycleService() {

    companion object {
        const val CHANNEL_ID = "ChatBuddyChannel"
        const val NOTIFICATION_ID = 1
        const val ACTION_SHOW_BUDDY = "com.chatbuddy.ACTION_SHOW_BUDDY"
        const val ACTION_HIDE_BUDDY = "com.chatbuddy.ACTION_HIDE_BUDDY"
        const val ACTION_CUSTOMIZE = "com.chatbuddy.ACTION_CUSTOMIZE"
        
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
    
    private var isBuddyVisible = true
    
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startForeground(NOTIFICATION_ID, createNotification())
        
        when (intent?.action) {
            ACTION_SHOW_BUDDY -> showBuddy()
            ACTION_HIDE_BUDDY -> hideBuddy()
            ACTION_CUSTOMIZE -> showCustomization()
            else -> showBuddy()
        }
        
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "ChatBuddy Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Chat Buddy overlay service"
            setShowBadge(false)
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val mainIntent = Intent(this, MainActivity::class.java)
        val pendingMainIntent = PendingIntent.getActivity(
            this, 0, mainIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val hideIntent = Intent(this, ChatBuddyService::class.java).apply {
            action = ACTION_HIDE_BUDDY
        }
        val pendingHideIntent = PendingIntent.getService(
            this, 1, hideIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val customizeIntent = Intent(this, ChatBuddyService::class.java).apply {
            action = ACTION_CUSTOMIZE
        }
        val pendingCustomizeIntent = PendingIntent.getService(
            this, 2, customizeIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ChatBuddy")
            .setContentText("Your character is active")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingMainIntent)
            .addAction(android.R.drawable.ic_menu_view, "Hide", pendingHideIntent)
            .addAction(android.R.drawable.ic_menu_edit, "Customize", pendingCustomizeIntent)
            .setOngoing(true)
            .build()
    }

    @SuppressLint("ClickableViewAccessibility", "InflateParams")
    private fun showBuddy() {
        if (buddyView != null) return

        buddyLayout = BuddyOverlayLayout(this)
        buddyView = buddyLayout
        
        buddyView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = 100
                    initialY = 300
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - initialTouchX
                    val deltaY = event.rawY - initialTouchY
                    if (abs(deltaX) > 10 || abs(deltaY) > 10) {
                        isDragging = true
                        val params = buddyView?.layoutParams as? WindowManager.LayoutParams
                        params?.let {
                            it.x = (initialX + deltaX).toInt()
                            it.y = (initialY + deltaY).toInt()
                            windowManager.updateViewLayout(buddyView, it)
                        }
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

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
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
            windowManager.removeView(it)
            buddyView = null
            buddyLayout = null
        }
    }

    private fun showCustomization() {
        buddyLayout?.showCustomization()
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

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        hideBuddy()
    }
}

enum class BuddyAction {
    YAWNING,
    KNOCKING,
    PLAYING_WITH_ICONS,
    WAVE,
    DANCE,
    SLEEP
}

enum class BuddyExpression {
    HAPPY,
    SAD,
    ANGRY,
    LAUGHING,
    CRYING,
    YAWNING,
    SLEEPY,
    SURPRISED,
    CONFUSED,
    LOVE
}

enum class NotificationType {
    MESSAGE,
    EMAIL,
    LOW_BATTERY,
    MISSED_CALL,
    SOCIAL_MENTION,
    REMINDER,
    ALARM,
    CUSTOM
}
