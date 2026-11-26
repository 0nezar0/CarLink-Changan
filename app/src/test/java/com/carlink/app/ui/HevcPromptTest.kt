package com.carlink.app.ui

import com.carlink.core.settings.CarBrand
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HevcPromptTest {
    @Test fun shownOnceForACarThatIsNotTheCs75() {
        assertTrue(HevcPrompt.shouldShow(CarBrand.OTHER, alreadyAnswered = false, firstLaunchQuestionShowing = false))
        assertTrue(HevcPrompt.shouldShow(CarBrand.BYD, alreadyAnswered = false, firstLaunchQuestionShowing = false))
        assertTrue(HevcPrompt.shouldShow(CarBrand.CHANGAN, alreadyAnswered = false, firstLaunchQuestionShowing = false))
        assertTrue(HevcPrompt.shouldShow(null, alreadyAnswered = false, firstLaunchQuestionShowing = false))
    }

    @Test fun neverShownForTheCs75Plus() {
        assertFalse(HevcPrompt.shouldShow(CarBrand.CHANGAN_CS75_PLUS, alreadyAnswered = false, firstLaunchQuestionShowing = false))
    }

    @Test fun notShownAgainOnceAnswered() {
        assertFalse(HevcPrompt.shouldShow(CarBrand.OTHER, alreadyAnswered = true, firstLaunchQuestionShowing = false))
        assertFalse(HevcPrompt.shouldShow(null, alreadyAnswered = true, firstLaunchQuestionShowing = false))
    }

    @Test fun waitsWhileTheFirstLaunchCarQuestionIsOnScreen() {
        assertFalse(HevcPrompt.shouldShow(null, alreadyAnswered = false, firstLaunchQuestionShowing = true))
    }
}
