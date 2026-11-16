package com.carlink.app.ui.premium

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.carlink.app.R
import com.carlink.app.ui.settings.RoundGlassButton
import com.carlink.app.ui.theme.CarLinkTokens
import com.carlink.app.ui.theme.UiProfile
import com.carlink.app.ui.theme.WidthClass

/**
 * Settings › Display › Screen margins › Top, in pixels. Factory notification bars on some head
 * units sit outside [WindowInsets.safeDrawing]; chrome uses the larger of that inset and this value.
 * Changing the setting recomposes every screen that reads it.
 */
val LocalScreenMarginTopPx = compositionLocalOf { 0 }

/**
 * System safe-drawing insets, with [LocalScreenMarginTopPx] as a minimum top inset so one Top
 * setting clears both the projection surface and CarLink's own screens (Settings, Home, and
 * everything that uses [PremiumScreen]).
 */
@Composable
fun appChromeInsets(): WindowInsets =
    WindowInsets.safeDrawing.union(WindowInsets(top = LocalScreenMarginTopPx.current.coerceAtLeast(0)))

/**
 * A secondary screen on the atmosphere: the canvas runs behind the system bars, the content is
 * inset from them, and a header with a round glass Back button and the title sits on top (the
 * same header as Settings). [content] gets the area under the header and its horizontal margin.
 */
@Composable
fun PremiumScreen(
    title: String,
    onBack: () -> Unit,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable BoxWithConstraintsScope.(margin: Dp) -> Unit,
) {
    val dimens = CarLinkTokens.dimens
    val margin = if (CarLinkTokens.layout.width == WidthClass.COMPACT) dimens.compactMargin else dimens.screenMargin
    val car = CarLinkTokens.profile == UiProfile.CAR
    Atmosphere(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(appChromeInsets())) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = dimens.topBar + if (car) 16.dp else 8.dp).padding(horizontal = margin),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundGlassButton(R.drawable.carlink_ic_arrow_back, stringResource(R.string.carlink_back), onBack)
                Spacer(Modifier.width(if (car) 24.dp else 16.dp))
                Text(title, Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (trailing != null) {
                    Spacer(Modifier.width(dimens.gutter))
                    trailing()
                }
            }
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) { content(margin) }
        }
    }
}
