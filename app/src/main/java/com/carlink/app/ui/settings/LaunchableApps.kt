package com.carlink.app.ui.settings

import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import androidx.core.graphics.drawable.toBitmap

/**
 * Launchable apps for the CS75 status-bar hide picker: user and system.
 *
 * The earlier picker called [PackageManager.queryIntentActivities] with
 * [PackageManager.MATCH_DEFAULT_ONLY]. Launcher activities often declare MAIN/LAUNCHER but
 * not CATEGORY_DEFAULT, so the car only showed a handful (Settings, FceLauncher, Radio,
 * CarLink). This lists MAIN+LAUNCHER (and leanback / car-launcher) with no DEFAULT filter,
 * then unions packages that have a launch intent.
 */
object LaunchableApps {

    /** Do not pass [PackageManager.MATCH_DEFAULT_ONLY]: that is what hid most car apps. */
    const val QUERY_FLAGS = 0

    private val LAUNCH_CATEGORIES = listOf(
        Intent.CATEGORY_LAUNCHER,
        Intent.CATEGORY_LEANBACK_LAUNCHER,
        "android.intent.category.CAR_LAUNCHER",
    )

    data class App(val packageName: String, val label: String, val icon: android.graphics.Bitmap?)

    fun list(pm: PackageManager): List<App> {
        val byPackage = LinkedHashMap<String, App>()
        fun add(pkg: String, label: String, icon: android.graphics.Bitmap?) {
            if (pkg.isBlank()) return
            val existing = byPackage[pkg]
            if (existing == null) {
                byPackage[pkg] = App(pkg, label.ifBlank { pkg }, icon)
            } else if (existing.icon == null && icon != null) {
                byPackage[pkg] = existing.copy(icon = icon)
            }
        }

        for (category in LAUNCH_CATEGORIES) {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(category)
            for (info in resolveActivities(pm, intent)) {
                val pkg = info.activityInfo?.packageName ?: continue
                val label = runCatching { info.loadLabel(pm).toString() }.getOrDefault(pkg)
                val icon = runCatching { info.loadIcon(pm).toBitmap(96, 96) }.getOrNull()
                add(pkg, label, icon)
            }
        }

        for (pkg in installedPackageNames(pm)) {
            if (pkg in byPackage) continue
            val launch = runCatching { pm.getLaunchIntentForPackage(pkg) }.getOrNull() ?: continue
            if (launch.component == null && launch.`package` == null) continue
            val label = runCatching {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                pm.getApplicationLabel(appInfo).toString()
            }.getOrDefault(pkg)
            val icon = runCatching { pm.getApplicationIcon(pkg).toBitmap(96, 96) }.getOrNull()
            add(pkg, label, icon)
        }
        return byPackage.values.toList()
    }

    fun launchIntent(pm: PackageManager, pkg: String): Intent? {
        pm.getLaunchIntentForPackage(pkg)?.let { return it }
        for (category in LAUNCH_CATEGORIES) {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(category).setPackage(pkg)
            val activity = resolveActivities(pm, intent).firstOrNull()?.activityInfo ?: continue
            return intent.setClassName(activity.packageName, activity.name)
        }
        return null
    }

    fun sorted(apps: List<App>, ownPackage: String, selected: Set<String>): List<App> =
        apps.sortedWith(
            compareByDescending<App> { it.packageName == ownPackage }
                .thenByDescending { it.packageName in selected }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.label },
        )

    fun matchesSearch(app: App, query: String): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return true
        return app.label.contains(q, ignoreCase = true) || app.packageName.contains(q, ignoreCase = true)
    }

    private fun resolveActivities(pm: PackageManager, intent: Intent): List<ResolveInfo> {
        return runCatching {
            if (Build.VERSION.SDK_INT >= 33) {
                pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(QUERY_FLAGS.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(intent, QUERY_FLAGS)
            }
        }.getOrDefault(emptyList())
    }

    private fun installedPackageNames(pm: PackageManager): List<String> {
        val packages: List<PackageInfo> = runCatching {
            if (Build.VERSION.SDK_INT >= 33) {
                pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledPackages(0)
            }
        }.getOrDefault(emptyList())
        return packages.mapNotNull { it.packageName }
    }
}
