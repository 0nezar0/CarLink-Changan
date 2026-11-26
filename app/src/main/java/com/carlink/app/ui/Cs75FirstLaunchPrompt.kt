package com.carlink.app.ui

import android.content.Context

/**
 * First-launch question after a fresh install: whether the car is a Changan CS75 Plus.
 * Shown once only. Never shown on an update (including users migrated from generic Changan),
 * or when a car brand is already chosen.
 */
object Cs75FirstLaunchPrompt {
    fun shouldShow(freshInstall: Boolean, brandChosen: Boolean, alreadyAnswered: Boolean): Boolean =
        freshInstall && !brandChosen && !alreadyAnswered

    fun isFreshInstall(firstInstallTime: Long, lastUpdateTime: Long): Boolean =
        firstInstallTime == lastUpdateTime

    fun isFreshInstall(context: Context): Boolean = runCatching {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        isFreshInstall(info.firstInstallTime, info.lastUpdateTime)
    }.getOrDefault(false)
}
