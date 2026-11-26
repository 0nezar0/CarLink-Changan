package com.carlink.app.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Cs75FirstLaunchPromptTest {
    @Test fun shownOnlyOnAFreshInstallWithNoBrandAndNoPriorAnswer() {
        assertTrue(Cs75FirstLaunchPrompt.shouldShow(freshInstall = true, brandChosen = false, alreadyAnswered = false))
    }

    @Test fun notShownOnAnUpdate() {
        assertFalse(Cs75FirstLaunchPrompt.shouldShow(freshInstall = false, brandChosen = false, alreadyAnswered = false))
    }

    @Test fun notShownWhenABrandIsAlreadyChosen() {
        assertFalse(Cs75FirstLaunchPrompt.shouldShow(freshInstall = true, brandChosen = true, alreadyAnswered = false))
    }

    @Test fun notShownAgainAfterTheDriverAnswers() {
        assertFalse(Cs75FirstLaunchPrompt.shouldShow(freshInstall = true, brandChosen = false, alreadyAnswered = true))
    }

    @Test fun aMatchingInstallAndUpdateTimeIsAFreshInstall() {
        assertTrue(Cs75FirstLaunchPrompt.isFreshInstall(1000L, 1000L))
        assertFalse(Cs75FirstLaunchPrompt.isFreshInstall(1000L, 2000L))
    }
}
