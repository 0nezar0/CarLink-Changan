package com.carlink.app.ui

import android.content.Context

/**
 * First-launch question: whether the car is a Changan CS75 Plus 2023.
 * Shown once only, gated on the stored answer flag (`cs75_first_launch_prompt_done`), so it
 * never repeats. Shown on a fresh install and after the app's data was cleared; never on an
 * update of a build already in use, or when a car brand is already chosen.
 */
object Cs75FirstLaunchPrompt {
    /**
     * CarLink v1.7.7: [freshInstall] alone missed clearing the app's data: the package times only
     * match on a first install, so a build updated over an older one never asked after a data clear.
     * An empty data folder ([priorUse] false: no finished setup, no known phone) counts as fresh too.
     */
    fun shouldShow(freshInstall: Boolean, priorUse: Boolean, brandChosen: Boolean, alreadyAnswered: Boolean): Boolean =
        (freshInstall || !priorUse) && !brandChosen && !alreadyAnswered

    /** Yes turns the car status-bar hide on; a hide mode the driver already chose is not replaced. */
    fun hideModeAfterYes(current: com.carlink.core.settings.HideCarStatusBarMode): com.carlink.core.settings.HideCarStatusBarMode =
        if (current == com.carlink.core.settings.HideCarStatusBarMode.OFF) com.carlink.core.settings.HideCarStatusBarMode.ALL_EXCEPT_LAUNCHER
        else current

    fun isFreshInstall(firstInstallTime: Long, lastUpdateTime: Long): Boolean =
        firstInstallTime == lastUpdateTime

    fun isFreshInstall(context: Context): Boolean = runCatching {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        isFreshInstall(info.firstInstallTime, info.lastUpdateTime)
    }.getOrDefault(false)
}
