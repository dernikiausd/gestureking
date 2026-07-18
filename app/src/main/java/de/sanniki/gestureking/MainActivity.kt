package de.sanniki.gestureking

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import rikka.shizuku.Shizuku
import android.os.Build
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher

class MainActivity : Activity() {

    private lateinit var rootScroll: ScrollView
    private var segmentAnchorView: View? = null
    private var settingsAnchorView: View? = null
    private var lastScrollY: Int = 0

    private lateinit var statusDot: TextView
    private lateinit var statusText: TextView
    private lateinit var statusSubtitleText: TextView
    private lateinit var statusRightsText: TextView
    private lateinit var statusShizukuText: TextView
    private lateinit var statusModeText: TextView
    private lateinit var statusZonesText: TextView
    private lateinit var statusDisplayText: TextView
    private lateinit var statusFeedbackText: TextView

    private lateinit var lastEventText: TextView

    private lateinit var masterTitleText: TextView
    private lateinit var masterSwitch: Switch
    private lateinit var rightSwitch: Switch
    private lateinit var leftSwitch: Switch
    private lateinit var bottomSwitch: Switch
    private lateinit var visibleSwitch: Switch
    private lateinit var segmentedSwitch: Switch
    private lateinit var hapticSwitch: Switch
    private lateinit var toastSwitch: Switch

    private var appListExpandedSlot: Int = 0
    private var appPickerGesture: String? = null
    private var appSearchQuery: String = ""
    private var expandedGesture: String? = null
    private var expandedSegmentGroup: String? = null
    private var resetConfirmArmed: Boolean = false
    private var feedbackExpanded: Boolean = false
    private var maintenanceExpanded: Boolean = false

    private var settingsVisible: Boolean = false
    private var accentColorKey: String = "blue"
    private val actionCurrentLabels = mutableMapOf<String, TextView>()
    private val actionButtonsByGesture = mutableMapOf<String, MutableList<Button>>()
    private val actionOptionContainers = mutableMapOf<String, LinearLayout>()
    private val actionToggleButtons = mutableMapOf<String, Button>()

    private val appSlotContainers = mutableMapOf<Int, LinearLayout>()
    private val appSlotToggleButtons = mutableMapOf<Int, Button>()
    private val appSlotTargetLabels = mutableMapOf<Int, TextView>()

    private val segmentGroupContainers = mutableMapOf<String, LinearLayout>()
    private val segmentGroupToggleButtons = mutableMapOf<String, Button>()

    private val shizukuPermissionRequestCode = 9404

