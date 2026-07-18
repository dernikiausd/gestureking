# GestureKing

GestureKing is an Android app for configurable edge gestures and quick actions.

The app provides gesture zones along the left, right and bottom edges of the display. Each edge can work as one full gesture area or be divided into three independent segments.

## Features

- Left, right and bottom edge gestures
- Normal mode with short swipe, long swipe and double tap
- Segment mode with three independently configurable areas per edge
- Nine separate gesture actions in segment mode
- Configurable app launches
- System actions through Android accessibility services
- Optional Shizuku integration for additional system actions
- Adjustable gesture-zone size
- Optional visible gesture zones for testing
- Haptic and visual feedback
- Five selectable accent colors
- Dark interface

## Requirements

- Android 8.0 or newer
- Display-over-other-apps permission
- Accessibility service permission
- Optional Shizuku installation and permission
- Camera permission only when flashlight control is used

## Permissions

- **Display over other apps:** Creates the gesture areas at the screen edges.
- **Accessibility service:** Performs actions such as Back, Home, Recents, notifications and Quick Settings.
- **Shizuku:** Optional access for selected system actions without root.
- **Camera:** Required by Android for flashlight access on some devices.
- **Vibration:** Provides optional haptic feedback.

GestureKing does not require root access.

## Build

```bash
./gradlew assembleDebug
