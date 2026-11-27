package com.carlink.app.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Cs75FirstLaunchPromptTest {
    private fun show(freshInstall: Boolean = true, priorUse: Boolean = false, brandChosen: Boolean = false, answered: Boolean = false) =
        Cs75FirstLaunchPrompt.shouldShow(freshInstall, priorUse, brandChosen, answered)

    @Test fun shownOnAFreshInstallWithNoBrandAndNoPriorAnswer() {
        assertTrue(show())
    }

    @Test fun shownAfterClearingDataEvenOnAnUpdatedBuild() {
        // firstInstallTime != lastUpdateTime, but the data folder is empty.
        assertTrue(show(freshInstall = false, priorUse = false))
    }

    @Test fun notShownOnAnUpdateOfABuildAlreadyInUse() {
        assertFalse(show(freshInstall = false, priorUse = true))
    }

    @Test fun notShownWhenABrandIsAlreadyChosen() {
        assertFalse(show(brandChosen = true))
    }

    @Test fun notShownAgainAfterTheDriverAnswers() {
        assertFalse(show(answered = true))
        assertFalse(show(freshInstall = false, priorUse = false, answered = true))
    }

    @Test fun aMatchingInstallAndUpdateTimeIsAFreshInstall() {
        assertTrue(Cs75FirstLaunchPrompt.isFreshInstall(1000L, 1000L))
        assertFalse(Cs75FirstLaunchPrompt.isFreshInstall(1000L, 2000L))
    }
}
