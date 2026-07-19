package de.sanniki.gestureking

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {

    private var resumeTick by mutableIntStateOf(0)

    private val shizukuPermissionRequestCode = 9404

    private val shizukuPermissionListener =
        rikka.shizuku.Shizuku.OnRequestPermissionResultListener {
                requestCode,
                grantResult ->
            if (requestCode == shizukuPermissionRequestCode) {
                runOnUiThread {
                    GesturePrefs.setLastEvent(
                        this,
                        if (
                            grantResult ==
                                android.content.pm.PackageManager.PERMISSION_GRANTED
                        ) {
                            "Shizuku-Berechtigung erteilt."
                        } else {
                            "Shizuku-Berechtigung abgelehnt."
                        }
                    )

                    resumeTick++
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            rikka.shizuku.Shizuku
                .addRequestPermissionResultListener(
                    shizukuPermissionListener
                )
        } catch (_: Throwable) {
        }

        setContent {
            GestureKingComposeApp(
                refreshToken = resumeTick,
                restartOverlay = {
                    restartOverlay()
                },
                requestShizuku = {
                    try {
                        rikka.shizuku.Shizuku
                            .requestPermission(
                                shizukuPermissionRequestCode
                            )
                    } catch (_: Throwable) {
                        GesturePrefs.setLastEvent(
                            this,
                            "Shizuku-Freigabe konnte nicht geöffnet werden."
                        )
                    }
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        resumeTick++
    }

    override fun onDestroy() {
        try {
            rikka.shizuku.Shizuku
                .removeRequestPermissionResultListener(
                    shizukuPermissionListener
                )
        } catch (_: Throwable) {
        }

        super.onDestroy()
    }

    private fun restartOverlay() {
        stopService(
            Intent(
                this,
                GestureOverlayService::class.java
            )
        )

        if (GesturePrefs.isEnabled(this)) {
            startService(
                Intent(
                    this,
                    GestureOverlayService::class.java
                )
            )
        }
    }
}

private val Bg = Color(0xFF0A111B)
private val CardBg = Color(0xFF151E29)
private val TextMain = Color(0xFFEAF2FF)
private val TextMuted = Color(0xFF9AA9BA)
private val Stroke = Color(0xFF2B3745)
private val Success = Color(0xFF57C785)
private val Warning = Color(0xFFF2C94C)

private data class Accent(
    val title: String,
    val key: String,
    val color: Color,
    val header: Color
)

private val accents = listOf(
    Accent("Blau", "blue", Color(0xFF4E8FE8), Color(0xFF17477F)),
    Accent("Rot", "red", Color(0xFFE45C61), Color(0xFF71313A)),
    Accent("Grün", "green", Color(0xFF45A86B), Color(0xFF245C38)),
    Accent("Türkis", "turquoise", Color(0xFF25A9AA), Color(0xFF155B5C)),
    Accent("Gelb", "yellow", Color(0xFFD5A800), Color(0xFF66540B))
)

@Composable
private fun GestureKingComposeApp(
    refreshToken: Int,
    restartOverlay: () -> Unit,
    requestShizuku: () -> Unit
) {
    val context = LocalContext.current
    val uiPrefs = remember {
        context.getSharedPreferences(
            "gestureking_ui",
            android.content.Context.MODE_PRIVATE
        )
    }

    var settingsVisible by rememberSaveable {
        mutableStateOf(false)
    }

    var preferencesRevision by remember {
        mutableIntStateOf(0)
    }

    var accentKey by rememberSaveable {
        mutableStateOf(
            uiPrefs.getString("accent_color", "blue") ?: "blue"
        )
    }

    val accent = accents.firstOrNull { it.key == accentKey } ?: accents.first()

    val accentColor by animateColorAsState(
        targetValue = accent.color,
        label = "accent"
    )

    val headerColor by animateColorAsState(
        targetValue = accent.header,
        label = "header"
    )

    BackHandler(settingsVisible) {
        settingsVisible = false
    }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Bg
        ) {
            if (settingsVisible) {
                SettingsScreen(
                    refreshToken = refreshToken,
                    accent = accentColor,
                    headerColor = headerColor,
                    accentKey = accentKey,
                    onAccentSelected = {
                        accentKey = it.key
                        uiPrefs.edit()
                            .putString("accent_color", it.key)
                            .apply()

                        GesturePrefs.setLastEvent(
                            context,
                            "Akzentfarbe gesetzt: ${it.title}"
                        )
                    },
                    onBack = { settingsVisible = false },
                    restartOverlay = restartOverlay,
                    requestShizuku = requestShizuku,
                    onPreferencesReset = {
                        preferencesRevision++
                    }
                )
            } else {
                HomeScreen(
                    refreshToken = refreshToken,
                    preferencesRevision = preferencesRevision,
                    accent = accentColor,
                    headerColor = headerColor,
                    onOpenSettings = { settingsVisible = true },
                    restartOverlay = restartOverlay
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(
    refreshToken: Int,
    preferencesRevision: Int,
    accent: Color,
    headerColor: Color,
    onOpenSettings: () -> Unit,
    restartOverlay: () -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    var enabled by remember(preferencesRevision) {
        mutableStateOf(GesturePrefs.isEnabled(context))
    }
    var right by remember(preferencesRevision) {
        mutableStateOf(GesturePrefs.isRightEnabled(context))
    }
    var left by remember(preferencesRevision) {
        mutableStateOf(GesturePrefs.isLeftEnabled(context))
    }
    var bottom by remember(preferencesRevision) {
        mutableStateOf(GesturePrefs.isBottomEnabled(context))
    }
    var visible by remember(preferencesRevision) {
        mutableStateOf(GesturePrefs.areZonesVisible(context))
    }
    var segmented by remember(preferencesRevision) {
        mutableStateOf(GesturePrefs.isSegmentModeEnabled(context))
    }
    var expandedActionGroup by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    val overlay =
        remember(refreshToken) {
            Settings.canDrawOverlays(context)
        }

    val accessibility =
        remember(refreshToken) {
            GestureAccessibilityService.isRunning()
        }

    val permissionsOk =
        overlay && accessibility

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        state = listState,
        contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Header(
                title = "GestureKing",
                subtitle = "Version ${BuildConfig.VERSION_NAME} · dernikiausd",
                action = "Einstellungen",
                color = headerColor,
                onAction = onOpenSettings
            )
        }

        item {
            GkCard("Status", Stroke) {
                val activeCount = listOf(right, left, bottom).count { it }

                StatusRow(
                    "Berechtigungen",
                    if (permissionsOk) "Bereit" else "Prüfen"
                )
                val shizukuReachable =
                    remember(refreshToken) {
                        GestureShizuku.isReachable()
                    }

                val shizukuGranted =
                    remember(refreshToken) {
                        shizukuReachable &&
                            GestureShizuku.hasPermission()
                    }

                StatusRow(
                    "Shizuku",
                    when {
                        shizukuGranted ->
                            "Verbunden"

                        shizukuReachable ->
                            "Freigabe fehlt"

                        else ->
                            "Nicht erreichbar"
                    }
                )
                StatusRow(
                    "Modus",
                    if (segmented) "Segment" else "Normal"
                )
                StatusRow(
                    "Ränder",
                    "$activeCount/3 aktiv"
                )
                StatusRow(
                    "Darstellung",
                    "${if (visible) "Sichtbar" else "Unsichtbar"} · ${GesturePrefs.zoneSizeDp(context)} dp"
                )
            }
        }

        item {
            GkCard("Hauptschalter", accent) {
                ToggleRow(
                    title = if (enabled) "GestureKing aktiv" else "GestureKing pausiert",
                    subtitle = "Schaltet die Randgesten ein oder aus.",
                    checked = enabled,
                    enabled = permissionsOk,
                    accent = accent
                ) {
                    enabled = it
                    GesturePrefs.setEnabled(context, it)
                    GesturePrefs.setLastEvent(
                        context,
                        if (it) "GestureKing aktiviert." else "GestureKing deaktiviert."
                    )
                    restartOverlay()
                }
            }
        }

        item {
            GkCard("Gestenzonen", accent) {
                ToggleRow("Rechter Rand", "Swipe nach links erkennen.", right, permissionsOk, accent) {
                    right = it
                    GesturePrefs.setRightEnabled(context, it)
                    restartOverlay()
                }

                ToggleRow("Linker Rand", "Swipe nach rechts erkennen.", left, permissionsOk, accent) {
                    left = it
                    GesturePrefs.setLeftEnabled(context, it)
                    restartOverlay()
                }

                ToggleRow("Unterer Rand", "Swipe nach oben erkennen.", bottom, permissionsOk, accent) {
                    bottom = it
                    GesturePrefs.setBottomEnabled(context, it)
                    restartOverlay()
                }

                ToggleRow("Zonen sichtbar", "Zeigt die Randbereiche als Testhilfe an.", visible, permissionsOk, accent) {
                    visible = it
                    GesturePrefs.setZonesVisible(context, it)
                    restartOverlay()
                }

                ToggleRow("Segment-Modus", "Jeder Rand erhält drei eigene Bereiche.", segmented, permissionsOk, accent) {
                    segmented = it
                    GesturePrefs.setSegmentModeEnabled(context, it)
                    restartOverlay()
                }
            }
        }

        item {
            ActionGroups(
                segmented = segmented,
                preferencesRevision = preferencesRevision,
                accent = accent,
                expandedGroup = expandedActionGroup,
                onExpandedGroupChange = {
                    expandedActionGroup = it
                },
                restartOverlay = restartOverlay
            )
        }
    }
}

@Composable
private fun SettingsScreen(
    refreshToken: Int,
    accent: Color,
    headerColor: Color,
    accentKey: String,
    onAccentSelected: (Accent) -> Unit,
    onBack: () -> Unit,
    restartOverlay: () -> Unit,
    requestShizuku: () -> Unit,
    onPreferencesReset: () -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    var haptic by remember { mutableStateOf(GesturePrefs.isHapticEnabled(context)) }
    var bubble by remember { mutableStateOf(GesturePrefs.isToastEnabled(context)) }
    var zoneSize by remember { mutableIntStateOf(GesturePrefs.zoneSizeDp(context)) }
    var resetConfirmVisible by rememberSaveable {
        mutableStateOf(false)
    }

    var cameraRefresh by remember {
        mutableIntStateOf(0)
    }

    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            GesturePrefs.setLastEvent(
                context,
                if (granted) {
                    "Kamera-Berechtigung erteilt."
                } else {
                    "Kamera-Berechtigung abgelehnt."
                }
            )

            cameraRefresh++
        }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        state = listState,
        contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Header(
                title = "Einstellungen",
                subtitle = "GestureKing konfigurieren",
                action = "Zurück",
                color = headerColor,
                onAction = onBack
            )
        }

        item {
            GkCard("Feedback", Stroke) {
                ToggleRow("Haptik", "Kurzer Tick bei einer erkannten Geste.", haptic, true, accent) {
                    haptic = it
                    GesturePrefs.setHapticEnabled(context, it)
                }

                ToggleRow("Feedback-Bubble", "Zeigt kurz die ausgelöste Aktion.", bubble, true, accent) {
                    bubble = it
                    GesturePrefs.setToastEnabled(context, it)
                }
            }
        }

        item {
            GkCard("Breite / Höhe", Stroke) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(16, 24, 32, 40).forEach { size ->
                        ChoiceButton(
                            text = "${size}dp",
                            selected = zoneSize == size,
                            accent = accent,
                            modifier = Modifier.weight(1f)
                        ) {
                            zoneSize = size
                            GesturePrefs.setZoneSizeDp(context, size)
                            restartOverlay()
                        }
                    }
                }
            }
        }

        item {
            GkCard("Farben", Stroke) {
                Text(
                    "Die Farbe wechselt ohne Seiten-Neuaufbau.",
                    color = TextMuted,
                    fontSize = 14.sp
                )

                accents.forEach { option ->
                    AccentButton(
                        option = option,
                        selected = accentKey == option.key
                    ) {
                        onAccentSelected(option)
                    }
                }
            }
        }

        item {
            val overlayGranted =
                remember(refreshToken) {
                    Settings.canDrawOverlays(context)
                }

            val accessibilityGranted =
                remember(refreshToken) {
                    GestureAccessibilityService.isRunning()
                }

            val shizukuReachable =
                remember(refreshToken) {
                    GestureShizuku.isReachable()
                }

            val shizukuGranted =
                remember(refreshToken) {
                    shizukuReachable &&
                        GestureShizuku.hasPermission()
                }

            GkCard(
                "Berechtigungen & Diagnose",
                Stroke
            ) {
                PermissionAction(
                    title = "Overlay",
                    status =
                        if (overlayGranted) {
                            "Erlaubt"
                        } else {
                            "Fehlt"
                        },
                    ok = overlayGranted,
                    buttonText = "Overlay verwalten",
                    accent = accent,
                    enabled = true
                ) {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            android.net.Uri.parse(
                                "package:${context.packageName}"
                            )
                        )
                    )
                }

                PermissionAction(
                    title = "Bedienungshilfe",
                    status =
                        if (accessibilityGranted) {
                            "Aktiv"
                        } else {
                            "Nicht aktiv"
                        },
                    ok = accessibilityGranted,
                    buttonText =
                        "Bedienungshilfe öffnen",
                    accent = accent,
                    enabled = true
                ) {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_ACCESSIBILITY_SETTINGS
                        )
                    )
                }

                val cameraGranted =
                    remember(
                        refreshToken,
                        cameraRefresh
                    ) {
                        context.checkSelfPermission(
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    }

                PermissionAction(
                    title = "Kamera",
                    status =
                        if (cameraGranted) {
                            "Erlaubt"
                        } else {
                            "Fehlt"
                        },
                    ok = cameraGranted,
                    buttonText =
                        if (cameraGranted) {
                            "Kamera ist freigegeben"
                        } else {
                            "Kamera freigeben"
                        },
                    accent = accent,
                    enabled = !cameraGranted
                ) {
                    cameraPermissionLauncher.launch(
                        Manifest.permission.CAMERA
                    )
                }

                PermissionAction(
                    title = "Shizuku",
                    status =
                        when {
                            shizukuGranted ->
                                "Verbunden"

                            shizukuReachable ->
                                "Freigabe fehlt"

                            else ->
                                "Nicht erreichbar"
                        },
                    ok = shizukuGranted,
                    buttonText =
                        if (shizukuGranted) {
                            "Shizuku ist freigegeben"
                        } else {
                            "Shizuku freigeben"
                        },
                    accent = accent,
                    enabled =
                        shizukuReachable &&
                            !shizukuGranted
                ) {
                    requestShizuku()
                }

                HorizontalDivider(color = Stroke)

                Label("Letztes Ereignis")

                Text(
                    GesturePrefs.lastEvent(context),
                    color = TextMain,
                    fontSize = 14.sp
                )

                Label("Rohdaten")

                Text(
                    GesturePrefs.lastRawGesture(context),
                    color = TextMuted,
                    fontSize = 13.sp
                )
            }
        }

        item {
            GkCard(
                "Einstellungen zurücksetzen",
                Stroke
            ) {
                Text(
                    "Setzt Gesten, App-Ziele, Feedback und Zonengröße zurück. Normal-/Segment-Modus und Android-Berechtigungen bleiben erhalten.",
                    color = TextMuted,
                    fontSize = 14.sp
                )

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        resetConfirmVisible = true
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, accent),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                Color(0xFF202833)
                        )
                ) {
                    Text(
                        "Zurücksetzen vorbereiten",
                        color = TextMain
                    )
                }
            }
        }

        if (resetConfirmVisible) {
            item {
                ResetConfirmDialog(
                    accent = accent,
                    onDismiss = {
                        resetConfirmVisible = false
                    },
                    onConfirm = {
                        resetConfirmVisible = false

                        GesturePrefs.resetUserSettingsKeepMode(
                            context
                        )

                        haptic =
                            GesturePrefs.isHapticEnabled(
                                context
                            )

                        bubble =
                            GesturePrefs.isToastEnabled(
                                context
                            )

                        zoneSize =
                            GesturePrefs.zoneSizeDp(
                                context
                            )

                        restartOverlay()
                        onPreferencesReset()
                    }
                )
            }
        }

        item {
            GkCard("Über GestureKing", Stroke) {
                Text(
                    "Drei Randbereiche mit frei belegbaren Normal- und Segmentgesten: rechts, links und unten.",
                    color = TextMuted,
                    fontSize = 14.sp
                )
            }
        }
    }
}


