package com.carlink.app.ui.diagnostics

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.carlink.app.R
import com.carlink.app.ui.components.StatGrid
import com.carlink.app.ui.settings.GlassGroup
import com.carlink.app.ui.settings.GroupHeader
import com.carlink.app.ui.settings.RowGlyph
import com.carlink.app.ui.settings.isCar
import com.carlink.app.ui.settings.rowPadding
import com.carlink.app.ui.theme.CarLinkTokens
import com.shilapi.xcertplay.network.CarHotspotIpFamily
import java.text.DateFormat
import java.util.Date

/**
 * v1.6.6: the car hotspot's automatic address-family decision, as the engine recorded it
 * ([CarHotspotIpFamily.lastDecision]): IPv4 only or IPv4 + IPv6, whether the screen supports IPv6,
 * why, and when. There is no setting: the choice is automatic.
 */
internal data class IpFamilyUi(
    val dualStack: Boolean,
    val ipv6Supported: Boolean,
    val reason: String,
    val decidedAtMillis: Long,
) {
    companion object {
        fun from(last: CarHotspotIpFamily.LastDecision?): IpFamilyUi? = last?.let {
            IpFamilyUi(it.mode == CarHotspotIpFamily.MODE_DUAL_STACK, it.ipv6Supported, it.reason, it.decidedAtMillis)
        }
    }
}

/** The string for one engine reason; unknown reasons (a newer engine) fall back to "Not known". */
internal fun ipFamilyReasonRes(reason: String): Int = when (reason) {
    CarHotspotIpFamily.REASON_KNOWN_IPV4_UNIT -> R.string.carlink_ipfamily_r_known
    CarHotspotIpFamily.REASON_REMEMBERED_IPV4 -> R.string.carlink_ipfamily_r_rem_v4
    CarHotspotIpFamily.REASON_REMEMBERED_DUAL -> R.string.carlink_ipfamily_r_rem_dual
    CarHotspotIpFamily.REASON_NO_IPV4 -> R.string.carlink_ipfamily_r_no_v4
    CarHotspotIpFamily.REASON_IPV6_DISABLED -> R.string.carlink_ipfamily_r_v6_off
    CarHotspotIpFamily.REASON_NO_IPV6 -> R.string.carlink_ipfamily_r_no_v6
    CarHotspotIpFamily.REASON_IPV6_NOT_USABLE -> R.string.carlink_ipfamily_r_v6_not_usable
    CarHotspotIpFamily.REASON_LINK_LOCAL_ONLY -> R.string.carlink_ipfamily_r_link_local
    CarHotspotIpFamily.REASON_NO_IPV6_ROUTE -> R.string.carlink_ipfamily_r_no_route
    CarHotspotIpFamily.REASON_IPV6_USABLE -> R.string.carlink_ipfamily_r_usable
    CarHotspotIpFamily.REASON_FALLBACK_NO_TCP -> R.string.carlink_ipfamily_r_fb_tcp
    CarHotspotIpFamily.REASON_FALLBACK_PICTURE_STALL -> R.string.carlink_ipfamily_r_fb_picture
    else -> R.string.carlink_ipfamily_r_unknown
}

@Composable
internal fun IpFamilyCard(ui: IpFamilyUi?) {
    val scheme = MaterialTheme.colorScheme
    GlassGroup(header = { GroupHeader(stringResource(R.string.carlink_ipfamily_title), icon = R.drawable.carlink_ic_wifi_tethering) }) {
        Column(Modifier.fillMaxWidth().padding(start = rowPadding, end = rowPadding, top = 12.dp, bottom = rowPadding)) {
            if (ui == null) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    RowGlyph(R.drawable.carlink_ic_wifi_tethering, tint = scheme.onSurfaceVariant)
                    Spacer(Modifier.width(if (isCar) 24.dp else 16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.carlink_ipfamily_empty_title), style = MaterialTheme.typography.titleMedium,
                            color = scheme.onSurface)
                        Spacer(Modifier.height(4.dp))
                        Text(stringResource(R.string.carlink_ipfamily_empty_body), style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant)
                    }
                }
                return@Column
            }
            val tiles = listOf(
                stringResource(R.string.carlink_ipfamily_in_use) to
                    stringResource(if (ui.dualStack) R.string.carlink_ipfamily_dual else R.string.carlink_ipfamily_v4),
                stringResource(R.string.carlink_ipfamily_supports) to
                    stringResource(if (ui.ipv6Supported) R.string.carlink_ipfamily_yes else R.string.carlink_ipfamily_no),
                stringResource(R.string.carlink_ipfamily_decided) to
                    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(ui.decidedAtMillis)),
            )
            StatGrid(tiles.size, minTileWidth = if (isCar) 200.dp else 130.dp, maxColumns = 3) { i, m ->
                StatTile(tiles[i].first, tiles[i].second, m, labelLines = 2)
            }
            Spacer(Modifier.height(CarLinkTokens.dimens.targetGap))
            StatTile(stringResource(R.string.carlink_ipfamily_reason), stringResource(ipFamilyReasonRes(ui.reason)),
                Modifier.fillMaxWidth(), maxLines = 4)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.carlink_ipfamily_auto_note), style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant)
        }
    }
}
