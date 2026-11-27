package com.carlink.core.connection

import com.carlink.core.settings.CarBrand

/**
 * The icon of CarPlay's "back to the car" button ([com.shilapi.xcertplay.CarPlayHostIntegration.carButtonIcon]).
 * [CarBrand.CHANGAN] and [CarBrand.CHANGAN_CS75_PLUS] get the same Changan logo. Every other brand, and no brand, gets CarLink's default car
 * icon, also on a BYD head unit, where DiPlay would otherwise send the BYD logo.
 */
object CarPlayCarButton {
    fun icon(brand: CarBrand?): Int =
        if (brand == CarBrand.CHANGAN || brand?.isChanganCs75Plus == true) com.carlink.core.R.raw.carlink_changan_home
        else com.shilapi.xcertplay.host.R.raw.ic_carlink_home
}