private data class GestureEntry(
    val title: String,
    val key: String
)

private data class GestureGroup(
    val id: String,
    val title: String,
    val entries: List<GestureEntry>
)

private val normalGestureGroups =
    listOf(
        GestureGroup(
            id = "normal_right",
            title = "Rechte Gesten",
            entries =
                listOf(
                    GestureEntry(
                        "Rechter kurzer Swipe ←",
                        GesturePrefs.GESTURE_RIGHT_SHORT
                    ),
                    GestureEntry(
                        "Rechter langer Swipe ←",
                        GesturePrefs.GESTURE_RIGHT_LONG
                    ),
                    GestureEntry(
                        "Rechter Rand · Doppeltippen",
                        GesturePrefs.GESTURE_RIGHT_DOUBLE
                    )
                )
        ),
        GestureGroup(
            id = "normal_left",
            title = "Linke Gesten",
            entries =
                listOf(
                    GestureEntry(
                        "Linker kurzer Swipe →",
                        GesturePrefs.GESTURE_LEFT_SHORT
                    ),
                    GestureEntry(
                        "Linker langer Swipe →",
                        GesturePrefs.GESTURE_LEFT_LONG
                    ),
                    GestureEntry(
                        "Linker Rand · Doppeltippen",
                        GesturePrefs.GESTURE_LEFT_DOUBLE
                    )
                )
        ),
        GestureGroup(
            id = "normal_bottom",
            title = "Untere Gesten",
            entries =
                listOf(
                    GestureEntry(
                        "Unterer kurzer Swipe ↑",
                        GesturePrefs.GESTURE_BOTTOM_SHORT
                    ),
                    GestureEntry(
                        "Unterer langer Swipe ↑",
                        GesturePrefs.GESTURE_BOTTOM_LONG
                    ),
                    GestureEntry(
                        "Unterer Rand · Doppeltippen",
                        GesturePrefs.GESTURE_BOTTOM_DOUBLE
                    )
                )
        )
    )

