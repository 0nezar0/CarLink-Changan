package com.carlink.core.connection

import com.carlink.core.settings.CarBrand

/**
 * The icon of CarPlay's "back to the car" button ([com.shilapi.xcertplay.CarPlayHostIntegration.carButtonIcon]).
 * [com.carlink.core.settings.CarBrand.CHANGAN_CS75_PLUS] gets the Changan logo. Every other brand (including generic Changan), and no brand, gets CarLink's default car
 * icon, also on a BYD head unit, where DiPlay would otherwise send the BYD logo.
 */
object CarPlayCarButton {
    fun icon(brand: CarBrand?): Int =
        if (brand?.isChanganCs75Plus == true) com.carlink.core.R.raw.carlink_changan_home
        else com.shilapi.xcertplay.host.R.raw.ic_carlink_home
}
