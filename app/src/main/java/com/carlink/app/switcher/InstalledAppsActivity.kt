package com.carlink.app.switcher

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.carlink.app.R
import com.carlink.app.ui.settings.LaunchableApps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InstalledAppsActivity : ComponentActivity() {
    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val apps by produceState<List<LaunchableApps.App>?>(null) {
                value = withContext(Dispatchers.IO) {
                    LaunchableApps.sorted(LaunchableApps.list(packageManager), packageName, emptySet())
                }
            }
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.systemBarsPadding().padding(16.dp)) {
                        Text(stringResource(R.string.carlink_cs75_nav_apps), style = MaterialTheme.typography.titleLarge)
                        val loaded = apps
                        if (loaded == null) {
                            CircularProgressIndicator()
                        } else {
                            LazyVerticalGrid(columns = GridCells.Adaptive(112.dp)) {
                                items(loaded, key = { it.packageName }) { app ->
                                    Column(
                                        Modifier.fillMaxWidth()
                                            .combinedClickable(
                                                onClick = { openApp(app.packageName) },
                                                onLongClick = { openAppInfo(app.packageName) },
                                            )
                                            .padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        app.icon?.let { Image(it.asImageBitmap(), null, Modifier.size(48.dp)) }
                                        Text(app.label, textAlign = TextAlign.Center)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun openAppInfo(pkg: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", pkg, null))
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.carlink_cs75_nav_apps_unavailable, Toast.LENGTH_SHORT).show()
        } catch (_: SecurityException) {
            Toast.makeText(this, R.string.carlink_cs75_nav_apps_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    private fun openApp(pkg: String) {
        try {
            val target = LaunchableApps.launchIntent(packageManager, pkg)
            if (target != null) {
                startActivity(target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                finish()
                return
            }
        } catch (_: ActivityNotFoundException) {
            // The app may have been removed since the list was loaded.
        } catch (_: SecurityException) {
            // Some system activities restrict access to their own launcher.
        }
        Toast.makeText(this, R.string.carlink_cs75_nav_apps_unavailable, Toast.LENGTH_SHORT).show()
    }
}