private val segmentGestureGroups =
    listOf(
        GestureGroup(
            id = "segment_right",
            title = "Rechter Rand",
            entries =
                listOf(
                    GestureEntry(
                        "Rechts oben ←",
                        GesturePrefs.GESTURE_RIGHT_TOP
                    ),
                    GestureEntry(
                        "Rechts Mitte ←",
                        GesturePrefs.GESTURE_RIGHT_MIDDLE
                    ),
                    GestureEntry(
                        "Rechts unten ←",
                        GesturePrefs.GESTURE_RIGHT_BOTTOM
                    )
                )
        ),
        GestureGroup(
            id = "segment_left",
            title = "Linker Rand",
            entries =
                listOf(
                    GestureEntry(
                        "Links oben →",
                        GesturePrefs.GESTURE_LEFT_TOP
                    ),
                    GestureEntry(
                        "Links Mitte →",
                        GesturePrefs.GESTURE_LEFT_MIDDLE
                    ),
                    GestureEntry(
                        "Links unten →",
                        GesturePrefs.GESTURE_LEFT_BOTTOM
                    )
                )
        ),
        GestureGroup(
            id = "segment_bottom",
            title = "Unterer Rand",
            entries =
                listOf(
                    GestureEntry(
                        "Unten links ↑",
                        GesturePrefs.GESTURE_BOTTOM_LEFT
                    ),
                    GestureEntry(
                        "Unten Mitte ↑",
                        GesturePrefs.GESTURE_BOTTOM_CENTER
                    ),
                    GestureEntry(
                        "Unten rechts ↑",
                        GesturePrefs.GESTURE_BOTTOM_RIGHT
                    )
                )
        )
    )

