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
            statusText = "�️ ALARM!"
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
            service.updateStatus(reaction.statusText)
        }
    }

    data class ReactionConfig(
        val expression: BuddyExpression? = null,
        val action: BuddyAction? = null,
        val statusText: String = ""
    )
}

class BatteryMonitor(private val service: ChatBuddyService) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: return
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val batteryPct = level * 100 / scale.toFloat()
        
        when {
            batteryPct <= 10 -> service.reactToNotification(NotificationType.LOW_BATTERY)
            batteryPct <= 20 -> service.updateStatus("🔋 $batteryPct%")
        }
    }
}

class NotificationReceiver(private val service: ChatBuddyService) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            "com.chatbuddy.MESSAGE_RECEIVED" -> {
                service.reactToNotification(NotificationType.MESSAGE)
            }
            "com.chatbuddy.EMAIL_RECEIVED" -> {
                service.reactToNotification(NotificationType.EMAIL)
            }
            "com.chatbuddy.MISSED_CALL" -> {
                service.reactToNotification(NotificationType.MISSED_CALL)
            }
            "com.chatbuddy.SOCIAL_MENTION" -> {
                service.reactToNotification(NotificationType.SOCIAL_MENTION)
            }
            "com.chatbuddy.REMINDER" -> {
                service.reactToNotification(NotificationType.REMINDER)
            }
        }
    }
}