    private val shizukuPermissionListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == shizukuPermissionRequestCode) {
                runOnUiThread {
                    GesturePrefs.setLastEvent(
                        this,
                        if (grantResult == PackageManager.PERMISSION_GRANTED) {
                            "Shizuku-Berechtigung erteilt."
                        } else {
                            "Shizuku-Berechtigung abgelehnt."
                        }
                    )
                    updateUi()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)
        } catch (_: Throwable) {
        }

        accentColorKey =
            getSharedPreferences(
                ACCENT_PREFS_NAME,
                android.content.Context.MODE_PRIVATE
            ).getString(
                ACCENT_PREF_KEY,
                "blue"
            ) ?: "blue"

        buildUi()
        updateUi()
    

        setupNativeBackCallback()
}

    override fun onDestroy() {
        try {
            Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
        } catch (_: Throwable) {
        }

        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()

        if (settingsVisible) {
            buildUi()
        } else {
            collapseExpandedUi()
            updateUi()
        }
    }

    override fun onPause() {
        collapseExpandedUi()
        super.onPause()
    }

    private fun collapseExpandedUi() {
        expandedGesture = null
        expandedSegmentGroup = null
        appListExpandedSlot = 0
        resetConfirmArmed = false

        if (actionOptionContainers.isNotEmpty()) {
            updateExpandedActionCards()
        }

        if (appSlotContainers.isNotEmpty()) {
            updateExpandedAppTargetCards()
        }

        if (segmentGroupContainers.isNotEmpty()) {
            updateExpandedSegmentGroups()
        }
    }

    private fun buildUi() {
        actionCurrentLabels.clear()
        actionButtonsByGesture.clear()
        actionOptionContainers.clear()
        actionToggleButtons.clear()
        appSlotContainers.clear()
        appSlotToggleButtons.clear()
        appSlotTargetLabels.clear()
        segmentGroupContainers.clear()
        segmentGroupToggleButtons.clear()

        rootScroll = ScrollView(this).apply {
            setBackgroundColor(COLOR_BG)
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(48), dp(16), dp(24))
            setBackgroundColor(COLOR_BG)
        }

        rootScroll.addView(root)
        setContentView(rootScroll)


        if (settingsVisible) {
            buildSettingsNavigationUi(root)
            return
        }
        val signatureHeader =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(20),
                    dp(18),
                    dp(18),
                    dp(18)
                )

                background =
                    GradientDrawable().apply {
                        shape =
                            GradientDrawable.RECTANGLE

                        cornerRadius =
                            dp(26).toFloat()

                        setColor(
                            accentHeaderColor()
                        )
                    }
            }

        val headerTopRow =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    android.view.Gravity.CENTER_VERTICAL
            }

        val headerTitleColumn =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
            }

        headerTitleColumn.addView(
            TextView(this).apply {
                text = "GestureKing"
                textSize = 29f
                typeface =
                    Typeface.DEFAULT_BOLD

                setTextColor(
                    Color.parseColor(
                        "#EAF2FF"
                    )
                )

                maxLines = 1
            }
        )

        headerTitleColumn.addView(
            TextView(this).apply {
                text =
                    "Version 0.14 · dernikiausd"

                textSize = 14f

                setTextColor(
                    Color.parseColor(
                        "#C1D2EA"
                    )
                )

                maxLines = 1
            }
        )

        headerTopRow.addView(
            headerTitleColumn,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        headerTopRow.addView(
            TextView(this).apply {
                text = "Einstellungen"
                textSize = 15f
                typeface =
                    Typeface.DEFAULT_BOLD

                setTextColor(
                    Color.parseColor(
                        "#EAF2FF"
                    )
                )

                setPadding(
                    dp(8),
                    dp(10),
                    dp(2),
                    dp(10)
                )

                isClickable = true
                isFocusable = true

                setOnClickListener {
                    settingsVisible = true
                    feedbackExpanded = false
                    maintenanceExpanded = false
                    resetConfirmArmed = false
                    lastScrollY = 0

                    buildUi()
                }
            }
        )

        signatureHeader.addView(
            headerTopRow
        )

        signatureHeader.addView(
            TextView(this).apply {
                text =
                    "Randgesten & Schnellaktionen"

                textSize = 14f

                setTextColor(
                    Color.parseColor(
                        "#C1D2EA"
                    )
                )

                setPadding(
                    0,
                    dp(12),
                    0,
                    0
                )
            }
        )

        root.addView(
            signatureHeader,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(14)
            }
        )

        val statusCard =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(17),
                    dp(16),
                    dp(17),
                    dp(16)
                )

                background =
                    GradientDrawable().apply {
                        shape =
                            GradientDrawable.RECTANGLE

                        cornerRadius =
                            dp(20).toFloat()

                        setColor(COLOR_CARD)
                    }
            }

        statusCard.addView(
            sectionTitle("Status")
        )

        val serviceStatusRow =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    android.view.Gravity.CENTER_VERTICAL

                setPadding(
                    0,
                    dp(3),
                    0,
                    dp(12)
                )
            }

        statusDot =
            TextView(this).apply {
                text = "●"
                textSize = 17f

                setTextColor(
                    Color.parseColor(
                        "#57C785"
                    )
                )

                setPadding(
                    0,
                    0,
                    dp(10),
                    0
                )
            }

        val serviceStatusTexts =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
            }

        statusText =
            TextView(this).apply {
                text =
                    "Status wird geprüft…"

                textSize = 17f

                typeface =
                    Typeface.DEFAULT_BOLD

                setTextColor(COLOR_TEXT)
            }

        statusSubtitleText =
            TextView(this).apply {
                text =
                    "Gestenstatus wird geladen."

                textSize = 13f

                setTextColor(COLOR_MUTED)
            }

        serviceStatusTexts.addView(
            statusText
        )

        serviceStatusTexts.addView(
            statusSubtitleText
        )

        serviceStatusRow.addView(
            statusDot
        )

        serviceStatusRow.addView(
            serviceStatusTexts
        )

        statusCard.addView(
            serviceStatusRow
        )

        statusRightsText =
            statusValueRow(
                parent = statusCard,
                title = "Berechtigungen"
            )

        statusShizukuText =
            statusValueRow(
                parent = statusCard,
                title = "Shizuku"
            )

        statusModeText =
            statusValueRow(
                parent = statusCard,
                title = "Modus"
            )

        statusZonesText =
            statusValueRow(
                parent = statusCard,
                title = "Ränder"
            )

        statusDisplayText =
            statusValueRow(
                parent = statusCard,
                title = "Darstellung"
            )

        statusFeedbackText =
            statusValueRow(
                parent = statusCard,
                title = "Feedback"
            )

        root.addView(
            statusCard,
            matchWrap()
        )

        val controlCard = quietCard()
        applyAccentCardStyle(controlCard)
        controlCard.addView(sectionTitle("Hauptschalter"))

        val masterRow =
            switchRowWithTitle(
                parent = controlCard,
                title = "GestureKing wird geprüft…",
                subtitle =
                    "Schaltet die Randgesten ein oder aus."
            ) { checked ->
                GesturePrefs.setEnabled(
                    this,
                    checked
                )

                restartOverlay()

                GesturePrefs.setLastEvent(
                    this,
                    if (checked) {
                        "GestureKing aktiviert."
                    } else {
                        "GestureKing deaktiviert."
                    }
                )

                updateUi()
            }

        masterTitleText =
            masterRow.first

        masterSwitch =
            masterRow.second

        root.addView(controlCard, matchWrap())

        val zoneCard = quietCard()
        applyAccentCardStyle(zoneCard)
        segmentAnchorView = zoneCard
        zoneCard.addView(sectionTitle("Gestenzonen"))

        rightSwitch = switchRow(
            parent = zoneCard,
            title = "Rechter Rand",
            subtitle = "Swipe nach links erkennen."
        ) { checked ->
            GesturePrefs.setRightEnabled(this, checked)
            restartOverlay()
            updateUi()
        }

        leftSwitch = switchRow(
            parent = zoneCard,
            title = "Linker Rand",
            subtitle = "Swipe nach rechts erkennen."
        ) { checked ->
            GesturePrefs.setLeftEnabled(this, checked)
            restartOverlay()
            updateUi()
        }

        bottomSwitch = switchRow(
            parent = zoneCard,
            title = "Unterer Rand",
            subtitle = "Swipe nach oben erkennen."
        ) { checked ->
            GesturePrefs.setBottomEnabled(this, checked)
            restartOverlay()
            updateUi()
        }

        visibleSwitch = switchRow(
            parent = zoneCard,
            title = "Zonen sichtbar",
            subtitle = "Zeigt die Randbereiche als Testhilfe an."
        ) { checked ->
            GesturePrefs.setZonesVisible(this, checked)
            restartOverlay()
            updateUi()
        }

        segmentedSwitch = switchRow(
            parent = zoneCard,
            title = "Segment-Modus",
            subtitle = "Jeder Rand bekommt drei eigene Bereiche mit eigener Aktion."
        ) { checked ->
            GesturePrefs.setSegmentModeEnabled(this, checked)
            GesturePrefs.setLastEvent(
                this,
                if (checked) "Segment-Modus aktiviert." else "Segment-Modus deaktiviert."
            )
            rebuildAndScrollToSegmentAnchor()
            rootScroll.postDelayed({
                restartOverlay()
            }, 120L)
        }

        root.addView(zoneCard, matchWrap())

        if (!GesturePrefs.isSegmentModeEnabled(this)) {
            val normalInfoCard = card()
            normalInfoCard.addView(sectionTitle("Normal-Aktionen"))
            normalInfoCard.addView(description("Aktiv: kurze, lange und Doppeltipp-Gesten pro Seite."))
            root.addView(normalInfoCard, matchWrap())

            root.addView(segmentGroupCard(
                title = "Rechte Gesten",
                key = "normal_right",
                summary = listOf(
                    "kurz: ${actionLabelForUi(GesturePrefs.actionFor(this, GesturePrefs.GESTURE_RIGHT_SHORT))}",
                    "lang: ${actionLabelForUi(GesturePrefs.actionFor(this, GesturePrefs.GESTURE_RIGHT_LONG))}",
                    "doppelt: ${actionLabelForUi(GesturePrefs.actionFor(this, GesturePrefs.GESTURE_RIGHT_DOUBLE))}"
                ),
                entries = listOf(
                    "Rechter kurzer Swipe ←" to GesturePrefs.GESTURE_RIGHT_SHORT,
                    "Rechter langer Swipe ←" to GesturePrefs.GESTURE_RIGHT_LONG,
                    "Rechter Rand · Doppeltippen" to GesturePrefs.GESTURE_RIGHT_DOUBLE
                )
            ), matchWrap())

            root.addView(segmentGroupCard(
                title = "Linke Gesten",
                key = "normal_left",
                summary = listOf(
                    "kurz: ${actionLabelForUi(GesturePrefs.actionFor(this, GesturePrefs.GESTURE_LEFT_SHORT))}",
                    "lang: ${actionLabelForUi(GesturePrefs.actionFor(this, GesturePrefs.GESTURE_LEFT_LONG))}",
                    "doppelt: ${actionLabelForUi(GesturePrefs.actionFor(this, GesturePrefs.GESTURE_LEFT_DOUBLE))}"
                ),
                entries = listOf(
                    "Linker kurzer Swipe →" to GesturePrefs.GESTURE_LEFT_SHORT,
                    "Linker langer Swipe →" to GesturePrefs.GESTURE_LEFT_LONG,
                    "Linker Rand · Doppeltippen" to GesturePrefs.GESTURE_LEFT_DOUBLE
                )
            ), matchWrap())

            root.addView(segmentGroupCard(
                title = "Untere Gesten",
                key = "normal_bottom",
                summary = listOf(
                    "kurz: ${actionLabelForUi(GesturePrefs.actionFor(this, GesturePrefs.GESTURE_BOTTOM_SHORT))}",
                    "lang: ${actionLabelForUi(GesturePrefs.actionFor(this, GesturePrefs.GESTURE_BOTTOM_LONG))}",
                    "doppelt: ${actionLabelForUi(GesturePrefs.actionFor(this, GesturePrefs.GESTURE_BOTTOM_DOUBLE))}"
                ),
                entries = listOf(
                    "Unterer kurzer Swipe ↑" to GesturePrefs.GESTURE_BOTTOM_SHORT,
                    "Unterer langer Swipe ↑" to GesturePrefs.GESTURE_BOTTOM_LONG,
                    "Unterer Rand · Doppeltippen" to GesturePrefs.GESTURE_BOTTOM_DOUBLE
                )
            ), matchWrap())
        }

        if (GesturePrefs.isSegmentModeEnabled(this)) {
            val segmentInfoCard = card()
            segmentInfoCard.addView(sectionTitle("Segment-Aktionen"))
            segmentInfoCard.addView(
                description(
                    "3 Bereiche je Rand · 9 getrennte Gestenaktionen"
                )
            )
            root.addView(segmentInfoCard, matchWrap())

            root.addView(segmentGroupCard(
                title = "Rechte Gesten",
                key = "right",
                summary = listOf(
                    "oben: ${actionLabelForUi(GesturePrefs.actionFor(this, GesturePrefs.GESTURE_RIGHT_TOP))}",
                    "mitte: ${actionLabelForUi(GesturePrefs.actionFor(this, GesturePrefs.GESTURE_RIGHT_MIDDLE))}",
                    "unten: ${actionLabelForUi(GesturePrefs.actionFor(this, GesturePrefs.GESTURE_RIGHT_BOTTOM))}"
                ),
                entries = listOf(
                    "Rechter Rand oben ←" to GesturePrefs.GESTURE_RIGHT_TOP,
                    "Rechter Rand mitte ←" to GesturePrefs.GESTURE_RIGHT_MIDDLE,
                    "Rechter Rand unten ←" to GesturePrefs.GESTURE_RIGHT_BOTTOM
                )
            ), matchWrap())

            root.addView(segmentGroupCard(
                title = "Linke Gesten",
                key = "left",
                summary = listOf(
                    "oben: ${actionLabelForUi(GesturePrefs.actionFor(this, GesturePrefs.GESTURE_LEFT_TOP))}",
                    "mitte: ${actionLabelForUi(GesturePrefs.actionFor(this, GesturePrefs.GESTURE_LEFT_MIDDLE))}",
                    "unten: ${actionLabelForUi(GesturePrefs.actionFor(this, GesturePrefs.GESTURE_LEFT_BOTTOM))}"
                ),
                entries = listOf(
                    "Linker Rand oben →" to GesturePrefs.GESTURE_LEFT_TOP,
                    "Linker Rand mitte →" to GesturePrefs.GESTURE_LEFT_MIDDLE,
                    "Linker Rand unten →" to GesturePrefs.GESTURE_LEFT_BOTTOM
                )
            ), matchWrap())

            root.addView(segmentGroupCard(
                title = "Untere Gesten",
                key = "bottom",
                summary = listOf(
                    "links: ${actionLabelForUi(GesturePrefs.actionFor(this, GesturePrefs.GESTURE_BOTTOM_LEFT))}",
                    "mitte: ${actionLabelForUi(GesturePrefs.actionFor(this, GesturePrefs.GESTURE_BOTTOM_CENTER))}",
                    "rechts: ${actionLabelForUi(GesturePrefs.actionFor(this, GesturePrefs.GESTURE_BOTTOM_RIGHT))}"
                ),
                entries = listOf(
                    "Unterer Rand links ↑" to GesturePrefs.GESTURE_BOTTOM_LEFT,
                    "Unterer Rand mitte ↑" to GesturePrefs.GESTURE_BOTTOM_CENTER,
                    "Unterer Rand rechts ↑" to GesturePrefs.GESTURE_BOTTOM_RIGHT
                )
            ), matchWrap())
        }

    }

    private fun buildSettingsNavigationUi(
        root: LinearLayout
    ) {
        val header =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(20),
                    dp(18),
                    dp(18),
                    dp(18)
                )

                background =
                    GradientDrawable().apply {
                        shape =
                            GradientDrawable.RECTANGLE

                        cornerRadius =
                            dp(26).toFloat()

                        setColor(
                            accentHeaderColor()
                        )
                    }
            }

        val row =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    android.view.Gravity.CENTER_VERTICAL
            }

        val titleBox =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
            }

        titleBox.addView(
            TextView(this).apply {
                text = "Einstellungen"
                textSize = 28f
                typeface =
                    Typeface.DEFAULT_BOLD

                setTextColor(
                    Color.parseColor(
                        "#EAF2FF"
                    )
                )
            }
        )

        titleBox.addView(
            TextView(this).apply {
                text =
                    "GestureKing konfigurieren"

                textSize = 14f

                setTextColor(
                    Color.parseColor(
                        "#C1D2EA"
                    )
                )
            }
        )

        row.addView(
            titleBox,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        row.addView(
            TextView(this).apply {
                text = "Zurück"
                textSize = 15f
                typeface =
                    Typeface.DEFAULT_BOLD

                setTextColor(
                    Color.parseColor(
                        "#EAF2FF"
                    )
                )

                setPadding(
                    dp(10),
                    dp(10),
                    dp(2),
                    dp(10)
                )

                isClickable = true
                isFocusable = true

                setOnClickListener {
                    settingsVisible = false
                    lastScrollY = 0

                    buildUi()
                    updateUi()
                }
            }
        )

        header.addView(row)

        root.addView(
            header,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(14)
            }
        )

        val feedbackCard =
            quietCard()

        feedbackCard.addView(
            sectionTitle("Feedback")
        )

        feedbackCard.addView(
            description(
                "Rückmeldung bei erkannten Gesten."
            )
        )

        hapticSwitch =
            switchRow(
                parent = feedbackCard,
                title = "Haptik",
                subtitle =
                    "Kurzer Tick bei einer erkannten Geste."
            ) { checked ->
                GesturePrefs.setHapticEnabled(
                    this,
                    checked
                )

                GesturePrefs.setLastEvent(
                    this,
                    if (checked) {
                        "Haptik aktiviert."
                    } else {
                        "Haptik deaktiviert."
                    }
                )
            }

        setSwitch(
            sw = hapticSwitch,
            checked =
                GesturePrefs.isHapticEnabled(this),
            enabled = true
        ) { checked ->
            GesturePrefs.setHapticEnabled(
                this,
                checked
            )

            GesturePrefs.setLastEvent(
                this,
                if (checked) {
                    "Haptik aktiviert."
                } else {
                    "Haptik deaktiviert."
                }
            )
        }

        toastSwitch =
            switchRow(
                parent = feedbackCard,
                title = "Feedback-Bubble",
                subtitle =
                    "Zeigt kurz die ausgelöste Aktion."
            ) { checked ->
                GesturePrefs.setToastEnabled(
                    this,
                    checked
                )

                GesturePrefs.setLastEvent(
                    this,
                    if (checked) {
                        "Feedback-Bubble aktiviert."
                    } else {
                        "Feedback-Bubble deaktiviert."
                    }
                )
            }

        setSwitch(
            sw = toastSwitch,
            checked =
                GesturePrefs.isToastEnabled(this),
            enabled = true
        ) { checked ->
            GesturePrefs.setToastEnabled(
                this,
                checked
            )

            GesturePrefs.setLastEvent(
                this,
                if (checked) {
                    "Feedback-Bubble aktiviert."
                } else {
                    "Feedback-Bubble deaktiviert."
                }
            )
        }

        root.addView(
            feedbackCard,
            matchWrap()
        )

        val sizeCard =
            quietCard()

        sizeCard.addView(
            sectionTitle("Breite / Höhe")
        )

        sizeCard.addView(
            description(
                "Breite der Seitenränder und Höhe der unteren Zone."
            )
        )

        val sizeRow =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL

                setPadding(
                    0,
                    dp(10),
                    0,
                    0
                )
            }

        val selectedSize =
            GesturePrefs.zoneSizeDp(this)

        listOf(
            16,
            24,
            32,
            40
        ).forEachIndexed { index, size ->
            val selected =
                size == selectedSize

            sizeRow.addView(
                Button(this).apply {
                    text = "${size}dp"
                    textSize = 13f
                    isAllCaps = false

                    applyModernButtonStyle(
                        button = this,
                        selected = selected
                    )

                    setOnClickListener {
                        GesturePrefs.setZoneSizeDp(
                            this@MainActivity,
                            size
                        )

                        GesturePrefs.setLastEvent(
                            this@MainActivity,
                            "Zonengröße gesetzt: ${size}dp"
                        )

                        restartOverlay()

                        buildUi()
                    }
                },
                LinearLayout.LayoutParams(
                    0,
                    dp(44),
                    1f
                ).apply {
                    setMargins(
                        if (index == 0) {
                            0
                        } else {
                            dp(4)
                        },
                        0,
                        if (index == 3) {
                            0
                        } else {
                            dp(4)
                        },
                        0
                    )
                }
            )
        }

        sizeCard.addView(
            sizeRow
        )

        root.addView(
            sizeCard,
            matchWrap()
        )

        val colorCard =
            quietCard()

        colorCard.addView(
            sectionTitle("Farben")
        )

        colorCard.addView(
            description(
                "Wähle die Akzentfarbe von GestureKing."
            )
        )

        listOf(
            Triple("Blau", "blue", Color.parseColor("#4E8FE8")),
            Triple("Rot", "red", Color.parseColor("#E45C61")),
            Triple("Grün", "green", Color.parseColor("#45A86B")),
            Triple("Türkis", "turquoise", Color.parseColor("#25A9AA")),
            Triple("Gelb", "yellow", Color.parseColor("#D5A800"))
        ).forEach { option ->
            colorCard.addView(
                accentColorButton(
                    title = option.first,
                    key = option.second,
                    color = option.third
                ),
                matchWrap()
            )
        }

        root.addView(
            colorCard,
            matchWrap()
        )

        val overlayGranted =
            Settings.canDrawOverlays(this)

        val accessibilityGranted =
            isAccessibilityEnabled()

        val shizukuReachable =
            GestureShizuku.isReachable()

        val shizukuGranted =
            shizukuReachable &&
                GestureShizuku.hasPermission()

        val cameraGranted =
            checkSelfPermission(
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

        val permissionCard =
            quietCard()

        permissionCard.addView(
            sectionTitle("Berechtigungen")
        )

        permissionCard.addView(
            description(
                "Erforderliche Zugriffe für Gestenzonen und Systemaktionen."
            )
        )

        permissionCard.addView(
            description(
                "Overlay: " +
                    if (overlayGranted) {
                        "Erlaubt"
                    } else {
                        "Fehlt"
                    }
            ).apply {
                setTextColor(
                    if (overlayGranted) {
                        Color.parseColor("#57C785")
                    } else {
                        Color.parseColor("#F2C94C")
                    }
                )
            }
        )

        permissionCard.addView(
            Button(this).apply {
                text = "Overlay verwalten"
                isAllCaps = false

                applyModernButtonStyle(
                    button = this,
                    selected = false
                )

                setOnClickListener {
                    openOverlaySettings()
                }
            },
            matchWrap()
        )

        permissionCard.addView(
            description(
                "Bedienungshilfe: " +
                    if (accessibilityGranted) {
                        "Aktiv"
                    } else {
                        "Nicht aktiv"
                    }
            ).apply {
                setTextColor(
                    if (accessibilityGranted) {
                        Color.parseColor("#57C785")
                    } else {
                        Color.parseColor("#F2C94C")
                    }
                )

                setPadding(
                    0,
                    dp(10),
                    0,
                    0
                )
            }
        )

        permissionCard.addView(
            Button(this).apply {
                text = "Bedienungshilfe öffnen"
                isAllCaps = false

                applyModernButtonStyle(
                    button = this,
                    selected = false
                )

                setOnClickListener {
                    startActivity(
                        Intent(
                            Settings.ACTION_ACCESSIBILITY_SETTINGS
                        )
                    )
                }
            },
            matchWrap()
        )

        permissionCard.addView(
            description(
                "Shizuku: " +
                    when {
                        shizukuGranted ->
                            "Verbunden"

                        shizukuReachable ->
                            "Freigabe fehlt"

                        else ->
                            "Nicht erreichbar"
                    }
            ).apply {
                setTextColor(
                    if (shizukuGranted) {
                        Color.parseColor("#57C785")
                    } else {
                        Color.parseColor("#F2C94C")
                    }
                )

                setPadding(
                    0,
                    dp(10),
                    0,
                    0
                )
            }
        )

        permissionCard.addView(
            Button(this).apply {
                text =
                    if (shizukuGranted) {
                        "Shizuku ist freigegeben"
                    } else {
                        "Shizuku freigeben"
                    }

                isAllCaps = false

                isEnabled =
                    shizukuReachable &&
                        !shizukuGranted

                alpha =
                    if (isEnabled) {
                        1f
                    } else {
                        0.55f
                    }

                applyModernButtonStyle(
                    button = this,
                    selected = false
                )

                setOnClickListener {
                    requestShizukuPermission()
                }
            },
            matchWrap()
        )

        permissionCard.addView(
            description(
                "Kamera / Taschenlampe: " +
                    if (cameraGranted) {
                        "Erlaubt"
                    } else {
                        "Fehlt"
                    }
            ).apply {
                setTextColor(
                    if (cameraGranted) {
                        Color.parseColor("#57C785")
                    } else {
                        Color.parseColor("#F2C94C")
                    }
                )

                setPadding(
                    0,
                    dp(10),
                    0,
                    0
                )
            }
        )

        permissionCard.addView(
            Button(this).apply {
                text =
                    if (cameraGranted) {
                        "Kamerazugriff erlaubt"
                    } else {
                        "Kamera / Taschenlampe erlauben"
                    }

                isAllCaps = false
                isEnabled = !cameraGranted

                alpha =
                    if (isEnabled) {
                        1f
                    } else {
                        0.55f
                    }

                applyModernButtonStyle(
                    button = this,
                    selected = false
                )

                setOnClickListener {
                    requestPermissions(
                        arrayOf(
                            android.Manifest.permission.CAMERA
                        ),
                        9405
                    )
                }
            },
            matchWrap()
        )

        root.addView(
            permissionCard,
            matchWrap()
        )

        root.addView(
            resetSettingsCard(),
            matchWrap()
        )

        val eventCard =
            quietCard()

        eventCard.addView(
            sectionTitle("Letztes Ereignis")
        )

        eventCard.addView(
            description(
                GesturePrefs.lastEvent(this)
            ).apply {
                textSize = 13.5f
                setTextColor(COLOR_TEXT)
            }
        )

        eventCard.addView(
            TextView(this).apply {
                text = "Rohdaten"
                textSize = 14f

                typeface =
                    Typeface.DEFAULT_BOLD

                setTextColor(COLOR_TEXT)

                setPadding(
                    0,
                    dp(12),
                    0,
                    dp(4)
                )
            }
        )

        eventCard.addView(
            description(
                GesturePrefs.lastRawGesture(this)
            ).apply {
                textSize = 13f
                setTextColor(COLOR_MUTED)
            }
        )

        root.addView(
            eventCard,
            matchWrap()
        )

        val versionCard =
            quietCard()

        versionCard.addView(
            sectionTitle("GestureKing 0.14")
        )

        versionCard.addView(
            description(
                "Neun frei belegbare Gestenzonen: " +
                    "drei rechts, drei links und drei unten."
            )
        )

        versionCard.addView(
            description(
                "Overlay und Bedienungshilfe bilden die Basis. " +
                    "Shizuku erweitert GestureKing optional um " +
                    "bestimmte Systemaktionen ohne Root."
            ).apply {
                setPadding(
                    0,
                    dp(8),
                    0,
                    0
                )
            }
        )

        root.addView(
            versionCard,
            matchWrap()
        )
    }

    private data class LaunchableApp(
        val label: String,
        val packageName: String
    )

    private fun resetSettingsCard(): LinearLayout {
        val c = card()

        c.addView(sectionTitle("Einstellungen zurücksetzen"))
        c.addView(description("Setzt Gesten, App-Ziele pro Geste, Feedback und Zonengröße zurück. Normal-/Segment-Modus und Android-Berechtigungen bleiben erhalten."))

        if (!resetConfirmArmed) {
            c.addView(Button(this).apply {
                text = "Zurücksetzen vorbereiten"
                isAllCaps = false

                applyModernButtonStyle(
                    button = this,
                    selected = false
                )

                setOnClickListener {
                    resetConfirmArmed = true
                    GesturePrefs.setLastEvent(
                        this@MainActivity,
                        "Reset vorbereitet. Tippe zur Bestätigung erneut."
                    )
                    rebuild()
                }
            }, matchWrap())
        } else {
            c.addView(description("Achtung: Alle Gesten und App-Ziele pro Geste werden geleert. Der aktuelle Modus bleibt erhalten."))

            c.addView(Button(this).apply {
                text = "Jetzt wirklich zurücksetzen"
                isAllCaps = false

                applyModernButtonStyle(
                    button = this,
                    selected = true
                )

                setOnClickListener {
                    resetConfirmArmed = false

                    stopService(Intent(this@MainActivity, GestureOverlayService::class.java))

                    expandedGesture = null
                    expandedSegmentGroup = null
                    appListExpandedSlot = 0
                    appSearchQuery = ""

                    GesturePrefs.resetUserSettingsKeepMode(this@MainActivity)

                    buildUi()
                    updateUi()
                }
            }, matchWrap())

            c.addView(Button(this).apply {
                text = "Abbrechen"
                isAllCaps = false

                applyModernButtonStyle(
                    button = this,
                    selected = false
                )

                setOnClickListener {
                    resetConfirmArmed = false
                    GesturePrefs.setLastEvent(this@MainActivity, "Reset abgebrochen.")
                    rebuild()
                }
            }, matchWrap())
        }

        return c
    }

    private fun segmentGroupCard(
        title: String,
        key: String,
        summary: List<String>,
        entries: List<Pair<String, String>>
    ): LinearLayout {
        val c = card()

        c.addView(sectionTitle(title))

        summary.forEach { line ->
            c.addView(description(line))
        }

        val isExpanded = expandedSegmentGroup == key

        val toggleButton =
            Button(this).apply {
                text =
                    if (isExpanded) {
                        "$title einklappen ▲"
                    } else {
                        "$title anzeigen ▼"
                    }

                isAllCaps = false

                applyModernButtonStyle(
                    button = this,
                    selected = isExpanded
                )

                setOnClickListener {
                    expandedSegmentGroup =
                        if (
                            expandedSegmentGroup ==
                                key
                        ) {
                            null
                        } else {
                            key
                        }

                    expandedGesture = null
                    rebuild()
                }
            }

        segmentGroupToggleButtons[key] = toggleButton
        c.addView(toggleButton, matchWrap())

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = if (isExpanded) View.VISIBLE else View.GONE
        }

        segmentGroupContainers[key] = box
        entries.forEach { entry ->
            box.addView(actionCard(entry.first, entry.second), matchWrap())
        }

        c.addView(box)

        return c
    }

    private fun updateExpandedSegmentGroups() {
        segmentGroupContainers.forEach { entry ->
            val key = entry.key
            val container = entry.value
            val expanded = expandedSegmentGroup == key

            container.visibility = if (expanded) View.VISIBLE else View.GONE

            val title = when (key) {
                "right", "normal_right" -> "Rechte Gesten"
                "left", "normal_left" -> "Linke Gesten"
                "bottom", "normal_bottom" -> "Untere Gesten"
                else -> "Gesten"
            }

            segmentGroupToggleButtons[key]?.text = if (expanded) {
                "$title einklappen ▲"
            } else {
                "$title anzeigen ▼"
            }
        }
    }

    private fun appTargetCard(): LinearLayout {
        val c = card()
        c.addView(sectionTitle("App-Ziele"))

        c.addView(description("Du kannst drei getrennte App-Ziele speichern und danach pro Geste App 1, App 2 oder App 3 auswählen."))

        for (slot in 1..3) {
            c.addView(label("App $slot"))

            val targetLabel = description("Ziel-App: ${GesturePrefs.openAppTargetLabel(this, slot)}")
            appSlotTargetLabels[slot] = targetLabel
            c.addView(targetLabel)

            val toggleButton = Button(this).apply {
                text = if (appListExpandedSlot == slot) {
                    "APP $slot LISTE AUSBLENDEN"
                } else {
                    "APP $slot AUSWÄHLEN"
                }
                isAllCaps = false
                setOnClickListener {
                    appListExpandedSlot = if (appListExpandedSlot == slot) 0 else slot
                    updateExpandedAppTargetCards()
                }
            }

            appSlotToggleButtons[slot] = toggleButton
            c.addView(toggleButton, matchWrap())

            val slotBox = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                visibility = if (appListExpandedSlot == slot) View.VISIBLE else View.GONE
            }

            appSlotContainers[slot] = slotBox

            slotBox.addView(description("Tippe eine App an, die als App $slot gespeichert werden soll. Suche filtert nach App-Name oder Paketname."))

            val searchInput = EditText(this).apply {
                hint = "App suchen, z.B. Termius, ChatGPT, Kamera…"
                setSingleLine(true)
                textSize = 14f
                setTextColor(COLOR_TEXT)
                setHintTextColor(COLOR_MUTED)
                setText(appSearchQuery)
                setPadding(dp(12), dp(8), dp(12), dp(8))
                background = roundedStroke(COLOR_CARD, COLOR_STROKE, dp(1), dp(10))
            }

            slotBox.addView(searchInput, matchWrap())

            val searchRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            searchRow.addView(Button(this).apply {
                text = "SUCHEN"
                isAllCaps = false
                setOnClickListener {
                    appSearchQuery = searchInput.text?.toString()?.trim().orEmpty()
                    rebuild()
                }
            }, buttonWeight(0, 2))

            searchRow.addView(Button(this).apply {
                text = "LEEREN"
                isAllCaps = false
                setOnClickListener {
                    appSearchQuery = ""
                    rebuild()
                }
            }, buttonWeight(1, 2))

            slotBox.addView(searchRow)

            val apps = loadLaunchableApps().filter { app ->
                appSearchQuery.isBlank() ||
                    app.label.contains(appSearchQuery, ignoreCase = true) ||
                    app.packageName.contains(appSearchQuery, ignoreCase = true)
            }

            slotBox.addView(description("${apps.size} Treffer${if (appSearchQuery.isNotBlank()) " für „$appSearchQuery“" else ""}"))

            apps.take(180).forEach { app ->
                slotBox.addView(Button(this).apply {
                    text = app.label
                    isAllCaps = false
                    setOnClickListener {
                        GesturePrefs.setOpenAppTarget(
                            this@MainActivity,
                            slot,
                            app.packageName,
                            app.label
                        )
                        GesturePrefs.setLastEvent(
                            this@MainActivity,
                            "App $slot gesetzt: ${app.label}"
                        )
                        appListExpandedSlot = 0
                        restartOverlay()
                        updateAppTargetUi(slot)
                        updateAllActionUi()
                        updateExpandedAppTargetCards()
                        updateUi()
                    }
                }, matchWrap())
            }

            if (apps.size > 180) {
                slotBox.addView(description("Noch ${apps.size - 180} weitere Apps. Bitte Suche verfeinern."))
            }

            c.addView(slotBox)
        }

        return c
    }

    private fun updateExpandedAppTargetCards() {
        appSlotContainers.forEach { entry ->
            val slot = entry.key
            val container = entry.value
            val expanded = appListExpandedSlot == slot

            container.visibility = if (expanded) View.VISIBLE else View.GONE

            appSlotToggleButtons[slot]?.text = if (expanded) {
                "APP $slot LISTE AUSBLENDEN"
            } else {
                "APP $slot AUSWÄHLEN"
            }
        }
    }

    private fun updateAppTargetUi(slot: Int) {
        val safeSlot = slot.coerceIn(1, 3)
        appSlotTargetLabels[safeSlot]?.text =
            "Ziel-App: ${GesturePrefs.openAppTargetLabel(this, safeSlot)}"
    }

    private fun updateAllActionUi() {
        actionCurrentLabels.keys.toList().forEach { gesture ->
            updateActionUi(gesture)
        }
    }

    private fun loadLaunchableApps(): List<LaunchableApp> {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        return packageManager
            .queryIntentActivities(intent, 0)
            .mapNotNull { info ->
                val label = info.loadLabel(packageManager)?.toString()?.trim().orEmpty()
                val packageName = info.activityInfo?.packageName.orEmpty()

                if (label.isBlank() || packageName.isBlank()) {
                    null
                } else {
                    LaunchableApp(label, packageName)
                }
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    private fun gestureAppTargetCard(gesture: String, title: String): LinearLayout {
        val c = card()
        c.addView(sectionTitle("Ziel-App für diese Geste"))
        c.addView(description("$title → ${GesturePrefs.openAppTargetLabel(this, gesture)}"))

        val expanded = appPickerGesture == gesture

        c.addView(Button(this).apply {
            text = if (expanded) "APP-LISTE AUSBLENDEN ▲" else "APP FÜR DIESE GESTE AUSWÄHLEN ▼"
            isAllCaps = false
            setOnClickListener {
                appPickerGesture = if (appPickerGesture == gesture) null else gesture
                rebuild()
            }
        }, matchWrap())

        if (expanded) {
            c.addView(description("Wähle eine App nur für diese eine Geste. Links/rechts/unten teilen sich danach nichts mehr."))

            val searchInput = EditText(this).apply {
                hint = "App suchen, z.B. Termius, ChatGPT, Kamera…"
                setSingleLine(true)
                textSize = 14f
                setTextColor(COLOR_TEXT)
                setHintTextColor(COLOR_MUTED)
                setText(appSearchQuery)
                setPadding(dp(12), dp(8), dp(12), dp(8))
                background = roundedStroke(COLOR_CARD, COLOR_STROKE, dp(1), dp(10))
            }

            c.addView(searchInput, matchWrap())

            val searchRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            searchRow.addView(Button(this).apply {
                text = "SUCHEN"
                isAllCaps = false
                setOnClickListener {
                    appSearchQuery = searchInput.text?.toString()?.trim().orEmpty()
                    rebuild()
                }
            }, buttonWeight(0, 2))

            searchRow.addView(Button(this).apply {
                text = "LEEREN"
                isAllCaps = false
                setOnClickListener {
                    appSearchQuery = ""
                    rebuild()
                }
            }, buttonWeight(1, 2))

            c.addView(searchRow)

            val apps = loadLaunchableApps().filter { app ->
                appSearchQuery.isBlank() ||
                    app.label.contains(appSearchQuery, ignoreCase = true) ||
                    app.packageName.contains(appSearchQuery, ignoreCase = true)
            }

            c.addView(description("${apps.size} Treffer${if (appSearchQuery.isNotBlank()) " für „$appSearchQuery“" else ""}"))

            apps.take(180).forEach { app ->
                c.addView(Button(this).apply {
                    text = app.label
                    isAllCaps = false
                    setOnClickListener {
                        GesturePrefs.setOpenAppTarget(
                            this@MainActivity,
                            gesture,
                            app.packageName,
                            app.label
                        )
                        GesturePrefs.setAction(
                            this@MainActivity,
                            gesture,
                            GesturePrefs.ACTION_OPEN_APP_1
                        )
                        GesturePrefs.setLastEvent(
                            this@MainActivity,
                            "$title öffnet jetzt: ${app.label}"
                        )

                        appPickerGesture = null
                        appSearchQuery = ""
                        restartOverlay()
                        rebuild()
                    }
                }, matchWrap())
            }

            if (apps.size > 180) {
                c.addView(description("Noch ${apps.size - 180} weitere Apps. Bitte Suche verfeinern."))
            }
        }

        return c
    }

    private fun actionCard(title: String, gesture: String): LinearLayout {
        val c = card()

        val isExpanded = expandedGesture == gesture

        c.addView(sectionTitle(title))

        val current = GesturePrefs.actionFor(this, gesture)

        val currentLabel = description("Aktuell: ${actionLabelForUi(current, gesture)}")
        actionCurrentLabels[gesture] = currentLabel
        c.addView(currentLabel)

        val toggleButton = Button(this).apply {
            text = if (isExpanded) "AKTIONEN EINKLAPPEN ▲" else "AKTION ÄNDERN ▼"
            isAllCaps = false
            setOnClickListener {
                expandedGesture = if (expandedGesture == gesture) null else gesture
                updateExpandedActionCards()
            }
        }

        actionToggleButtons[gesture] = toggleButton
        c.addView(toggleButton, matchWrap())

        val actionsBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = if (isExpanded) View.VISIBLE else View.GONE
        }

        actionOptionContainers[gesture] = actionsBox

        actionsBox.addView(gestureAppTargetCard(gesture, title), matchWrap())

        val row1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(4), 0, 0)
        }

        row1.addView(actionButton("Zurück", gesture, GesturePrefs.ACTION_BACK), buttonWeight(0, 3))
        row1.addView(actionButton("Home", gesture, GesturePrefs.ACTION_HOME), buttonWeight(1, 3))
        row1.addView(actionButton("Letzte", gesture, GesturePrefs.ACTION_RECENTS), buttonWeight(2, 3))

        actionsBox.addView(row1)

        val row2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, 0)
        }

        row2.addView(actionButton("Benachr.", gesture, GesturePrefs.ACTION_NOTIFICATIONS), buttonWeight(0, 3))
        row2.addView(actionButton("Quick", gesture, GesturePrefs.ACTION_QUICK_SETTINGS), buttonWeight(1, 3))
        row2.addView(actionButton("Keine", gesture, GesturePrefs.ACTION_NONE), buttonWeight(2, 3))

        actionsBox.addView(row2)

        val row3 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, 0)
        }

        row3.addView(actionButton("Bild aus", gesture, GesturePrefs.ACTION_SCREEN_OFF), buttonWeight(0, 3))
        row3.addView(actionButton("Rotation", gesture, GesturePrefs.ACTION_ROTATION_TOGGLE), buttonWeight(1, 3))
        row3.addView(actionButton("Licht", gesture, GesturePrefs.ACTION_FLASHLIGHT_TOGGLE), buttonWeight(2, 3))

        actionsBox.addView(row3)

        val row4 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, 0)
        }

        row4.addView(actionButton("App öffnen", gesture, GesturePrefs.ACTION_OPEN_APP_1), buttonWeight(0, 3))
        row4.addView(description(""), buttonWeight(1, 3))
        row4.addView(description(""), buttonWeight(2, 3))

        actionsBox.addView(row4)

        val row5 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, 0)
        }

        row5.addView(actionButton("Browser vor", gesture, GesturePrefs.ACTION_BROWSER_FORWARD), buttonWeight(0, 3))
        row5.addView(description(""), buttonWeight(1, 3))
        row5.addView(description(""), buttonWeight(2, 3))

        actionsBox.addView(row5)

        c.addView(actionsBox)

        return c
    }

    private fun actionButton(label: String, gesture: String, action: String): Button {
        val selected = GesturePrefs.actionFor(this, gesture) == action

        val button = Button(this).apply {
            text = if (selected) "✓ $label" else label
            tag = ActionButtonTag(gesture, action, label)
            isAllCaps = false
            textSize = 12f
            setOnClickListener {
                GesturePrefs.setAction(this@MainActivity, gesture, action)
                GesturePrefs.setLastEvent(
                    this@MainActivity,
                    "Aktion gesetzt: $gesture = ${actionLabelForUi(action, gesture)}"
                )

                if (GesturePrefs.isOpenAppAction(action)) {
                    expandedGesture = gesture
                    appPickerGesture = gesture
                    rebuild()
                } else {
                    expandedGesture = null
                    appPickerGesture = null
                    restartOverlay()
                    updateActionUi(gesture)
                    updateExpandedActionCards()
                    updateUi()
                }
            }
        }

        actionButtonsByGesture.getOrPut(gesture) { mutableListOf() }.add(button)

        return button
    }

    private data class ActionButtonTag(
        val gesture: String,
        val action: String,
        val label: String
    )

    private fun updateExpandedActionCards() {
        actionOptionContainers.forEach { entry ->
            val gesture = entry.key
            val container = entry.value
            val expanded = expandedGesture == gesture

            container.visibility = if (expanded) View.VISIBLE else View.GONE

            actionToggleButtons[gesture]?.text = if (expanded) {
                "AKTIONEN EINKLAPPEN ▲"
            } else {
                "AKTION ÄNDERN ▼"
            }
        }
    }

    private fun updateActionUi(gesture: String) {
        val selectedAction = GesturePrefs.actionFor(this, gesture)

        actionCurrentLabels[gesture]?.text = "Aktuell: ${actionLabelForUi(selectedAction, gesture)}"

        actionButtonsByGesture[gesture]?.forEach { button ->
            val tag = button.tag as? ActionButtonTag ?: return@forEach
            button.text = if (tag.action == selectedAction) {
                "✓ ${tag.label}"
            } else {
                tag.label
            }
        }
    }

    private fun actionLabelForUi(action: String, gesture: String? = null): String {
        return if (GesturePrefs.isOpenAppAction(action)) {
            if (gesture != null) {
                "App öffnen: ${GesturePrefs.openAppTargetLabel(this, gesture)}"
            } else {
                "App öffnen"
            }
        } else {
            GesturePrefs.actionLabel(action)
        }
    }



    private fun collapseOpenMenusForBack(): Boolean {
        if (settingsVisible) {
            settingsVisible = false
            lastScrollY = 0

            buildUi()
            updateUi()

            return true
        }

        val visibleActionMenu = actionOptionContainers.values.any { it.visibility == View.VISIBLE }
        val visibleGestureGroup = segmentGroupContainers.values.any { it.visibility == View.VISIBLE }
        val visibleAppPicker = appSlotContainers.values.any { it.visibility == View.VISIBLE }

        val hadOpenMenu =
            expandedGesture != null ||
            expandedSegmentGroup != null ||
            appPickerGesture != null ||
            feedbackExpanded ||
            maintenanceExpanded ||
            resetConfirmArmed ||
            visibleActionMenu ||
            visibleGestureGroup ||
            visibleAppPicker

        if (!hadOpenMenu) return false

        expandedGesture = null
        expandedSegmentGroup = null
        appPickerGesture = null
        appListExpandedSlot = 0
        feedbackExpanded = false
        maintenanceExpanded = false
        resetConfirmArmed = false

        GesturePrefs.setLastEvent(
            this,
            "Offene Menüs per Zurück-Taste eingeklappt."
        )

        rebuild()
        return true
    }

    private fun setupNativeBackCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                OnBackInvokedCallback {
                    if (!collapseOpenMenusForBack()) {
                        finish()
                    }
                }
            )
        }
    }

    override fun onBackPressed() {
        if (!collapseOpenMenusForBack()) {
            super.onBackPressed()
        }
    }

    private fun rememberScrollPosition() {
        if (::rootScroll.isInitialized) {
            lastScrollY = rootScroll.scrollY
        }
    }

    private fun rebuild() {
        rememberScrollPosition()
        rebuildKeepingScroll()
    }

    private fun rebuildAndScrollToSegmentAnchor() {
        buildUi()
        updateUi()

        rootScroll.post {
            val targetY = (segmentAnchorView?.top ?: 0) - dp(12)
            rootScroll.scrollTo(0, targetY.coerceAtLeast(0))
        }

        rootScroll.postDelayed({
            val targetY = (segmentAnchorView?.top ?: 0) - dp(12)
            rootScroll.scrollTo(0, targetY.coerceAtLeast(0))
        }, 80L)
    }

    private fun rebuildKeepingScroll() {
        val targetY = lastScrollY

        buildUi()
        updateUi()

        restoreScrollPosition(targetY)
    }

    private fun restoreScrollPosition(targetY: Int) {
        if (!::rootScroll.isInitialized) return

        rootScroll.post {
            rootScroll.scrollTo(0, targetY)
        }

        rootScroll.postDelayed({
            rootScroll.scrollTo(0, targetY)
        }, 80L)

        rootScroll.postDelayed({
            rootScroll.scrollTo(0, targetY)
        }, 180L)
    }

    private fun updateUi() {
        if (settingsVisible) {
            return
        }

        val overlay = Settings.canDrawOverlays(this)
        val accessibility = isAccessibilityEnabled()
        val permissionsOk = overlay && accessibility

        setSwitch(masterSwitch, GesturePrefs.isEnabled(this), permissionsOk) { checked ->
            GesturePrefs.setEnabled(this, checked)
            restartOverlay()
            GesturePrefs.setLastEvent(this, if (checked) "GestureKing aktiviert." else "GestureKing deaktiviert.")
            updateUi()
        }

        setSwitch(rightSwitch, GesturePrefs.isRightEnabled(this), permissionsOk) { checked ->
            GesturePrefs.setRightEnabled(this, checked)
            restartOverlay()
            updateUi()
        }

        setSwitch(leftSwitch, GesturePrefs.isLeftEnabled(this), permissionsOk) { checked ->
            GesturePrefs.setLeftEnabled(this, checked)
            restartOverlay()
            updateUi()
        }

        setSwitch(bottomSwitch, GesturePrefs.isBottomEnabled(this), permissionsOk) { checked ->
            GesturePrefs.setBottomEnabled(this, checked)
            restartOverlay()
            updateUi()
        }

        setSwitch(visibleSwitch, GesturePrefs.areZonesVisible(this), permissionsOk) { checked ->
            GesturePrefs.setZonesVisible(this, checked)
            restartOverlay()
            updateUi()
        }

        setSwitch(segmentedSwitch, GesturePrefs.isSegmentModeEnabled(this), permissionsOk) { checked ->
            GesturePrefs.setSegmentModeEnabled(this, checked)
            GesturePrefs.setLastEvent(
                this,
                if (checked) "Segment-Modus aktiviert." else "Segment-Modus deaktiviert."
            )
            rebuildAndScrollToSegmentAnchor()
            rootScroll.postDelayed({
                restartOverlay()
            }, 120L)
        }

        val rightsOk =
            overlay && accessibility

        val serviceEnabled =
            GesturePrefs.isEnabled(
                this
            )

        val shizukuReachable =
            GestureShizuku.isReachable()

        val shizukuGranted =
            shizukuReachable &&
                GestureShizuku.hasPermission()

        val modeText =
            if (
                GesturePrefs
                    .isSegmentModeEnabled(this)
            ) {
                "Segment"
            } else {
                "Normal"
            }

        val activeZones =
            mutableListOf<String>().apply {
                if (
                    GesturePrefs
                        .isLeftEnabled(
                            this@MainActivity
                        )
                ) {
                    add("Links")
                }

                if (
                    GesturePrefs
                        .isRightEnabled(
                            this@MainActivity
                        )
                ) {
                    add("Rechts")
                }

                if (
                    GesturePrefs
                        .isBottomEnabled(
                            this@MainActivity
                        )
                ) {
                    add("Unten")
                }
            }

        when {
            !rightsOk -> {
                statusDot.setTextColor(
                    Color.parseColor(
                        "#E85D5D"
                    )
                )

                statusText.text =
                    "Einrichtung erforderlich"

                statusSubtitleText.text =
                    "Berechtigungen müssen geprüft werden."
            }

            serviceEnabled -> {
                statusDot.setTextColor(
                    Color.parseColor(
                        "#57C785"
                    )
                )

                statusText.text =
                    "GestureKing aktiv"

                statusSubtitleText.text =
                    "Randgesten werden überwacht."
            }

            else -> {
                statusDot.setTextColor(
                    Color.parseColor(
                        "#F2C94C"
                    )
                )

                statusText.text =
                    "GestureKing pausiert"

                statusSubtitleText.text =
                    "Randgesten sind derzeit ausgeschaltet."
            }
        }

        masterTitleText.text =
            if (serviceEnabled) {
                "GestureKing aktiv"
            } else {
                "GestureKing pausiert"
            }

        statusRightsText.text =
            if (rightsOk) {
                "Bereit"
            } else {
                "Prüfen"
            }

        statusShizukuText.text =
            when {
                shizukuGranted ->
                    "Verbunden"

                shizukuReachable ->
                    "Freigabe fehlt"

                else ->
                    "Nicht erreichbar"
            }

        statusModeText.text =
            modeText

        statusZonesText.text =
            if (activeZones.isEmpty()) {
                "0/3 aktiv"
            } else {
                "${activeZones.size}/3 · " +
                    activeZones.joinToString(
                        separator = ", "
                    )
            }

        statusDisplayText.text =
            buildString {
                append(
                    if (
                        GesturePrefs
                            .areZonesVisible(this@MainActivity)
                    ) {
                        "Sichtbar"
                    } else {
                        "Unsichtbar"
                    }
                )

                append(" · ")

                append(
                    GesturePrefs.zoneSizeDp(
                        this@MainActivity
                    )
                )

                append(" dp")
            }

        statusFeedbackText.text =
            buildList {
                if (
                    GesturePrefs
                        .isHapticEnabled(
                            this@MainActivity
                        )
                ) {
                    add("Haptik")
                }

                if (
                    GesturePrefs
                        .isToastEnabled(
                            this@MainActivity
                        )
                ) {
                    add("Bubble")
                }
            }.ifEmpty {
                listOf("Aus")
            }.joinToString(
                separator = " · "
            )

    }

    private fun statusValueRow(
        parent: LinearLayout,
        title: String
    ): TextView {
        val row =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    android.view.Gravity.CENTER_VERTICAL

                setPadding(
                    0,
                    dp(4),
                    0,
                    dp(4)
                )
            }

        row.addView(
            TextView(this).apply {
                text = title
                textSize = 13.5f
                setTextColor(COLOR_MUTED)
            },
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        val valueView =
            TextView(this).apply {
                text = "—"
                textSize = 13.5f

                typeface =
                    Typeface.DEFAULT_BOLD

                setTextColor(COLOR_TEXT)

                gravity =
                    android.view.Gravity.END
            }

        row.addView(
            valueView
        )

        parent.addView(
            row
        )

        return valueView
    }

    private fun switchRowWithTitle(
        parent: LinearLayout,
        title: String,
        subtitle: String,
        onChanged: (Boolean) -> Unit
    ): Pair<TextView, Switch> {
        val row =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    android.view.Gravity.CENTER_VERTICAL

                setPadding(
                    0,
                    dp(6),
                    0,
                    dp(6)
                )
            }

        val textBox =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL

                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
            }

        val titleView =
            label(title)

        textBox.addView(
            titleView
        )

        textBox.addView(
            description(subtitle)
        )

        val switch =
            Switch(this).apply {
                setOnCheckedChangeListener {
                        _,
                        checked ->
                    onChanged(checked)
                }
            }

        row.addView(
            textBox
        )

        row.addView(
            switch
        )

        parent.addView(
            row
        )

        return titleView to switch
    }

    private fun switchRow(
        parent: LinearLayout,
        title: String,
        subtitle: String,
        onChanged: (Boolean) -> Unit
    ): Switch {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(6), 0, dp(6))
        }

        val textBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        textBox.addView(label(title))
        textBox.addView(description(subtitle))

        val sw = Switch(this)
        sw.setOnCheckedChangeListener { _, checked -> onChanged(checked) }

        row.addView(textBox)
        row.addView(sw)

        parent.addView(row)
        return sw
    }

    private fun setSwitch(
        sw: Switch,
        checked: Boolean,
        enabled: Boolean,
        onChanged: (Boolean) -> Unit
    ) {
        sw.setOnCheckedChangeListener(null)
        sw.isChecked = checked
        sw.isEnabled = enabled
        applyAccentSwitchStyle(sw)
        sw.setOnCheckedChangeListener { _, value -> onChanged(value) }
    }

    private fun restartOverlay() {
        stopService(Intent(this, GestureOverlayService::class.java))

        if (GesturePrefs.isEnabled(this)) {
            startService(Intent(this, GestureOverlayService::class.java))
        }
    }

    private fun activeZonesText(): String {
        val list = mutableListOf<String>()

        if (GesturePrefs.isLeftEnabled(this)) list += "links"
        if (GesturePrefs.isRightEnabled(this)) list += "rechts"
        if (GesturePrefs.isBottomEnabled(this)) list += "unten"

        val text = list.ifEmpty { listOf("keine") }.joinToString(", ")
        return if (GesturePrefs.isSegmentModeEnabled(this)) {
            "$text · Segment-Modus"
        } else {
            text
        }
    }

    private fun requestShizukuPermission() {
        try {
            if (!GestureShizuku.isReachable()) {
                GesturePrefs.setLastEvent(this, "Shizuku ist nicht erreichbar.")
                updateUi()
                return
            }

            if (GestureShizuku.hasPermission()) {
                GesturePrefs.setLastEvent(this, "Shizuku-Berechtigung ist bereits erteilt.")
                updateUi()
                return
            }

            Shizuku.requestPermission(shizukuPermissionRequestCode)
        } catch (t: Throwable) {
            GesturePrefs.setLastEvent(this, "Shizuku-Anfrage fehlgeschlagen: ${t.javaClass.simpleName}")
            updateUi()
        }
    }

    private fun openOverlaySettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    private fun isAccessibilityEnabled(): Boolean {
        val expected = ComponentName(this, GestureAccessibilityService::class.java)
        val flat = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return flat.split(":").any {
            val component = ComponentName.unflattenFromString(it)
            component != null &&
                TextUtils.equals(component.packageName, expected.packageName) &&
                TextUtils.equals(component.className, expected.className)
        }
    }

    private fun accentSwitchColor(): Int {
        return when (accentColorKey) {
            "red" ->
                Color.parseColor("#E45C61")

            "green" ->
                Color.parseColor("#45A86B")

            "turquoise" ->
                Color.parseColor("#25A9AA")

            "yellow" ->
                Color.parseColor("#D5A800")

            else ->
                Color.parseColor("#4E8FE8")
        }
    }

    private fun applyAccentSwitchStyle(
        sw: Switch
    ) {
        val checkedColor =
            accentSwitchColor()

        val checkedTrackColor =
            Color.argb(
                145,
                Color.red(checkedColor),
                Color.green(checkedColor),
                Color.blue(checkedColor)
            )

        val states =
            arrayOf(
                intArrayOf(
                    android.R.attr.state_checked
                ),
                intArrayOf()
            )

        sw.thumbTintList =
            android.content.res.ColorStateList(
                states,
                intArrayOf(
                    checkedColor,
                    Color.parseColor("#C7C9CC")
                )
            )

        sw.trackTintList =
            android.content.res.ColorStateList(
                states,
                intArrayOf(
                    checkedTrackColor,
                    Color.parseColor("#5B626B")
                )
            )
    }

    private fun applyAccentCardStyle(
        card: LinearLayout
    ) {
        card.background =
            GradientDrawable().apply {
                shape =
                    GradientDrawable.RECTANGLE

                cornerRadius =
                    dp(20).toFloat()

                setColor(COLOR_CARD)

                setStroke(
                    dp(1),
                    accentSwitchColor()
                )
            }
    }

    private fun quietCard(): LinearLayout {
        return LinearLayout(this).apply {
            orientation =
                LinearLayout.VERTICAL

            setPadding(
                dp(17),
                dp(16),
                dp(17),
                dp(16)
            )

            background =
                GradientDrawable().apply {
                    shape =
                        GradientDrawable.RECTANGLE

                    cornerRadius =
                        dp(20).toFloat()

                    setColor(COLOR_CARD)
                }
        }
    }

    private fun card(): LinearLayout {
        return LinearLayout(this).apply {
            orientation =
                LinearLayout.VERTICAL

            setPadding(
                dp(17),
                dp(16),
                dp(17),
                dp(16)
            )

            background =
                GradientDrawable().apply {
                    shape =
                        GradientDrawable.RECTANGLE

                    cornerRadius =
                        dp(20).toFloat()

                    setColor(COLOR_CARD)

                    setStroke(
                        dp(1),
                        accentSwitchColor()
                    )
                }
        }
    }

    private fun sectionTitle(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(COLOR_TEXT)
            setPadding(0, 0, 0, dp(6))
        }
    }

    private fun label(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(COLOR_TEXT)
        }
    }

    private fun description(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 13f
            setTextColor(COLOR_MUTED)
        }
    }

    private fun accentColorButton(
        title: String,
        key: String,
        color: Int
    ): Button {
        val selected =
            accentColorKey == key

        return Button(this).apply {
            text =
                if (selected) {
                    "●  $title · Aktiv"
                } else {
                    "●  $title"
                }

            textSize = 14f
            isAllCaps = false

            background =
                roundedStroke(
                    color =
                        if (selected) {
                            color
                        } else {
                            Color.parseColor("#202833")
                        },
                    strokeColor = color,
                    strokeWidth =
                        dp(
                            if (selected) {
                                2
                            } else {
                                1
                            }
                        ),
                    radius = dp(12)
                )

            setTextColor(
                when {
                    selected && key == "yellow" ->
                        Color.parseColor("#302700")

                    selected ->
                        Color.WHITE

                    else ->
                        color
                }
            )

            setPadding(
                dp(12),
                dp(8),
                dp(12),
                dp(8)
            )

            setOnClickListener {
                accentColorKey = key

                getSharedPreferences(
                    ACCENT_PREFS_NAME,
                    android.content.Context.MODE_PRIVATE
                ).edit()
                    .putString(
                        ACCENT_PREF_KEY,
                        key
                    )
                    .apply()

                GesturePrefs.setLastEvent(
                    this@MainActivity,
                    "Akzentfarbe gesetzt: $title"
                )

                buildUi()
            }
        }
    }

    private fun accentHeaderColor(): Int {
        return when (accentColorKey) {
            "red" ->
                Color.parseColor("#71313A")

            "green" ->
                Color.parseColor("#245C38")

            "turquoise" ->
                Color.parseColor("#155B5C")

            "yellow" ->
                Color.parseColor("#66540B")

            else ->
                Color.parseColor("#17477F")
        }
    }

    private fun accentSelectedBorderColor(): Int {
        return when (accentColorKey) {
            "red" ->
                Color.parseColor("#FFB3B5")

            "green" ->
                Color.parseColor("#82D99E")

            "turquoise" ->
                Color.parseColor("#74D7D8")

            "yellow" ->
                Color.parseColor("#FFD85A")

            else ->
                Color.parseColor("#8DB8FF")
        }
    }

    private fun accentSelectedTextColor(): Int {
        return when (accentColorKey) {
            "red" ->
                Color.parseColor("#FFDADC")

            "green" ->
                Color.parseColor("#C5F2D0")

            "turquoise" ->
                Color.parseColor("#C6F0F0")

            "yellow" ->
                Color.parseColor("#FFEFAF")

            else ->
                Color.parseColor("#D6E4FF")
        }
    }

    private fun applyModernButtonStyle(
        button: Button,
        selected: Boolean = false
    ) {
        val backgroundColor =
            if (selected) {
                accentHeaderColor()
            } else {
                Color.parseColor(
                    "#202833"
                )
            }

        val borderColor =
            if (selected) {
                accentSelectedBorderColor()
            } else {
                Color.parseColor(
                    "#3B4654"
                )
            }

        button.background =
            roundedStroke(
                color = backgroundColor,
                strokeColor = borderColor,
                strokeWidth = dp(1),
                radius = dp(12)
            )

        button.setTextColor(
            if (selected) {
                accentSelectedTextColor()
            } else {
                COLOR_TEXT
            }
        )

        button.setPadding(
            dp(10),
            dp(7),
            dp(10),
            dp(7)
        )
    }

    private fun roundedStroke(color: Int, strokeColor: Int, strokeWidth: Int, radius: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.toFloat()
            setStroke(strokeWidth, strokeColor)
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun matchWrap(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 0, 0, dp(10))
        }
    }

    private fun buttonWeight(index: Int, count: Int): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(0, dp(42), 1f).apply {
            val left = if (index == 0) 0 else dp(4)
            val right = if (index == count - 1) 0 else dp(4)
            setMargins(left, 0, right, 0)
        }
    }

    companion object {
        private const val ACCENT_PREFS_NAME =
            "gestureking_ui"

        private const val ACCENT_PREF_KEY =
            "accent_color"

        private const val COLOR_BG = -16117473
        private const val COLOR_CARD = -15525343
        private const val COLOR_TEXT = -1118482
        private const val COLOR_MUTED = -5000269
        private const val COLOR_STROKE = -11166875
    }
}