private val selectableActions =
    listOf(
        GesturePrefs.ACTION_BACK,
        GesturePrefs.ACTION_HOME,
        GesturePrefs.ACTION_RECENTS,
        GesturePrefs.ACTION_NOTIFICATIONS,
        GesturePrefs.ACTION_QUICK_SETTINGS,
        GesturePrefs.ACTION_SCREEN_OFF,
        GesturePrefs.ACTION_ROTATION_TOGGLE,
        GesturePrefs.ACTION_FLASHLIGHT_TOGGLE,
        GesturePrefs.ACTION_BROWSER_FORWARD,
        GesturePrefs.ACTION_OPEN_APP_1,
        GesturePrefs.ACTION_NONE
    )

@Composable
private fun ActionGroups(
    segmented: Boolean,
    preferencesRevision: Int,
    accent: Color,
    expandedGroup: String?,
    onExpandedGroupChange: (String?) -> Unit,
    restartOverlay: () -> Unit
) {
    val groups =
        if (segmented) {
            segmentGestureGroups
        } else {
            normalGestureGroups
        }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GkCard(
            title =
                if (segmented) {
                    "Segment-Aktionen"
                } else {
                    "Normal-Aktionen"
                },
            borderColor = accent
        ) {
            Text(
                if (segmented) {
                    "Drei getrennte Bereiche je Rand · neun Gestenaktionen."
                } else {
                    "Kurze, lange und Doppeltipp-Gesten je Rand."
                },
                color = TextMuted,
                fontSize = 14.sp
            )
        }

        groups.forEach { group ->
            ActionGroupCard(
                group = group,
                preferencesRevision = preferencesRevision,
                accent = accent,
                expanded = expandedGroup == group.id,
                onToggle = {
                    onExpandedGroupChange(
                        if (expandedGroup == group.id) {
                            null
                        } else {
                            group.id
                        }
                    )
                },
                restartOverlay = restartOverlay
            )
        }
    }
}

