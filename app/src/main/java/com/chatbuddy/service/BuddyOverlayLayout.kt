package com.chatbuddy.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.chatbuddy.R

class BuddyOverlayLayout(
    context: Context
) : FrameLayout(context) {

    private val buddyImageView: ImageView
    private val expressionView: TextView
    private val statusView: TextView
    
    private var characterConfig = CharacterConfig()
    private var isCustomizing = false
    
    init {
        LayoutInflater.from(context).inflate(R.layout.layout_buddy, this, true)
        
        buddyImageView = findViewById(R.id.buddyImage)
        expressionView = findViewById(R.id.expressionText)
        statusView = findViewById(R.id.statusText)
        
        updateCharacter()
    }

    fun performClick() {
        if (isCustomizing) {
            isCustomizing = false
            updateCharacter()
        } else {
            showCustomization()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun showCustomization() {
        isCustomizing = true
        
        val customizationView = CustomizationPanel(context) { config ->
            characterConfig = config
            updateCharacter()
            isCustomizing = false
        }
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = android.view.Gravity.CENTER
        }
        
        (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.addView(customizationView, params)
    }

    fun triggerAction(action: BuddyAction) {
        statusView.text = when (action) {
            BuddyAction.YAWNING -> "💤 Yawning..."
            BuddyAction.KNOCKING -> "👊 Knocking..."
            BuddyAction.PLAYING_WITH_ICONS -> "🎮 Playing..."
            BuddyAction.WAVE -> "👋 Wave!"
            BuddyAction.DANCE -> "💃 Dancing!"
            BuddyAction.SLEEP -> "😴 Sleeping..."
        }
        
        performAnimation(action)
    }

    fun triggerExpression(expression: BuddyExpression) {
        expressionView.text = when (expression) {
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

    fun reactToNotification(type: NotificationType) {
        when (type) {
            NotificationType.MESSAGE -> {
                triggerExpression(BuddyExpression.HAPPY)
                statusView.text = "📱 New message!"
            }
            NotificationType.EMAIL -> {
                triggerExpression(BuddyExpression.CONFUSED)
                statusView.text = "📧 New email!"
            }
            NotificationType.LOW_BATTERY -> {
                triggerExpression(BuddyExpression.YAWNING)
                statusView.text = "🔋 Battery low..."
            }
            NotificationType.MISSED_CALL -> {
                triggerExpression(BuddyExpression.SAD)
                statusView.text = "📞 Missed call!"
            }
            NotificationType.SOCIAL_MENTION -> {
                triggerExpression(BuddyExpression.LOVE)
                statusView.text = "❤️ Mentioned!"
            }
            NotificationType.REMINDER -> {
                triggerExpression(BuddyExpression.SURPRISED)
                statusView.text = "⏰ Reminder!"
            }
            NotificationType.ALARM -> {
                triggerExpression(BuddyExpression.ANGRY)
                statusView.text = "⏰ ALARM!"
            }
            NotificationType.CUSTOM -> {
                triggerExpression(BuddyExpression.CONFUSED)
            }
        }
    }

    private fun performAnimation(action: BuddyAction) {
        when (action) {
            BuddyAction.YAWNING -> {
                triggerExpression(BuddyExpression.YAWNING)
            }
            BuddyAction.KNOCKING -> {
                triggerExpression(BuddyExpression.ANGRY)
            }
            BuddyAction.PLAYING_WITH_ICONS -> {
                triggerExpression(BuddyExpression.HAPPY)
            }
            else -> {}
        }
    }

    private fun updateCharacter() {
        // Update character appearance based on config
        // This would load actual character assets based on configuration
        buddyImageView.setImageResource(android.R.drawable.ic_menu_compass)
        
        // Apply skin tone
        buddyImageView.alpha = if (characterConfig.skinTone == SkinTone.DARK) 0.7f else 1f
        
        // Apply body type (would affect size)
        val scale = when (characterConfig.bodyType) {
            BodyType.SLIM -> 0.8f
            BodyType.NORMAL -> 1.0f
            BodyType.CHUBBY -> 1.2f
        }
        scaleX = scale
        scaleY = scale
    }

    data class CharacterConfig(
        val skinTone: SkinTone = SkinTone.NORMAL,
        val hairStyle: HairStyle = HairStyle.SHORT,
        val hairColor: HairColor = HairColor.BLACK,
        val bodyType: BodyType = BodyType.NORMAL,
        val clothing: Clothing = Clothing.CASUAL,
        val accessories: List<Accessory> = emptyList()
    )

    enum class SkinTone { LIGHT, NORMAL, TAN, DARK }
    enum class HairStyle { SHORT, LONG, CURLY, PONYTAIL, BUN, BRAIDS, MOHawk }
    enum class HairColor { BLACK, BROWN, BLONDE, RED, BLUE, PINK, GREEN, GRAY }
    enum class BodyType { SLIM, NORMAL, CHUBBY }
    enum class Clothing { CASUAL, FORMAL, SPORTS, PAJAMAS, COSTUME }
    enum class Accessory { GLASSES, HAT, EARRINGS, NECKLACE, WATCH, HEADPHONES }
}

class CustomizationPanel(
    context: Context,
    private val onComplete: (BuddyOverlayLayout.CharacterConfig) -> Unit
) : LinearLayout(context) {

    private var config = BuddyOverlayLayout.CharacterConfig()

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.DKGRAY)
        setPadding(16, 16, 16, 16)
        
        addTitle("Customize Your Buddy")
        addSkinToneSelector()
        addHairStyleSelector()
        addHairColorSelector()
        addBodyTypeSelector()
        addClothingSelector()
        addDoneButton()
    }

    private fun addTitle(text: String) {
        val title = TextView(context).apply {
            this.text = text
            setTextColor(Color.WHITE)
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
        }
        addView(title)
    }

    private fun addSkinToneSelector() {
        addTitle("Skin Tone")
        val tones = BuddyOverlayLayout.SkinTone.entries
        val buttons = LinearLayout(context).apply {
            orientation = HORIZONTAL
        }
        tones.forEach { tone ->
            val btn = android.widget.Button(context).apply {
                text = tone.name
                setOnClickListener {
                    config = config.copy(skinTone = tone)
                }
            }
            buttons.addView(btn)
        }
        addView(buttons)
    }

    private fun addHairStyleSelector() {
        addTitle("Hair Style")
        val styles = BuddyOverlayLayout.HairStyle.entries
        val buttons = LinearLayout(context).apply {
            orientation = HORIZONTAL
        }
        styles.take(4).forEach { style ->
            val btn = android.widget.Button(context).apply {
                text = style.name.take(4)
                setOnClickListener {
                    config = config.copy(hairStyle = style)
                }
            }
            buttons.addView(btn)
        }
        addView(buttons)
    }

    private fun addHairColorSelector() {
        addTitle("Hair Color")
        val colors = BuddyOverlayLayout.HairColor.entries
        val buttons = LinearLayout(context).apply {
            orientation = HORIZONTAL
        }
        colors.take(4).forEach { color ->
            val btn = android.widget.Button(context).apply {
                text = color.name.take(4)
                setOnClickListener {
                    config = config.copy(hairColor = color)
                }
            }
            buttons.addView(btn)
        }
        addView(buttons)
    }

    private fun addBodyTypeSelector() {
        addTitle("Body Type")
        val types = BuddyOverlayLayout.BodyType.entries
        val buttons = LinearLayout(context).apply {
            orientation = HORIZONTAL
        }
        types.forEach { type ->
            val btn = android.widget.Button(context).apply {
                text = type.name
                setOnClickListener {
                    config = config.copy(bodyType = type)
                }
            }
            buttons.addView(btn)
        }
        addView(buttons)
    }

    private fun addClothingSelector() {
        addTitle("Clothing")
        val clothes = BuddyOverlayLayout.Clothing.entries
        val buttons = LinearLayout(context).apply {
            orientation = HORIZONTAL
        }
        clothes.take(4).forEach { clothing ->
            val btn = android.widget.Button(context).apply {
                text = clothing.name.take(5)
                setOnClickListener {
                    config = config.copy(clothing = clothing)
                }
            }
            buttons.addView(btn)
        }
        addView(buttons)
    }

    private fun addDoneButton() {
        val btn = android.widget.Button(context).apply {
            text = "Done"
            setOnClickListener {
                onComplete(config)
            }
        }
        addView(btn)
    }
}
