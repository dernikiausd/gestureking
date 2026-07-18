package de.sanniki.gestureking

import android.content.Context

object GesturePrefs {
    private const val PREFS = "gestureking_settings"

    const val ACTION_NONE = "none"
    const val ACTION_BACK = "back"
    const val ACTION_HOME = "home"
    const val ACTION_RECENTS = "recents"
    const val ACTION_NOTIFICATIONS = "notifications"
    const val ACTION_QUICK_SETTINGS = "quick_settings"
    const val ACTION_SCREEN_OFF = "screen_off"
    const val ACTION_ROTATION_TOGGLE = "rotation_toggle"
    const val ACTION_BROWSER_FORWARD = "browser_forward"
    const val ACTION_OPEN_APP = "open_app"
    const val ACTION_OPEN_APP_1 = "open_app_1"
    const val ACTION_OPEN_APP_2 = "open_app_2"
    const val ACTION_OPEN_APP_3 = "open_app_3"
    const val ACTION_FLASHLIGHT_TOGGLE = "flashlight_toggle"

    const val GESTURE_RIGHT_SHORT = "right_short"
    const val GESTURE_RIGHT_LONG = "right_long"
    const val GESTURE_LEFT_SHORT = "left_short"
    const val GESTURE_LEFT_LONG = "left_long"
    const val GESTURE_BOTTOM_SHORT = "bottom_short"
    const val GESTURE_BOTTOM_LONG = "bottom_long"

    const val GESTURE_RIGHT_DOUBLE = "right_double"
    const val GESTURE_LEFT_DOUBLE = "left_double"
    const val GESTURE_BOTTOM_DOUBLE = "bottom_double"