@Composable
private fun ActionGroupCard(
    group: GestureGroup,
    preferencesRevision: Int,
    accent: Color,
    expanded: Boolean,
    onToggle: () -> Unit,
    restartOverlay: () -> Unit
) {
    val context = LocalContext.current
    var refreshKey by remember {
        mutableIntStateOf(0)
    }

    GkCard(
        title = group.title,
        borderColor = accent
    ) {
        group.entries.forEach { entry ->
            val current =
                remember(
                    refreshKey,
                    preferencesRevision,
                    entry.key
                ) {
                    GesturePrefs.actionFor(
                        context,
                        entry.key
                    )
                }

            Text(
                "${entry.title}: ${
                    actionLabelForCompose(
                        context = context,
                        gesture = entry.key,
                        action = current
                    )
                }",
                color = TextMuted,
                fontSize = 13.5.sp
            )
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onToggle,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, accent),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        if (expanded) {
                            accent.copy(alpha = 0.28f)
                        } else {
                            Color(0xFF202833)
                        }
                )
        ) {
            Text(
                if (expanded) {
                    "${group.title} einklappen ▲"
                } else {
                    "${group.title} anzeigen ▼"
                },
                color = TextMain
            )
        }

        if (expanded) {
            group.entries.forEach { entry ->
                GestureActionEditor(
                    entry = entry,
                    preferencesRevision = preferencesRevision,
                    accent = accent,
                    onActionChanged = {
                        refreshKey++
                        restartOverlay()
                    }
                )
            }
        }
    }
}

