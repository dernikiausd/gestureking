package de.sanniki.gestureking

import android.content.Context
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

object GestureShizuku {

    fun isReachable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (_: Throwable) {
            false
        }
    }

    fun hasPermission(): Boolean {
        return try {
            Shizuku.pingBinder() &&
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) {
            false
        }
    }

    fun perform(context: Context, action: String): Boolean {
        return when (action) {
            GesturePrefs.ACTION_SCREEN_OFF -> screenOff(context)
            GesturePrefs.ACTION_ROTATION_TOGGLE -> toggleRotation(context)
            GesturePrefs.ACTION_BROWSER_FORWARD -> browserForward(context)
            GesturePrefs.ACTION_FLASHLIGHT_TOGGLE -> toggleFlashlight(context)
            else -> false
        }
    }

    private fun toggleFlashlight(context: Context): Boolean {
        if (!hasPermission()) {
            GesturePrefs.setLastEvent(context, "Shizuku fehlt · Taschenlampe nicht möglich.")
            return false
        }

        val result = runCommand("cmd statusbar click-tile com.android.systemui/.qs.tiles.FlashlightTile")

        if (result.exitCode == 0) {
            GesturePrefs.setLastEvent(context, "Taschenlampe umgeschaltet.")
        } else {
            GesturePrefs.setLastEvent(
                context,
                "Taschenlampe fehlgeschlagen: ${result.stderr.trim().ifBlank { "Tile nicht erreichbar" }}"
            )
        }

        return result.exitCode == 0
    }

    private fun browserForward(context: Context): Boolean {
        if (!hasPermission()) {
            GesturePrefs.setLastEvent(context, "Shizuku fehlt · Browser vor nicht möglich.")
            return false
        }

        return runCommand("input keyevent 125").exitCode == 0
    }

    private fun screenOff(context: Context): Boolean {
        if (!hasPermission()) {
            GesturePrefs.setLastEvent(context, "Shizuku fehlt · Bildschirm aus nicht möglich.")
            return false
        }

        return runCommand("input keyevent 223").exitCode == 0
    }

    private fun toggleRotation(context: Context): Boolean {
        if (!hasPermission()) {
            GesturePrefs.setLastEvent(context, "Shizuku fehlt · Rotation umschalten nicht möglich.")
            return false
        }

        val current = readRotationValue()
        if (current != "0" && current != "1") {
            GesturePrefs.setLastEvent(
                context,
                "Auto-Rotate Status unklar: $current"
            )
            return false
        }

        val next = if (current == "1") "0" else "1"

        val firstOk = writeRotationValue(next)
        Thread.sleep(90L)

        val firstVerify = readRotationValue()
        if (firstOk && firstVerify == next) {
            GesturePrefs.setLastEvent(
                context,
                "Auto-Rotate umgeschaltet: $current → $next · OK"
            )
            return true
        }

        val secondOk = writeRotationValue(next)
        Thread.sleep(140L)

        val secondVerify = readRotationValue()
        val ok = secondOk && secondVerify == next

        GesturePrefs.setLastEvent(
            context,
            if (ok) {
                "Auto-Rotate Retry: $current → $next · OK"
            } else {
                "Auto-Rotate fehlgeschlagen: $current → $next · gelesen: $secondVerify"
            }
        )

        return ok
    }

    private fun readRotationValue(): String {
        return runCommand("settings get system accelerometer_rotation")
            .stdout
            .trim()
    }

    private fun writeRotationValue(value: String): Boolean {
        return runCommand("settings put system accelerometer_rotation $value").exitCode == 0
    }

    private fun runCommand(command: String): ShellResult {
        val stdout = StringBuilder()
        val stderr = StringBuilder()

        return try {
            val process = newProcess(command)

            val outThread = Thread {
                process.inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { stdout.appendLine(it) }
                }
            }

            val errThread = Thread {
                process.errorStream.bufferedReader().useLines { lines ->
                    lines.forEach { stderr.appendLine(it) }
                }
            }

            outThread.start()
            errThread.start()

            val exit = process.waitFor()

            outThread.join(500)
            errThread.join(500)

            ShellResult(exit, stdout.toString(), stderr.toString())
        } catch (t: Throwable) {
            ShellResult(-99, "", "${t.javaClass.simpleName}: ${t.message.orEmpty()}")
        }
    }

    private fun newProcess(command: String): Process {
        val method = Shizuku::class.java.getDeclaredMethod(
            "newProcess",
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java
        )
        method.isAccessible = true

        return method.invoke(
            null,
            arrayOf("sh", "-c", command),
            null,
            null
        ) as Process
    }

    data class ShellResult(
        val exitCode: Int,
        val stdout: String,
        val stderr: String
    )
}