    const val GESTURE_RIGHT_TOP = "right_top"
    const val GESTURE_RIGHT_MIDDLE = "right_middle"
    const val GESTURE_RIGHT_BOTTOM = "right_bottom"
    const val GESTURE_LEFT_TOP = "left_top"
    const val GESTURE_LEFT_MIDDLE = "left_middle"
    const val GESTURE_LEFT_BOTTOM = "left_bottom"
    const val GESTURE_BOTTOM_LEFT = "bottom_left"
    const val GESTURE_BOTTOM_CENTER = "bottom_center"
    const val GESTURE_BOTTOM_RIGHT = "bottom_right"

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean("enabled", enabled).apply()
    }

    fun isEnabled(context: Context): Boolean {
        return prefs(context).getBoolean("enabled", false)
    }

    fun setRightEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean("right_enabled", enabled).apply()
    }

    fun isRightEnabled(context: Context): Boolean {
        return prefs(context).getBoolean("right_enabled", true)
    }

    fun setLeftEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean("left_enabled", enabled).apply()
    }

    fun isLeftEnabled(context: Context): Boolean {
        return prefs(context).getBoolean("left_enabled", false)
    }

    fun setBottomEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean("bottom_enabled", enabled).apply()
    }

    fun isBottomEnabled(context: Context): Boolean {
        return prefs(context).getBoolean("bottom_enabled", false)
    }

    fun setSegmentModeEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean("segment_mode_enabled", enabled).apply()
    }

    fun isSegmentModeEnabled(context: Context): Boolean {
        return prefs(context).getBoolean("segment_mode_enabled", false)
    }

    fun setZonesVisible(context: Context, visible: Boolean) {
        prefs(context).edit().putBoolean("zones_visible", visible).apply()
    }

    fun areZonesVisible(context: Context): Boolean {
        return prefs(context).getBoolean("zones_visible", true)
    }

    fun setHapticEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean("haptic_enabled", enabled).apply()
    }

    fun isHapticEnabled(context: Context): Boolean {
        return prefs(context).getBoolean("haptic_enabled", true)
    }

    fun setToastEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean("toast_enabled", enabled).apply()
    }

    fun isToastEnabled(context: Context): Boolean {
        return prefs(context).getBoolean("toast_enabled", false)
    }

    fun setZoneSizeDp(context: Context, sizeDp: Int) {
        prefs(context).edit().putInt("zone_size_dp", sizeDp.coerceIn(12, 80)).apply()
    }

    fun zoneSizeDp(context: Context): Int {
        return prefs(context).getInt("zone_size_dp", 24)
    }

    fun setAction(context: Context, gesture: String, action: String) {
        prefs(context).edit().putString("action_$gesture", action).apply()
    }

    fun actionFor(context: Context, gesture: String): String {
        val default = when (gesture) {
            GESTURE_RIGHT_SHORT -> ACTION_BACK
            GESTURE_RIGHT_LONG -> ACTION_HOME
            GESTURE_LEFT_SHORT -> ACTION_BACK
            GESTURE_LEFT_LONG -> ACTION_NOTIFICATIONS
            GESTURE_BOTTOM_SHORT -> ACTION_HOME
            GESTURE_BOTTOM_LONG -> ACTION_RECENTS
            GESTURE_RIGHT_DOUBLE -> ACTION_NONE
            GESTURE_LEFT_DOUBLE -> ACTION_NONE
            GESTURE_BOTTOM_DOUBLE -> ACTION_NONE

            GESTURE_RIGHT_TOP -> ACTION_OPEN_APP_1
            GESTURE_RIGHT_MIDDLE -> ACTION_BACK
            GESTURE_RIGHT_BOTTOM -> ACTION_SCREEN_OFF
            GESTURE_LEFT_TOP -> ACTION_OPEN_APP_2
            GESTURE_LEFT_MIDDLE -> ACTION_NOTIFICATIONS
            GESTURE_LEFT_BOTTOM -> ACTION_QUICK_SETTINGS
            GESTURE_BOTTOM_LEFT -> ACTION_OPEN_APP_3
            GESTURE_BOTTOM_CENTER -> ACTION_HOME
            GESTURE_BOTTOM_RIGHT -> ACTION_RECENTS

            else -> ACTION_NONE
        }

        val stored = prefs(context).getString("action_$gesture", default) ?: default

        return when (stored) {
            "media_play_pause",
            "media_next" -> ACTION_NONE
            ACTION_OPEN_APP -> ACTION_OPEN_APP_1
            else -> stored
        }
    }

    fun actionLabel(action: String): String {
        return when (action) {
            ACTION_NONE -> "Keine Aktion"
            ACTION_BACK -> "Zurück"
            ACTION_HOME -> "Home"
            ACTION_RECENTS -> "Letzte Apps"
            ACTION_NOTIFICATIONS -> "Benachrichtigungen"
            ACTION_QUICK_SETTINGS -> "Quick Settings"
            ACTION_SCREEN_OFF -> "Bildschirm aus"
            ACTION_ROTATION_TOGGLE -> "Rotation umschalten"
            ACTION_BROWSER_FORWARD -> "Browser vor"
            ACTION_OPEN_APP,
            ACTION_OPEN_APP_1,
            ACTION_OPEN_APP_2,
            ACTION_OPEN_APP_3 -> "App öffnen"
            ACTION_FLASHLIGHT_TOGGLE -> "Taschenlampe"
            else -> action
        }
    }

    fun resetUserSettingsKeepMode(context: Context) {
        val keepSegmentMode = isSegmentModeEnabled(context)

        prefs(context).edit()
            .clear()
            .putBoolean("enabled", false)
            .putBoolean("right_enabled", true)
            .putBoolean("left_enabled", false)
            .putBoolean("bottom_enabled", false)
            .putBoolean("segment_mode_enabled", keepSegmentMode)
            .putBoolean("zones_visible", true)
            .putBoolean("haptic_enabled", true)
            .putBoolean("toast_enabled", false)
            .putInt("zone_size_dp", 24)
            .putBoolean("flashlight_on", false)

            // Normalmodus: wirklich alles leeren
            .putString("action_$GESTURE_RIGHT_SHORT", ACTION_NONE)
            .putString("action_$GESTURE_RIGHT_LONG", ACTION_NONE)
            .putString("action_$GESTURE_RIGHT_DOUBLE", ACTION_NONE)
            .putString("action_$GESTURE_LEFT_SHORT", ACTION_NONE)
            .putString("action_$GESTURE_LEFT_LONG", ACTION_NONE)
            .putString("action_$GESTURE_LEFT_DOUBLE", ACTION_NONE)
            .putString("action_$GESTURE_BOTTOM_SHORT", ACTION_NONE)
            .putString("action_$GESTURE_BOTTOM_LONG", ACTION_NONE)
            .putString("action_$GESTURE_BOTTOM_DOUBLE", ACTION_NONE)

            // Segment-Modus: wirklich alles leeren
            .putString("action_$GESTURE_RIGHT_TOP", ACTION_NONE)
            .putString("action_$GESTURE_RIGHT_MIDDLE", ACTION_NONE)
            .putString("action_$GESTURE_RIGHT_BOTTOM", ACTION_NONE)
            .putString("action_$GESTURE_LEFT_TOP", ACTION_NONE)
            .putString("action_$GESTURE_LEFT_MIDDLE", ACTION_NONE)
            .putString("action_$GESTURE_LEFT_BOTTOM", ACTION_NONE)
            .putString("action_$GESTURE_BOTTOM_LEFT", ACTION_NONE)
            .putString("action_$GESTURE_BOTTOM_CENTER", ACTION_NONE)
            .putString("action_$GESTURE_BOTTOM_RIGHT", ACTION_NONE)

            .putString("last_event", "Einstellungen zurückgesetzt · alle Gesten leer · Modus beibehalten.")
            .putString("last_raw_gesture", "Noch keine Rohdaten.")
            .apply()
    }

    fun applyRecommendedPreset(context: Context) {
        prefs(context).edit()
            .putBoolean("right_enabled", true)
            .putBoolean("left_enabled", true)
            .putBoolean("bottom_enabled", true)
            .putBoolean("zones_visible", true)
            .putBoolean("haptic_enabled", true)
            .putBoolean("toast_enabled", false)
            .putInt("zone_size_dp", 24)
            .putString("action_$GESTURE_RIGHT_SHORT", ACTION_BACK)
            .putString("action_$GESTURE_RIGHT_LONG", ACTION_HOME)
            .putString("action_$GESTURE_LEFT_SHORT", ACTION_NOTIFICATIONS)
            .putString("action_$GESTURE_LEFT_LONG", ACTION_QUICK_SETTINGS)
            .putString("action_$GESTURE_BOTTOM_SHORT", ACTION_HOME)
            .putString("action_$GESTURE_BOTTOM_LONG", ACTION_RECENTS)
            .putString("action_$GESTURE_RIGHT_TOP", ACTION_OPEN_APP_1)
            .putString("action_$GESTURE_RIGHT_MIDDLE", ACTION_BACK)
            .putString("action_$GESTURE_RIGHT_BOTTOM", ACTION_SCREEN_OFF)
            .putString("action_$GESTURE_LEFT_TOP", ACTION_OPEN_APP_2)
            .putString("action_$GESTURE_LEFT_MIDDLE", ACTION_NOTIFICATIONS)
            .putString("action_$GESTURE_LEFT_BOTTOM", ACTION_QUICK_SETTINGS)
            .putString("action_$GESTURE_BOTTOM_LEFT", ACTION_OPEN_APP_3)
            .putString("action_$GESTURE_BOTTOM_CENTER", ACTION_HOME)
            .putString("action_$GESTURE_BOTTOM_RIGHT", ACTION_RECENTS)
            .apply()
    }

    fun isOpenAppAction(action: String): Boolean {
        return action == ACTION_OPEN_APP ||
            action == ACTION_OPEN_APP_1 ||
            action == ACTION_OPEN_APP_2 ||
            action == ACTION_OPEN_APP_3
    }

    fun appSlotForAction(action: String): Int {
        return when (action) {
            ACTION_OPEN_APP_2 -> 2
            ACTION_OPEN_APP_3 -> 3
            else -> 1
        }
    }

    fun setOpenAppTarget(context: Context, packageName: String, label: String) {
        setOpenAppTarget(context, 1, packageName, label)
    }

    fun setOpenAppTarget(context: Context, slot: Int, packageName: String, label: String) {
        val safeSlot = slot.coerceIn(1, 3)

        prefs(context).edit()
            .putString("open_app_${safeSlot}_package", packageName)
            .putString("open_app_${safeSlot}_label", label)
            .apply()
    }

    fun openAppTargetPackage(context: Context): String {
        return openAppTargetPackage(context, 1)
    }

    fun openAppTargetPackage(context: Context, slot: Int): String {
        val safeSlot = slot.coerceIn(1, 3)
        val value = prefs(context).getString("open_app_${safeSlot}_package", "") ?: ""

        if (safeSlot == 1 && value.isBlank()) {
            return prefs(context).getString("open_app_package", "") ?: ""
        }

        return value
    }

    fun openAppTargetLabel(context: Context): String {
        return openAppTargetLabel(context, 1)
    }

    fun openAppTargetLabel(context: Context, slot: Int): String {
        val safeSlot = slot.coerceIn(1, 3)
        val value = prefs(context).getString("open_app_${safeSlot}_label", "") ?: ""

        if (value.isNotBlank()) return value

        if (safeSlot == 1) {
            val legacy = prefs(context).getString("open_app_label", "") ?: ""
            if (legacy.isNotBlank()) return legacy
        }

        return "Keine App gewählt"
    }

    private fun openAppGestureKey(gesture: String, suffix: String): String {
        return "open_app_target_${gesture}_$suffix"
    }

    fun setOpenAppTarget(context: Context, gesture: String, packageName: String, label: String) {
        prefs(context).edit()
            .putString(openAppGestureKey(gesture, "package"), packageName)
            .putString(openAppGestureKey(gesture, "label"), label)
            .apply()
    }

    fun openAppTargetPackage(context: Context, gesture: String): String {
        val direct = prefs(context).getString(openAppGestureKey(gesture, "package"), "") ?: ""
        if (direct.isNotBlank()) return direct

        // Fallback für alte globale App-1/2/3-Slots
        val action = actionFor(context, gesture)
        return openAppTargetPackage(context, appSlotForAction(action))
    }

    fun openAppTargetLabel(context: Context, gesture: String): String {
        val direct = prefs(context).getString(openAppGestureKey(gesture, "label"), "") ?: ""
        if (direct.isNotBlank()) return direct

        // Fallback für alte globale App-1/2/3-Slots
        val action = actionFor(context, gesture)
        return openAppTargetLabel(context, appSlotForAction(action))
    }


    fun setLastEvent(context: Context, text: String) {
        prefs(context).edit().putString("last_event", text).apply()
    }

    fun lastEvent(context: Context): String {
        return prefs(context).getString("last_event", "Noch kein Ereignis.") ?: "Noch kein Ereignis."
    }

    fun setLastRawGesture(context: Context, text: String) {
        prefs(context).edit().putString("last_raw_gesture", text).apply()
    }

    fun lastRawGesture(context: Context): String {
        return prefs(context).getString("last_raw_gesture", "Noch keine Rohdaten.") ?: "Noch keine Rohdaten."
    }

    fun setFlashlightOn(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean("flashlight_on", enabled).apply()
    }

    fun isFlashlightOn(context: Context): Boolean {
        return prefs(context).getBoolean("flashlight_on", false)
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