@Composable
private fun GestureActionEditor(
    entry: GestureEntry,
    preferencesRevision: Int,
    accent: Color,
    onActionChanged: () -> Unit
) {
    val context = LocalContext.current
    var selectedAction by remember(
        entry.key,
        preferencesRevision
    ) {
        mutableStateOf(
            GesturePrefs.actionFor(
                context,
                entry.key
            )
        )
    }

    var showAppPicker by rememberSaveable(entry.key) {
        mutableStateOf(false)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Stroke),
        colors =
            CardDefaults.cardColors(
                containerColor = Color(0xFF111923)
            )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text(
                entry.title,
                color = TextMain,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Aktuell: ${
                    actionLabelForCompose(
                        context = context,
                        gesture = entry.key,
                        action = selectedAction
                    )
                }",
                color = TextMuted,
                fontSize = 13.5.sp
            )

            if (
                GesturePrefs.isOpenAppAction(
                    selectedAction
                )
            ) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        showAppPicker = true
                    },
                    shape = RoundedCornerShape(11.dp),
                    border = BorderStroke(1.dp, accent),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                accent.copy(alpha = 0.22f)
                        )
                ) {
                    Text(
                        "Ziel-App auswählen",
                        color = TextMain
                    )
                }
            }

            selectableActions
                .chunked(2)
                .forEach { rowActions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {
                        rowActions.forEach { action ->
                            ActionChoiceButton(
                                action = action,
                                selected =
                                    selectedAction == action,
                                accent = accent,
                                modifier = Modifier.weight(1f)
                            ) {
                                selectedAction = action

                                GesturePrefs.setAction(
                                    context,
                                    entry.key,
                                    action
                                )

                                GesturePrefs.setLastEvent(
                                    context,
                                    "Aktion gesetzt: ${entry.title} = ${GesturePrefs.actionLabel(action)}"
                                )

                                if (
                                    GesturePrefs.isOpenAppAction(
                                        action
                                    )
                                ) {
                                    showAppPicker = true
                                }

                                onActionChanged()
                            }
                        }

                        if (rowActions.size == 1) {
                            Spacer(
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
        }
    }

    if (showAppPicker) {
        AppPickerDialog(
            gesture = entry.key,
            gestureTitle = entry.title,
            accent = accent,
            onDismiss = {
                showAppPicker = false
            },
            onAppSelected = { app ->
                GesturePrefs.setOpenAppTarget(
                    context,
                    entry.key,
                    app.packageName,
                    app.label
                )

                GesturePrefs.setAction(
                    context,
                    entry.key,
                    GesturePrefs.ACTION_OPEN_APP_1
                )

                selectedAction =
                    GesturePrefs.ACTION_OPEN_APP_1

                GesturePrefs.setLastEvent(
                    context,
                    "${entry.title} öffnet jetzt: ${app.label}"
                )

                showAppPicker = false
                onActionChanged()
            }
        )
    }
}

private data class LaunchableApp(
    val label: String,
    val packageName: String
)

@Composable
private fun AppPickerDialog(
    gesture: String,
    gestureTitle: String,
    accent: Color,
    onDismiss: () -> Unit,
    onAppSelected: (LaunchableApp) -> Unit
) {
    val context = LocalContext.current

    var query by rememberSaveable(gesture) {
        mutableStateOf("")
    }

    val apps =
        remember(gesture) {
            loadLaunchableApps(context)
        }

    val filtered =
        remember(apps, query) {
            if (query.isBlank()) {
                apps
            } else {
                apps.filter { app ->
                    app.label.contains(
                        query,
                        ignoreCase = true
                    ) ||
                        app.packageName.contains(
                            query,
                            ignoreCase = true
                        )
                }
            }
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Ziel-App auswählen",
                color = TextMain,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    gestureTitle,
                    color = TextMuted,
                    fontSize = 13.5.sp
                )

                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = {
                        Text("App suchen")
                    },
                    placeholder = {
                        Text("z. B. Kamera, ChatGPT …")
                    },
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextMain,
                            unfocusedTextColor = TextMain,
                            focusedBorderColor = accent,
                            unfocusedBorderColor = Stroke,
                            focusedLabelColor = accent,
                            unfocusedLabelColor = TextMuted,
                            cursorColor = accent
                        )
                )

                Text(
                    "${filtered.size} Treffer",
                    color = TextMuted,
                    fontSize = 13.sp
                )

                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(6.dp)
                ) {
                    items(
                        count = filtered.size,
                        key = { index ->
                            filtered[index].packageName
                        }
                    ) { index ->
                        val app = filtered[index]

                        Button(
                            modifier =
                                Modifier.fillMaxWidth(),
                            onClick = {
                                onAppSelected(app)
                            },
                            shape =
                                RoundedCornerShape(10.dp),
                            border =
                                BorderStroke(
                                    1.dp,
                                    Stroke
                                ),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor =
                                        Color(0xFF202833)
                                ),
                            contentPadding =
                                PaddingValues(
                                    horizontal = 12.dp,
                                    vertical = 9.dp
                                )
                        ) {
                            Column(
                                modifier =
                                    Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    app.label,
                                    color = TextMain,
                                    fontSize = 14.sp,
                                    fontWeight =
                                        FontWeight.SemiBold
                                )

                                Text(
                                    app.packageName,
                                    color = TextMuted,
                                    fontSize = 11.5.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    "Schließen",
                    color = accent
                )
            }
        },
        containerColor = CardBg
    )
}

