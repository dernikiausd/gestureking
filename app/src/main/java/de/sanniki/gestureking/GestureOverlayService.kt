package de.sanniki.gestureking

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

class GestureOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private val zoneViews = mutableListOf<View>()
    private var feedbackBubble: View? = null
    private val feedbackHandler = Handler(Looper.getMainLooper())

    private var startX = 0f
    private var startY = 0f
    private var startTime = 0L

    private enum class Zone {
        LEFT,
        RIGHT,
        BOTTOM
    }

    private var lastTapTime = 0L
    private var lastTapX = 0f
    private var lastTapY = 0f
    private var lastTapZone: Zone? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WindowManager::class.java)
        reloadOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!GesturePrefs.isEnabled(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        reloadOverlay()
        return START_STICKY
    }

    override fun onDestroy() {
        removeOverlay()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun reloadOverlay() {
        removeOverlay()

        if (!Settings.canDrawOverlays(this)) {
            GesturePrefs.setLastEvent(this, "Overlay-Berechtigung fehlt.")
            return
        }

        if (!GesturePrefs.isEnabled(this)) {
            GesturePrefs.setLastEvent(this, "${time()} · GestureKing deaktiviert.")
            return
        }

        if (GesturePrefs.isSegmentModeEnabled(this)) {
            if (GesturePrefs.isRightEnabled(this)) addSegmentedZone(Zone.RIGHT)
            if (GesturePrefs.isLeftEnabled(this)) addSegmentedZone(Zone.LEFT)
            if (GesturePrefs.isBottomEnabled(this)) addSegmentedZone(Zone.BOTTOM)
        } else {
            if (GesturePrefs.isRightEnabled(this)) addZone(Zone.RIGHT)
            if (GesturePrefs.isLeftEnabled(this)) addZone(Zone.LEFT)
            if (GesturePrefs.isBottomEnabled(this)) addZone(Zone.BOTTOM)
        }

        GesturePrefs.setLastEvent(
            this,
            "${time()} · Overlay aktualisiert · Zonen: ${activeZoneText()}"
        )
    }

    private fun addSegmentedZone(zone: Zone) {
        for (segment in 0..2) {
            addZonePart(zone, segment)
        }
    }

    private fun addZonePart(zone: Zone, segment: Int) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val sizePx = dp(GesturePrefs.zoneSizeDp(this))
        val screenWidth = resources.displayMetrics.widthPixels.coerceAtLeast(1)
        val screenHeight = resources.displayMetrics.heightPixels.coerceAtLeast(1)

        val thirdHeight = (screenHeight / 3).coerceAtLeast(1)
        val thirdWidth = (screenWidth / 3).coerceAtLeast(1)

        val width = when (zone) {
            Zone.LEFT, Zone.RIGHT -> sizePx
            Zone.BOTTOM -> if (segment == 2) {
                screenWidth - thirdWidth * 2
            } else {
                thirdWidth
            }
        }

        val height = when (zone) {
            Zone.LEFT, Zone.RIGHT -> if (segment == 2) {
                screenHeight - thirdHeight * 2
            } else {
                thirdHeight
            }
            Zone.BOTTOM -> sizePx
        }

        val gravity = when (zone) {
            Zone.LEFT -> Gravity.START or Gravity.TOP
            Zone.RIGHT -> Gravity.END or Gravity.TOP
            Zone.BOTTOM -> Gravity.BOTTOM or Gravity.START
        }

        val params = WindowManager.LayoutParams(
            width,
            height,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            this.gravity = gravity

            when (zone) {
                Zone.LEFT, Zone.RIGHT -> {
                    y = thirdHeight * segment
                }
                Zone.BOTTOM -> {
                    x = thirdWidth * segment
                }
            }
        }

        val visible = GesturePrefs.areZonesVisible(this)

        val fillColor = segmentFillColor(zone, segment, visible)
        val strokeColor = segmentStrokeColor(zone, segment, visible)

        val view = View(this).apply {
            background = GradientDrawable().apply {
                setColor(fillColor)
                cornerRadius = 0f
                setStroke(if (visible) dp(2) else 0, strokeColor)
            }

            setOnTouchListener { _, event ->
                handleTouch(zone, event)
                true
            }
        }

        try {
            windowManager?.addView(view, params)
            zoneViews += view
        } catch (t: Throwable) {
            GesturePrefs.setLastEvent(
                this,
                "Overlay-Segment ${zone.name}/${segment + 1} fehlgeschlagen: ${t.message}"
            )
        }
    }

    private fun segmentFillColor(zone: Zone, segment: Int, visible: Boolean): Int {
        val alpha = if (visible) 96 else 1

        return when (zone) {
            Zone.RIGHT -> when (segment.coerceIn(0, 2)) {
                0 -> Color.argb(alpha, 56, 189, 248)
                1 -> Color.argb(alpha, 14, 165, 233)
                else -> Color.argb(alpha, 2, 132, 199)
            }

            Zone.LEFT -> when (segment.coerceIn(0, 2)) {
                0 -> Color.argb(alpha, 196, 181, 253)
                1 -> Color.argb(alpha, 167, 139, 250)
                else -> Color.argb(alpha, 139, 92, 246)
            }

            Zone.BOTTOM -> when (segment.coerceIn(0, 2)) {
                0 -> Color.argb(alpha, 134, 239, 172)
                1 -> Color.argb(alpha, 74, 222, 128)
                else -> Color.argb(alpha, 34, 197, 94)
            }
        }
    }

    private fun segmentStrokeColor(zone: Zone, segment: Int, visible: Boolean): Int {
        val alpha = if (visible) 235 else 0

        return when (zone) {
            Zone.RIGHT -> when (segment.coerceIn(0, 2)) {
                0 -> Color.argb(alpha, 125, 211, 252)
                1 -> Color.argb(alpha, 56, 189, 248)
                else -> Color.argb(alpha, 3, 105, 161)
            }

            Zone.LEFT -> when (segment.coerceIn(0, 2)) {
                0 -> Color.argb(alpha, 221, 214, 254)
                1 -> Color.argb(alpha, 196, 181, 253)
                else -> Color.argb(alpha, 124, 58, 237)
            }

            Zone.BOTTOM -> when (segment.coerceIn(0, 2)) {
                0 -> Color.argb(alpha, 187, 247, 208)
                1 -> Color.argb(alpha, 74, 222, 128)
                else -> Color.argb(alpha, 22, 163, 74)
            }
        }
    }

    private fun addZone(zone: Zone) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val sizePx = dp(GesturePrefs.zoneSizeDp(this))

        val width = when (zone) {
            Zone.LEFT, Zone.RIGHT -> sizePx
            Zone.BOTTOM -> WindowManager.LayoutParams.MATCH_PARENT
        }

        val height = when (zone) {
            Zone.LEFT, Zone.RIGHT -> WindowManager.LayoutParams.MATCH_PARENT
            Zone.BOTTOM -> sizePx
        }

        val gravity = when (zone) {
            Zone.LEFT -> Gravity.START or Gravity.TOP
            Zone.RIGHT -> Gravity.END or Gravity.TOP
            Zone.BOTTOM -> Gravity.BOTTOM or Gravity.START
        }

        val params = WindowManager.LayoutParams(
            width,
            height,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            this.gravity = gravity
        }

        val visible = GesturePrefs.areZonesVisible(this)

        val fillColor = when (zone) {
            Zone.LEFT -> Color.argb(if (visible) 86 else 1, 167, 139, 250)
            Zone.RIGHT -> Color.argb(if (visible) 86 else 1, 125, 211, 252)
            Zone.BOTTOM -> Color.argb(if (visible) 78 else 1, 74, 222, 128)
        }

        val strokeColor = when (zone) {
            Zone.LEFT -> Color.argb(if (visible) 220 else 0, 196, 181, 253)
            Zone.RIGHT -> Color.argb(if (visible) 230 else 0, 56, 189, 248)
            Zone.BOTTOM -> Color.argb(if (visible) 220 else 0, 34, 197, 94)
        }

        val view = View(this).apply {
            background = GradientDrawable().apply {
                setColor(fillColor)
                cornerRadius = 0f
                setStroke(if (visible) dp(2) else 0, strokeColor)
            }

            setOnTouchListener { _, event ->
                handleTouch(zone, event)
                true
            }
        }

        try {
            windowManager?.addView(view, params)
            zoneViews += view
        } catch (t: Throwable) {
            GesturePrefs.setLastEvent(this, "Overlay-Zone ${zone.name} fehlgeschlagen: ${t.message}")
        }
    }

    private fun removeOverlay() {
        removeFeedbackBubble()

        zoneViews.forEach { view ->
            try {
                windowManager?.removeView(view)
            } catch (_: Throwable) {
            }
        }

        zoneViews.clear()
    }

    private fun handleTouch(zone: Zone, event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.rawX
                startY = event.rawY
                startTime = System.currentTimeMillis()
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val dx = event.rawX - startX
                val dy = event.rawY - startY
                val duration = System.currentTimeMillis() - startTime

                GesturePrefs.setLastRawGesture(
                    this,
                    "Zone=${zone.name.lowercase(Locale.getDefault())} · dx=${dx.roundToInt()} · dy=${dy.roundToInt()} · Dauer=${duration}ms"
                )

                if (isTap(dx, dy, duration)) {
                    handleTap(zone, event.rawX, event.rawY, duration)
                    return
                }

                when (zone) {
                    Zone.RIGHT -> handleRightZone(dx, dy, duration)
                    Zone.LEFT -> handleLeftZone(dx, dy, duration)
                    Zone.BOTTOM -> handleBottomZone(dx, dy, duration)
                }
            }
        }
    }

    private fun isTap(dx: Float, dy: Float, duration: Long): Boolean {
        val maxMove = dp(18)
        return duration <= 240L &&
            abs(dx) <= maxMove &&
            abs(dy) <= maxMove
    }

    private fun handleTap(zone: Zone, x: Float, y: Float, duration: Long) {
        val now = System.currentTimeMillis()
        val maxDelay = 330L
        val maxDistance = dp(30)

        val sameZone = lastTapZone == zone
        val closeEnough = abs(x - lastTapX) <= maxDistance &&
            abs(y - lastTapY) <= maxDistance
        val fastEnough = now - lastTapTime <= maxDelay

        if (sameZone && closeEnough && fastEnough) {
            lastTapTime = 0L
            lastTapZone = null

            val gesture = when (zone) {
                Zone.RIGHT -> GesturePrefs.GESTURE_RIGHT_DOUBLE
                Zone.LEFT -> GesturePrefs.GESTURE_LEFT_DOUBLE
                Zone.BOTTOM -> GesturePrefs.GESTURE_BOTTOM_DOUBLE
            }

            val label = when (zone) {
                Zone.RIGHT -> "rechter Rand · Doppeltipp"
                Zone.LEFT -> "linker Rand · Doppeltipp"
                Zone.BOTTOM -> "unterer Rand · Doppeltipp"
            }

            GesturePrefs.setLastRawGesture(
                this,
                "Zone=${zone.name.lowercase(Locale.getDefault())} · Typ=Doppeltipp · Dauer=${duration}ms"
            )

            runGesture(gesture, label, false)
            return
        }

        lastTapTime = now
        lastTapX = x
        lastTapY = y
        lastTapZone = zone

        GesturePrefs.setLastRawGesture(
            this,
            "Zone=${zone.name.lowercase(Locale.getDefault())} · Typ=Tap wartet · Dauer=${duration}ms"
        )
    }

    private fun handleRightZone(dx: Float, dy: Float, duration: Long) {
        val horizontal = abs(dx) > abs(dy)
        val swipeLeft = horizontal && dx < -dp(70)
        if (!swipeLeft) {
            feedback(false, "Geste nicht erkannt")
            return
        }

        if (GesturePrefs.isSegmentModeEnabled(this)) {
            val third = verticalThird(startY)
            val gesture = when (third) {
                0 -> GesturePrefs.GESTURE_RIGHT_TOP
                1 -> GesturePrefs.GESTURE_RIGHT_MIDDLE
                else -> GesturePrefs.GESTURE_RIGHT_BOTTOM
            }
            val label = when (third) {
                0 -> "rechter Rand oben ←"
                1 -> "rechter Rand mitte ←"
                else -> "rechter Rand unten ←"
            }

            GesturePrefs.setLastRawGesture(
                this,
                "Zone=rechts · Segment=${thirdName(third)} · dx=${dx.roundToInt()} · dy=${dy.roundToInt()} · Dauer=${duration}ms"
            )

            runGesture(gesture, label, false)
            return
        }

        val longGesture = duration >= 550L

        val gesture = if (longGesture) {
            GesturePrefs.GESTURE_RIGHT_LONG
        } else {
            GesturePrefs.GESTURE_RIGHT_SHORT
        }

        val gestureLabel = if (longGesture) {
            "rechter langer Swipe ←"
        } else {
            "rechter Swipe ←"
        }

        runGesture(gesture, gestureLabel, longGesture)
    }

    private fun handleLeftZone(dx: Float, dy: Float, duration: Long) {
        val horizontal = abs(dx) > abs(dy)
        val swipeRight = horizontal && dx > dp(70)
        if (!swipeRight) {
            feedback(false, "Geste nicht erkannt")
            return
        }

        if (GesturePrefs.isSegmentModeEnabled(this)) {
            val third = verticalThird(startY)
            val gesture = when (third) {
                0 -> GesturePrefs.GESTURE_LEFT_TOP
                1 -> GesturePrefs.GESTURE_LEFT_MIDDLE
                else -> GesturePrefs.GESTURE_LEFT_BOTTOM
            }
            val label = when (third) {
                0 -> "linker Rand oben →"
                1 -> "linker Rand mitte →"
                else -> "linker Rand unten →"
            }

            GesturePrefs.setLastRawGesture(
                this,
                "Zone=links · Segment=${thirdName(third)} · dx=${dx.roundToInt()} · dy=${dy.roundToInt()} · Dauer=${duration}ms"
            )

            runGesture(gesture, label, false)
            return
        }

        val longGesture = duration >= 550L

        val gesture = if (longGesture) {
            GesturePrefs.GESTURE_LEFT_LONG
        } else {
            GesturePrefs.GESTURE_LEFT_SHORT
        }

        val gestureLabel = if (longGesture) {
            "linker langer Swipe →"
        } else {
            "linker Swipe →"
        }

        runGesture(gesture, gestureLabel, longGesture)
    }

    private fun handleBottomZone(dx: Float, dy: Float, duration: Long) {
        val vertical = abs(dy) > abs(dx)
        val swipeUp = vertical && dy < -dp(70)
        if (!swipeUp) {
            feedback(false, "Geste nicht erkannt")
            return
        }

        if (GesturePrefs.isSegmentModeEnabled(this)) {
            val third = horizontalThird(startX)
            val gesture = when (third) {
                0 -> GesturePrefs.GESTURE_BOTTOM_LEFT
                1 -> GesturePrefs.GESTURE_BOTTOM_CENTER
                else -> GesturePrefs.GESTURE_BOTTOM_RIGHT
            }
            val label = when (third) {
                0 -> "unterer Rand links ↑"
                1 -> "unterer Rand mitte ↑"
                else -> "unterer Rand rechts ↑"
            }

            GesturePrefs.setLastRawGesture(
                this,
                "Zone=unten · Segment=${thirdName(third)} · dx=${dx.roundToInt()} · dy=${dy.roundToInt()} · Dauer=${duration}ms"
            )

            runGesture(gesture, label, false)
            return
        }

        val longGesture = duration >= 550L

        val gesture = if (longGesture) {
            GesturePrefs.GESTURE_BOTTOM_LONG
        } else {
            GesturePrefs.GESTURE_BOTTOM_SHORT
        }

        val gestureLabel = if (longGesture) {
            "unterer langer Swipe ↑"
        } else {
            "unterer Swipe ↑"
        }

        runGesture(gesture, gestureLabel, longGesture)
    }

    private fun verticalThird(y: Float): Int {
        val height = resources.displayMetrics.heightPixels.coerceAtLeast(1)
        return ((y / height.toFloat()) * 3f).toInt().coerceIn(0, 2)
    }

    private fun horizontalThird(x: Float): Int {
        val width = resources.displayMetrics.widthPixels.coerceAtLeast(1)
        return ((x / width.toFloat()) * 3f).toInt().coerceIn(0, 2)
    }

    private fun thirdName(third: Int): String {
        return when (third.coerceIn(0, 2)) {
            0 -> "oben/links"
            1 -> "mitte"
            else -> "unten/rechts"
        }
    }

    private fun runGesture(gesture: String, gestureLabel: String, longGesture: Boolean) {
        val action = GesturePrefs.actionFor(this, gesture)
        val actionLabel = if (GesturePrefs.isOpenAppAction(action)) {
            "App öffnen: ${GesturePrefs.openAppTargetLabel(this, gesture)}"
        } else {
            GesturePrefs.actionLabel(action)
        }

        if (action == GesturePrefs.ACTION_NONE) {
            GesturePrefs.setLastEvent(
                this,
                "${time()} · $gestureLabel · Keine Aktion"
            )
            return
        }

        val ok = when (action) {
            GesturePrefs.ACTION_SCREEN_OFF,
            GesturePrefs.ACTION_ROTATION_TOGGLE,
            GesturePrefs.ACTION_BROWSER_FORWARD -> GestureShizuku.perform(this, action)
            GesturePrefs.ACTION_FLASHLIGHT_TOGGLE -> toggleFlashlight()
            GesturePrefs.ACTION_OPEN_APP,
            GesturePrefs.ACTION_OPEN_APP_1,
            GesturePrefs.ACTION_OPEN_APP_2,
            GesturePrefs.ACTION_OPEN_APP_3 -> openSelectedApp(gesture)
            else -> GestureAccessibilityService.perform(action)
        }

        GesturePrefs.setLastEvent(
            this,
            "${time()} · $gestureLabel · $actionLabel · ${okText(ok)}"
        )

        vibrateForGesture(ok, gestureLabel.contains("langer"))
        toastForGesture(ok, actionLabel)

        feedback(ok, actionLabel, longGesture)
    }

    private fun feedback(ok: Boolean, label: String, longGesture: Boolean = false) {
        if (GesturePrefs.isHapticEnabled(this)) {
            vibrate(
                when {
                    !ok -> 55L
                    longGesture -> 42L
                    else -> 22L
                }
            )
        }

        if (GesturePrefs.isToastEnabled(this)) {
            Toast.makeText(
                this,
                if (ok) label else "Nicht erkannt / fehlgeschlagen",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun vibrate(ms: Long) {
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(ms)
            }
        } catch (_: Throwable) {
        }
    }

    private fun toastForGesture(ok: Boolean, label: String) {
        if (!GesturePrefs.isToastEnabled(this)) return

        val text = if (ok) label else "Nicht erkannt / fehlgeschlagen"
        showFeedbackBubble(text)
    }

    private fun showFeedbackBubble(text: String) {
        if (!Settings.canDrawOverlays(this)) return

        removeFeedbackBubble()

        val bubble = TextView(this).apply {
            this.text = text
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(dp(18), dp(10), dp(18), dp(10))
            background = GradientDrawable().apply {
                setColor(Color.argb(235, 19, 26, 42))
                cornerRadius = dp(18).toFloat()
                setStroke(dp(1), Color.argb(220, 125, 211, 252))
            }
        }

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(84)
        }

        try {
            windowManager?.addView(bubble, params)
            feedbackBubble = bubble

            feedbackHandler.postDelayed({
                removeFeedbackBubble()
            }, 850L)
        } catch (_: Throwable) {
            feedbackBubble = null

            try {
                Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
            } catch (_: Throwable) {
            }
        }
    }

    private fun removeFeedbackBubble() {
        val bubble = feedbackBubble ?: return

        try {
            windowManager?.removeView(bubble)
        } catch (_: Throwable) {
        }

        feedbackBubble = null
    }

    private fun vibrateForGesture(ok: Boolean, longGesture: Boolean) {
        if (!GesturePrefs.isHapticEnabled(this)) return

        val ms = when {
            !ok -> 55L
            longGesture -> 42L
            else -> 22L
        }

        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(ms)
            }
        } catch (_: Throwable) {
        }
    }

    private fun toggleFlashlight(): Boolean {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            GesturePrefs.setLastEvent(this, "Kamera-Berechtigung fehlt · Taschenlampe nicht möglich.")
            return false
        }

        return try {
            val cameraManager = getSystemService(CameraManager::class.java)

            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val isBack = chars.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
                hasFlash && isBack
            } ?: cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }

            if (cameraId == null) {
                GesturePrefs.setLastEvent(this, "Keine Taschenlampe gefunden.")
                return false
            }

            val next = !GesturePrefs.isFlashlightOn(this)
            cameraManager.setTorchMode(cameraId, next)
            GesturePrefs.setFlashlightOn(this, next)

            GesturePrefs.setLastEvent(
                this,
                if (next) "Taschenlampe eingeschaltet." else "Taschenlampe ausgeschaltet."
            )

            true
        } catch (t: Throwable) {
            GesturePrefs.setLastEvent(this, "Taschenlampe fehlgeschlagen: ${t.javaClass.simpleName}")
            false
        }
    }

    private fun openSelectedApp(gesture: String): Boolean {
        val packageName = GesturePrefs.openAppTargetPackage(this, gesture)
        val label = GesturePrefs.openAppTargetLabel(this, gesture)

        if (packageName.isBlank()) {
            GesturePrefs.setLastEvent(this, "Keine Ziel-App für diese Geste gewählt.")
            return false
        }

        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: run {
            GesturePrefs.setLastEvent(this, "App nicht startbar: $label / $packageName")
            return false
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return try {
            startActivity(intent)
            true
        } catch (_: Throwable) {
            false
        }
    }

    private fun activeZoneText(): String {
        val zones = mutableListOf<String>()

        if (GesturePrefs.isLeftEnabled(this)) zones += "links"
        if (GesturePrefs.isRightEnabled(this)) zones += "rechts"
        if (GesturePrefs.isBottomEnabled(this)) zones += "unten"

        val text = zones.ifEmpty { listOf("keine") }.joinToString(", ")
        return if (GesturePrefs.isSegmentModeEnabled(this)) {
            "$text · Segment-Modus"
        } else {
            text
        }
    }

    private fun okText(ok: Boolean): String {
        return if (ok) "OK" else "fehlgeschlagen"
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun time(): String {
        return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    }
}
