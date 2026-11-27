package com.carlink.core.connection

import com.carlink.core.settings.CarBrand
import org.junit.Assert.assertEquals
import org.junit.Test

class CarPlayCarButtonTest {
    @Test fun changanBrandsGetTheChanganLogoEveryOtherCarGetsTheDefaultIcon() {
        val logo = com.carlink.core.R.raw.carlink_changan_home
        assertEquals(logo, CarPlayCarButton.icon(CarBrand.CHANGAN_CS75_PLUS))
        assertEquals(logo, CarPlayCarButton.icon(CarBrand.CHANGAN))
        // No brand logo for any other car, BYD included: the default car icon.
        for (brand in CarBrand.entries.filter { it != CarBrand.CHANGAN && !it.isChanganCs75Plus } + listOf<CarBrand?>(null)) {
            assertEquals("$brand", com.shilapi.xcertplay.host.R.raw.ic_carlink_home, CarPlayCarButton.icon(brand))
        }
    }
}