private fun loadLaunchableApps(
    context: android.content.Context
): List<LaunchableApp> {
    val intent =
        Intent(
            Intent.ACTION_MAIN
        ).apply {
            addCategory(
                Intent.CATEGORY_LAUNCHER
            )
        }

    return context.packageManager
        .queryIntentActivities(
            intent,
            0
        )
        .mapNotNull { info ->
            val label =
                info.loadLabel(
                    context.packageManager
                )?.toString()?.trim().orEmpty()

            val packageName =
                info.activityInfo?.packageName.orEmpty()

            if (
                label.isBlank() ||
                packageName.isBlank()
            ) {
                null
            } else {
                LaunchableApp(
                    label = label,
                    packageName = packageName
                )
            }
        }
        .distinctBy {
            it.packageName
        }
        .sortedBy {
            it.label.lowercase()
        }
}

private fun actionLabelForCompose(
    context: android.content.Context,
    gesture: String,
    action: String
): String {
    return if (
        GesturePrefs.isOpenAppAction(
            action
        )
    ) {
        "App öffnen: ${
            GesturePrefs.openAppTargetLabel(
                context,
                gesture
            )
        }"
    } else {
        GesturePrefs.actionLabel(action)
    }
}

@Composable
private fun ActionChoiceButton(
    action: String,
    selected: Boolean,
    accent: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Button(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(11.dp),
        border =
            BorderStroke(
                1.dp,
                if (selected) {
                    accent
                } else {
                    Stroke
                }
            ),
        colors =
            ButtonDefaults.buttonColors(
                containerColor =
                    if (selected) {
                        accent.copy(alpha = 0.30f)
                    } else {
                        Color(0xFF202833)
                    }
            ),
        contentPadding =
            PaddingValues(
                horizontal = 6.dp,
                vertical = 8.dp
            )
    ) {
        Text(
            if (selected) {
                "✓ ${shortActionLabel(action)}"
            } else {
                shortActionLabel(action)
            },
            color = TextMain,
            fontSize = 12.sp
        )
    }
}

