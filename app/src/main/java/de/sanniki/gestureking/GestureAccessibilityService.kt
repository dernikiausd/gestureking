package de.sanniki.gestureking

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import java.lang.ref.WeakReference

class GestureAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = WeakReference(this)
        GesturePrefs.setLastEvent(this, "Bedienungshilfe verbunden.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // GestureKing braucht aktuell keine Event-Auswertung.
    }

    override fun onInterrupt() {
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    companion object {
        private var instance: WeakReference<GestureAccessibilityService>? = null

        fun isRunning(): Boolean {
            return instance?.get() != null
        }

        fun perform(action: String): Boolean {
            val service = instance?.get() ?: return false

            return when (action) {
                GesturePrefs.ACTION_NONE -> true
                GesturePrefs.ACTION_BACK -> service.performGlobalAction(GLOBAL_ACTION_BACK)
                GesturePrefs.ACTION_HOME -> service.performGlobalAction(GLOBAL_ACTION_HOME)
                GesturePrefs.ACTION_RECENTS -> service.performGlobalAction(GLOBAL_ACTION_RECENTS)
                GesturePrefs.ACTION_NOTIFICATIONS -> service.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
                GesturePrefs.ACTION_QUICK_SETTINGS -> service.performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
                else -> false
            }
        }
    }
}
