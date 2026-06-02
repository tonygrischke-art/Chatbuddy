package com.chatbuddy.service

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.core.animation.doOnEnd
import com.chatbuddy.R

class BuddyOverlayLayout(
    private val context: Context,
    private val onAction: ((OverlayAction) -> Unit)? = null
) : FrameLayout(context) {

    enum class OverlayAction { CUSTOMIZE, HIDE }

    private val buddyImageView: ImageView
    private val expressionView: TextView
    private val statusView: TextView

    private var isCustomizing = false

    init {
        val bg = GradientDrawable().apply {
            setColor(Color.parseColor("#CC000000"))
            cornerRadius = 24f
        }
        background = bg
        elevation = 8f

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            val pad = dp(8)
            setPadding(pad, pad, pad, pad)
        }

        buddyImageView = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(80), dp(80))
            setImageResource(R.drawable.buddy_default)
            contentDescription = "Chat Buddy"
        }

        expressionView = TextView(context).apply {
            text = ""
            textSize = 20f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(2)
            }
        }

        statusView = TextView(context).apply {
            text = "Hi there!"
            setTextColor(Color.WHITE)
            textSize = 11f
            gravity = Gravity.CENTER
            maxLines = 1
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(2)
            }
        }

        container.addView(buddyImageView)
        container.addView(expressionView)
        container.addView(statusView)

        val totalPad = dp(12)
        setPadding(totalPad, totalPad, totalPad, totalPad)
        addView(container)
    }

    override fun performClick() {
        // Double-tap logic: single tap = hello reaction, long hold = menu
        triggerExpression(BuddyExpression.HAPPY)
        updateStatusText("Hello! 👋")
    }

    fun showCustomization(onConfigChanged: (CharacterConfig) -> Unit) {
        isCustomizing = true
        val winMgr = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return

        val customizationView = CustomizationPanel(context) { config ->
            onConfigChanged(config)
            try {
                winMgr.removeView(this)
            } catch (_: Exception) {}
            isCustomizing = false
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        winMgr.addView(customizationView, params)
    }

    fun triggerAction(action: BuddyAction) {
        expressionView.text = when (action) {
            BuddyAction.YAWNING -> "💤"
            BuddyAction.KNOCKING -> "👊"
            BuddyAction.PLAYING_WITH_ICONS -> "🎮"
            BuddyAction.WAVE -> "👋"
            BuddyAction.DANCE -> "💃"
            BuddyAction.SLEEP -> "😴"
        }
        statusView.text = when (action) {
            BuddyAction.YAWNING -> "Yawning..."
            BuddyAction.KNOCKING -> "Knocking..."
            BuddyAction.PLAYING_WITH_ICONS -> "Playing..."
            BuddyAction.WAVE -> "Waving!"
            BuddyAction.DANCE -> "Dancing!"
            BuddyAction.SLEEP -> "Sleeping..."
        }

        // Bounce animation
        ObjectAnimator.ofFloat(this, "scaleX", 1f, 1.2f, 1f).apply {
            duration = 400
            doOnEnd { scaleX = 1f }
            start()
        }
        ObjectAnimator.ofFloat(this, "scaleY", 1f, 1.2f, 1f).apply {
            duration = 400
            doOnEnd { scaleY = 1f }
            start()
        }
    }

    fun triggerExpression(expression: BuddyExpression) {
        buddyImageView.setImageResource(getDrawableForExpression(expression))
        expressionView.text = getEmojiForExpression(expression)
    }

    fun reactToNotification(type: NotificationType) {
        when (type) {
            NotificationType.MESSAGE -> {
                triggerExpression(BuddyExpression.HAPPY)
                updateStatusText("New message! 📱")
            }
            NotificationType.EMAIL -> {
                triggerExpression(BuddyExpression.CONFUSED)
                updateStatusText("New email! 📧")
            }
            NotificationType.LOW_BATTERY -> {
                triggerExpression(BuddyExpression.YAWNING)
                updateStatusText("Battery low! 🔋")
            }
            NotificationType.MISSED_CALL -> {
                triggerExpression(BuddyExpression.SAD)
                updateStatusText("Missed call! 📞")
            }
            NotificationType.SOCIAL_MENTION -> {
                triggerExpression(BuddyExpression.LOVE)
                updateStatusText("Mentioned! ❤️")
            }
            NotificationType.REMINDER -> {
                triggerExpression(BuddyExpression.SURPRISED)
                updateStatusText("Reminder! ⏰")
            }
            NotificationType.ALARM -> {
                triggerExpression(BuddyExpression.ANGRY)
                updateStatusText("ALARM! ⏰")
            }
            NotificationType.CUSTOM -> {
                triggerExpression(BuddyExpression.CONFUSED)
                updateStatusText("...")
            }
        }
    }

    fun updateStatusText(text: String) {
        statusView.text = text
    }

    private fun dp(value: Int): Int {
        return (value * context.resources.displayMetrics.density).toInt()
    }

    private fun getDrawableForExpression(expression: BuddyExpression): Int {
        return when (expression) {
            BuddyExpression.HAPPY -> R.drawable.buddy_happy
            BuddyExpression.SAD -> R.drawable.buddy_sad
            BuddyExpression.ANGRY -> R.drawable.buddy_angry
            BuddyExpression.LAUGHING -> R.drawable.buddy_laughing
            BuddyExpression.CRYING -> R.drawable.buddy_crying
            BuddyExpression.YAWNING -> R.drawable.buddy_sleepy
            BuddyExpression.SLEEPY -> R.drawable.buddy_sleepy
            BuddyExpression.SURPRISED -> R.drawable.buddy_surprised
            BuddyExpression.CONFUSED -> R.drawable.buddy_confused
            BuddyExpression.LOVE -> R.drawable.buddy_love
        }
    }

    private fun getEmojiForExpression(expression: BuddyExpression): String {
        return when (expression) {
            BuddyExpression.HAPPY -> "😊"
            BuddyExpression.SAD -> "😢"
            BuddyExpression.ANGRY -> "😠"
            BuddyExpression.LAUGHING -> "😂"
            BuddyExpression.CRYING -> "😭"
            BuddyExpression.YAWNING -> "🥱"
            BuddyExpression.SLEEPY -> "😴"
            BuddyExpression.SURPRISED -> "😲"
            BuddyExpression.CONFUSED -> "😕"
            BuddyExpression.LOVE -> "🥰"
        }
    }

    // --- Character customization data ---
    data class CharacterConfig(
        val skinTone: SkinTone = SkinTone.NORMAL,
        val hairStyle: HairStyle = HairStyle.SHORT,
        val hairColor: HairColor = HairColor.BLACK,
        val bodyType: BodyType = BodyType.NORMAL,
        val clothing: Clothing = Clothing.CASUAL,
        val accessories: List<Accessory> = emptyList()
    )

    enum class SkinTone { LIGHT, NORMAL, TAN, DARK }
    enum class HairStyle { SHORT, LONG, CURLY, PONYTAIL, BUN, BRAIDS, MOHAWK }
    enum class HairColor { BLACK, BROWN, BLONDE, RED, BLUE, PINK, GREEN, GRAY }
    enum class BodyType { SLIM, NORMAL, CHUBBY }
    enum class Clothing { CASUAL, FORMAL, SPORTS, PAJAMAS, COSTUME }
    enum class Accessory { GLASSES, HAT, EARRINGS, NECKLACE, WATCH, HEADPHONES }
}

