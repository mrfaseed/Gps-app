package com.example.gpsmapcameraapp.models

enum class AspectRatioMode(val id: Int, val label: String, val displayName: String) {
    RATIO_1_1(1, "1:1", "1:1 (Square)"),
    RATIO_4_3(2, "4:3", "4:3 (Standard)"),
    RATIO_16_9(3, "16:9", "16:9 (Widescreen)"),
    RATIO_FULL(4, "Full", "Full Screen");

    companion object {
        fun fromLabel(label: String?): AspectRatioMode {
            return entries.firstOrNull { it.label.equals(label, ignoreCase = true) } ?: RATIO_16_9
        }
    }
}

enum class CameraTimerMode(val label: String, val seconds: Int) {
    OFF("Off", 0),
    TIMER_3S("3s", 3),
    TIMER_5S("5s", 5),
    TIMER_10S("10s", 10);

    companion object {
        fun fromLabel(label: String?): CameraTimerMode {
            return when (label?.trim()) {
                "3s", "Timer 3sec" -> TIMER_3S
                "5s", "Timer 5sec" -> TIMER_5S
                "10s", "Timer 10sec" -> TIMER_10S
                else -> OFF
            }
        }
    }
}

