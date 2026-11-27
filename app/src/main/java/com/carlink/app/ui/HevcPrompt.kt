package com.carlink.app.ui

import com.carlink.core.settings.CarBrand

/**
 * One Yes/No question for every car except the Changan CS75 Plus: turn on CarPlay HEVC. The CS75
 * Plus never sees it (HEVC and the status-bar hide are on by default there, see
 * [com.carlink.core.settings.Cs75ProjectionDefaults]). Shown once; either answer is remembered.
 */
object HevcPrompt {
    /**
     * [firstLaunchQuestionShowing]: the CS75 car question is on screen, so this one waits for its
     * answer (No) instead of stacking a second dialog.
     */
    fun shouldShow(brand: CarBrand?, alreadyAnswered: Boolean, firstLaunchQuestionShowing: Boolean): Boolean =
        !alreadyAnswered && !firstLaunchQuestionShowing && brand?.isChanganCs75Plus != true
}