@SuppressLint("ViewConstructor")
class CustomizationPanel(
    context: Context,
    private val onComplete: (BuddyOverlayLayout.CharacterConfig) -> Unit
) : LinearLayout(context) {

    private var config = BuddyOverlayLayout.CharacterConfig()

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        val bg = GradientDrawable().apply {
            setColor(Color.parseColor("#EE222222"))
            cornerRadius = 28f
        }
        background = bg
        val pad = (20 * context.resources.displayMetrics.density).toInt()
        setPadding(pad, pad, pad, pad)

        addTitle("Customize Your Buddy")

        addSection("Skin Tone", BuddyOverlayLayout.SkinTone.entries.toList()) { tone ->
            config = config.copy(skinTone = tone)
        }
        addSection("Hair Style", BuddyOverlayLayout.HairStyle.entries.take(6).toList()) { style ->
            config = config.copy(hairStyle = style)
        }
        addSection("Hair Color", BuddyOverlayLayout.HairColor.entries.take(6).toList()) { color ->
            config = config.copy(hairColor = color)
        }
        addSection("Body", BuddyOverlayLayout.BodyType.entries.toList()) { type ->
            config = config.copy(bodyType = type)
        }
        addSection("Clothes", BuddyOverlayLayout.Clothing.entries.take(4).toList()) { clothing ->
            config = config.copy(clothing = clothing)
        }

        addDoneButton()
    }

    private fun addTitle(text: String) {
        addView(TextView(context).apply {
            this.text = text
            setTextColor(Color.WHITE)
            textSize = 16f
            gravity = Gravity.CENTER
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = (12 * context.resources.displayMetrics.density).toInt()
            }
            layoutParams = lp
        })
    }

    private fun <T> addSection(label: String, items: List<T>, onClick: (T) -> Unit) {
        val density = context.resources.displayMetrics.density
        addView(TextView(context).apply {
            text = label
            setTextColor(Color.parseColor("#BBBBBB"))
            textSize = 11f
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = (4 * density).toInt()
            }
            layoutParams = lp
        })

        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
        }

        items.forEach { item ->
            val btn = android.widget.Button(context).apply {
                text = item.toString().take(5)
                textSize = 10f
                setTextColor(Color.WHITE)
                val btnBg = GradientDrawable().apply {
                    setColor(Color.parseColor("#444444"))
                    cornerRadius = 12f
                }
                background = btnBg
                val m = (4 * density).toInt()
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    (32 * density).toInt()
                ).apply {
                    setMargins(m, m, m, m)
                }
                layoutParams = lp
                setPadding(m, 0, m, 0)
                setOnClickListener { onClick(item) }
            }
            row.addView(btn)
        }
        addView(row)

        addView(View(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, (8 * density).toInt())
        })
    }

    private fun addDoneButton() {
        val density = context.resources.displayMetrics.density
        addView(android.widget.Button(context).apply {
            text = "Done ✓"
            setTextColor(Color.WHITE)
            val btnBg = GradientDrawable().apply {
                setColor(Color.parseColor("#FF9800"))
                cornerRadius = 16f
            }
            background = btnBg
            val lp = LinearLayout.LayoutParams(
                (140 * density).toInt(),
                (40 * density).toInt()
            ).apply {
                topMargin = (8 * density).toInt()
            }
            layoutParams = lp
            setOnClickListener { onComplete(config) }
        })
    }
}
