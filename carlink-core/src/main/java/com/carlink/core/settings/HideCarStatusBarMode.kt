package com.carlink.core.settings

/**
 * How CarLink hides the Changan CS75 Plus floating FceSystemUI status bar.
 * Off: do not hide. [ALL_EXCEPT_LAUNCHER]: hide in every app except the car's home screen.
 * [SELECTED_APPS]: hide only in the packages the driver ticks, plus CarLink itself.
 */
enum class HideCarStatusBarMode {
    OFF,
    ALL_EXCEPT_LAUNCHER,
    SELECTED_APPS,
}
