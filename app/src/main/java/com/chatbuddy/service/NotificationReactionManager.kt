package com.chatbuddy.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

class NotificationReactionManager(private val service: ChatBuddyService) {

    private val reactions = mutableMapOf<NotificationType, ReactionConfig>()

    init {
        setupDefaultReactions()
    }

    private fun setupDefaultReactions() {
        reactions[NotificationType.MESSAGE] = ReactionConfig(
            expression = BuddyExpression.HAPPY,
            action = BuddyAction.WAVE,
            statusText = "📱 New message!"
        )
        reactions[NotificationType.EMAIL] = ReactionConfig(
            expression = BuddyExpression.CONFUSED,
            action = null,
            statusText = "📧 New email"
        )
        reactions[NotificationType.LOW_BATTERY] = ReactionConfig(
            expression = BuddyExpression.YAWNING,
            action = BuddyAction.SLEEP,
            statusText = "🔋 Battery low..."
        )
        reactions[NotificationType.MISSED_CALL] = ReactionConfig(
            expression = BuddyExpression.SAD,
            action = null,
            statusText = "📞 Missed call"
        )
        reactions[NotificationType.SOCIAL_MENTION] = ReactionConfig(
            expression = BuddyExpression.LOVE,
            action = BuddyAction.DANCE,
            statusText = "❤️ You were mentioned!"
        )
        reactions[NotificationType.REMINDER] = ReactionConfig(
            expression = BuddyExpression.SURPRISED,
            action = null,
            statusText = "⏰ Reminder"
        )
        reactions[NotificationType.ALARM] = ReactionConfig(
            expression = BuddyExpression.ANGRY,
            action = null,
            statusText = "⏰ ALARM!"
        )
    }

    fun setReaction(type: NotificationType, config: ReactionConfig) {
        reactions[type] = config
    }

    fun clearReaction(type: NotificationType) {
        reactions.remove(type)
    }

    fun getReaction(type: NotificationType): ReactionConfig? {
        return reactions[type]
    }

    fun getAllReactions(): Map<NotificationType, ReactionConfig> {
        return reactions.toMap()
    }

    fun processNotification(type: NotificationType) {
        val reaction = reactions[type] ?: return
        reaction.expression?.let { service.triggerExpression(it) }
        reaction.action?.let { service.triggerAction(it) }
        if (reaction.statusText.isNotEmpty()) {
            service.updateStatusText(reaction.statusText)
        }
    }

    data class ReactionConfig(
        val expression: BuddyExpression? = null,
        val action: BuddyAction? = null,
        val statusText: String = ""
    )
}

class BatteryMonitor : BroadcastReceiver() {
    var onBatteryLow: ((Float) -> Unit)? = null

    override fun onReceive(context: Context?, intent: Intent?) {
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: return
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val batteryPct = level * 100 / scale.toFloat()

        when {
            batteryPct <= 10 -> onBatteryLow?.invoke(batteryPct)
        }
    }
}

class NotificationReceiver : BroadcastReceiver() {
    var onNotificationReceived: ((NotificationType) -> Unit)? = null

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            "com.chatbuddy.MESSAGE_RECEIVED" ->
                onNotificationReceived?.invoke(NotificationType.MESSAGE)
            "com.chatbuddy.EMAIL_RECEIVED" ->
                onNotificationReceived?.invoke(NotificationType.EMAIL)
            "com.chatbuddy.MISSED_CALL" ->
                onNotificationReceived?.invoke(NotificationType.MISSED_CALL)
            "com.chatbuddy.SOCIAL_MENTION" ->
                onNotificationReceived?.invoke(NotificationType.SOCIAL_MENTION)
            "com.chatbuddy.REMINDER" ->
                onNotificationReceived?.invoke(NotificationType.REMINDER)
        }
    }

    companion object {
        val FILTER = IntentFilter().apply {
            addAction("com.chatbuddy.MESSAGE_RECEIVED")
            addAction("com.chatbuddy.EMAIL_RECEIVED")
            addAction("com.chatbuddy.MISSED_CALL")
            addAction("com.chatbuddy.SOCIAL_MENTION")
            addAction("com.chatbuddy.REMINDER")
        }
    }
}
