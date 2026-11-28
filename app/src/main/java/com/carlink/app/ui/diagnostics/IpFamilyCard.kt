package com.carlink.app.ui.diagnostics

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.carlink.app.R
import com.carlink.app.ui.components.StatGrid
import com.carlink.app.ui.settings.GlassGroup
import com.carlink.app.ui.settings.GroupHeader
import com.carlink.app.ui.settings.isCar
import com.carlink.app.ui.settings.rowPadding
import com.carlink.app.ui.theme.CarLinkTokens
import com.shilapi.xcertplay.network.CarHotspotIpFamily
import java.text.DateFormat
import java.util.Date

/**
 * v1.7.8: IPv4 is forced on the car hotspot, an iPhone's Personal Hotspot and Same Wi-Fi, for every screen
 * (WirelessAddressFamilyPlan.ipv4Only); nothing is probed or chosen. Diagnostics says so, with the time of the
 * last wireless session that applied it ([CarHotspotIpFamily.recordForcedIpv4]), or without a time before the first.
 */
internal data class IpFamilyUi(
    val reason: String = CarHotspotIpFamily.REASON_FORCED_IPV4,
    val decidedAtMillis: Long? = null,
) {
    companion object {
        /** Always the forced IPv4 card; an older engine's record (probe, dual stack, fallback) is no longer true. */
        fun from(last: CarHotspotIpFamily.LastDecision?): IpFamilyUi =
            IpFamilyUi(decidedAtMillis = last?.takeIf { it.reason == CarHotspotIpFamily.REASON_FORCED_IPV4 }?.decidedAtMillis)
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
    CarHotspotIpFamily.REASON_FORCED_IPV4 -> R.string.carlink_ipfamily_r_forced
    else -> R.string.carlink_ipfamily_r_unknown
}

@Composable
internal fun IpFamilyCard(ui: IpFamilyUi?) {
    val scheme = MaterialTheme.colorScheme
    val shown = ui ?: IpFamilyUi()
    GlassGroup(header = { GroupHeader(stringResource(R.string.carlink_ipfamily_title), icon = R.drawable.carlink_ic_wifi_tethering) }) {
        Column(Modifier.fillMaxWidth().padding(start = rowPadding, end = rowPadding, top = 12.dp, bottom = rowPadding)) {
            val tiles = buildList {
                add(stringResource(R.string.carlink_ipfamily_in_use) to stringResource(R.string.carlink_ipfamily_v4))
                shown.decidedAtMillis?.let {
                    add(stringResource(R.string.carlink_ipfamily_decided) to
                        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it)))
                }
            }
            StatGrid(tiles.size, minTileWidth = if (isCar) 200.dp else 130.dp, maxColumns = 3) { i, m ->
                StatTile(tiles[i].first, tiles[i].second, m, labelLines = 2)
            }
            Spacer(Modifier.height(CarLinkTokens.dimens.targetGap))
            StatTile(stringResource(R.string.carlink_ipfamily_reason), stringResource(ipFamilyReasonRes(shown.reason)),
                Modifier.fillMaxWidth(), maxLines = 4)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.carlink_ipfamily_forced_note), style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant)
        }
    }
}