private fun shortActionLabel(
    action: String
): String {
    return when (action) {
        GesturePrefs.ACTION_RECENTS ->
            "Letzte"

        GesturePrefs.ACTION_NOTIFICATIONS ->
            "Benachr."

        GesturePrefs.ACTION_QUICK_SETTINGS ->
            "Quick"

        GesturePrefs.ACTION_SCREEN_OFF ->
            "Bild aus"

        GesturePrefs.ACTION_ROTATION_TOGGLE ->
            "Rotation"

        GesturePrefs.ACTION_FLASHLIGHT_TOGGLE ->
            "Licht"

        GesturePrefs.ACTION_BROWSER_FORWARD ->
            "Browser vor"

        GesturePrefs.ACTION_OPEN_APP,
        GesturePrefs.ACTION_OPEN_APP_1,
        GesturePrefs.ACTION_OPEN_APP_2,
        GesturePrefs.ACTION_OPEN_APP_3 ->
            "App öffnen"

        GesturePrefs.ACTION_NONE ->
            "Keine"

        else ->
            GesturePrefs.actionLabel(action)
    }
}

@Composable
private fun Header(
    title: String,
    subtitle: String,
    action: String,
    color: Color,
    onAction: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp, 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = TextMain,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    subtitle,
                    color = Color(0xFFC1D2EA),
                    fontSize = 14.sp
                )
            }

            Text(
                action,
                modifier = Modifier
                    .clickable(onClick = onAction)
                    .padding(8.dp),
                color = TextMain,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun GkCard(
    title: String,
    borderColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(
            modifier = Modifier.padding(17.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                title,
                color = TextMain,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            content()
        }
    }
}

@Composable
private fun StatusRow(
    title: String,
    value: String
) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            title,
            modifier = Modifier.weight(1f),
            color = TextMuted,
            fontSize = 13.5.sp
        )
        Text(
            value,
            color = TextMain,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    accent: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = if (enabled) TextMain else TextMuted,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                subtitle,
                color = TextMuted,
                fontSize = 13.sp
            )
        }

        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = accent,
                checkedThumbColor = Color.White
            )
        )
    }
}

@Composable
private fun ChoiceButton(
    text: String,
    selected: Boolean,
    accent: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Button(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (selected) accent else Stroke),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) accent.copy(alpha = 0.25f) else Color(0xFF202833)
        ),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        Text(text, color = TextMain, fontSize = 13.sp)
    }
}

@Composable
private fun AccentButton(
    option: Accent,
    selected: Boolean,
    onClick: () -> Unit
) {
    Button(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (selected) option.color else Stroke),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) option.header else Color(0xFF202833)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(13.dp)
                    .background(option.color, CircleShape)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                if (selected) "✓ ${option.title}" else option.title,
                color = TextMain
            )
        }
    }
}

@Composable
private fun ResetConfirmDialog(
    accent: Color,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Einstellungen zurücksetzen?",
                color = TextMain,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                "Alle Gesten und App-Ziele werden geleert. Feedback und Zonengröße werden auf Standard gesetzt. Der aktuelle Normal-/Segment-Modus und die Android-Berechtigungen bleiben erhalten.",
                color = TextMuted,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text(
                    "Jetzt zurücksetzen",
                    color = accent,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    "Abbrechen",
                    color = TextMuted
                )
            }
        },
        containerColor = CardBg
    )
}

@Composable
private fun PermissionAction(
    title: String,
    status: String,
    ok: Boolean,
    buttonText: String,
    accent: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Column(
        verticalArrangement =
            Arrangement.spacedBy(6.dp)
    ) {
        Text(
            "$title: $status",
            color =
                if (ok) {
                    Success
                } else {
                    Warning
                },
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onClick,
            enabled = enabled,
            shape = RoundedCornerShape(12.dp),
            border =
                BorderStroke(
                    1.dp,
                    if (enabled) {
                        accent
                    } else {
                        Stroke
                    }
                ),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        Color(0xFF202833),
                    disabledContainerColor =
                        Color(0xFF202833),
                    disabledContentColor =
                        TextMuted
                )
        ) {
            Text(
                buttonText,
                color =
                    if (enabled) {
                        TextMain
                    } else {
                        TextMuted
                    }
            )
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text,
        color = TextMain,
        fontWeight = FontWeight.Bold
    )
}
