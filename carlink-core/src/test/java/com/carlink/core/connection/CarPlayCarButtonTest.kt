package com.carlink.core.connection

import com.carlink.core.settings.CarBrand
import org.junit.Assert.assertEquals
import org.junit.Test

class CarPlayCarButtonTest {
    @Test fun onlyChanganGetsItsLogoEveryOtherCarGetsTheDefaultIcon() {
        assertEquals(com.carlink.core.R.raw.carlink_changan_home, CarPlayCarButton.icon(CarBrand.CHANGAN_CS75_PLUS))
        // No brand logo for any other car, BYD included: the default car icon.
        for (brand in CarBrand.entries.filter { !it.isChanganCs75Plus } + listOf<CarBrand?>(null)) {
            assertEquals("$brand", com.shilapi.xcertplay.host.R.raw.ic_carlink_home, CarPlayCarButton.icon(brand))
        }
    }
}
