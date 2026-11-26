package com.carlink.app.ui

import com.carlink.core.settings.CarBrand
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectionPromptTest {
    @Test fun shownOnceForACarThatIsNotTheCs75() {
        assertTrue(ProjectionPrompt.shouldShow(CarBrand.OTHER, alreadyAnswered = false, firstLaunchQuestionShowing = false))
        assertTrue(ProjectionPrompt.shouldShow(CarBrand.BYD, alreadyAnswered = false, firstLaunchQuestionShowing = false))
        assertTrue(ProjectionPrompt.shouldShow(CarBrand.CHANGAN, alreadyAnswered = false, firstLaunchQuestionShowing = false))
        assertTrue(ProjectionPrompt.shouldShow(null, alreadyAnswered = false, firstLaunchQuestionShowing = false))
    }

    @Test fun neverShownForTheCs75Plus() {
        assertFalse(ProjectionPrompt.shouldShow(CarBrand.CHANGAN_CS75_PLUS, alreadyAnswered = false, firstLaunchQuestionShowing = false))
    }

    @Test fun notShownAgainOnceAnswered() {
        assertFalse(ProjectionPrompt.shouldShow(CarBrand.OTHER, alreadyAnswered = true, firstLaunchQuestionShowing = false))
        assertFalse(ProjectionPrompt.shouldShow(null, alreadyAnswered = true, firstLaunchQuestionShowing = false))
    }

    @Test fun waitsWhileTheFirstLaunchCarQuestionIsOnScreen() {
        assertFalse(ProjectionPrompt.shouldShow(null, alreadyAnswered = false, firstLaunchQuestionShowing = true))
    }
}
