package com.carlink.app.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppVersionTest {

    @Test fun parsesTagsWithTwoOrThreeParts() {
        assertEquals(AppVersion(1, 1), AppVersion.parseTag("v1.1"))
        assertEquals(AppVersion(1, 1, 2), AppVersion.parseTag("v1.1.2"))
        assertEquals(AppVersion(1, 2), AppVersion.parseTag("1.2"))
        assertEquals(AppVersion(10, 0), AppVersion.parseTag(" V10.0.0 "))
        // v1.1 and v1.1.0 are the same release.
        assertEquals(AppVersion.parseTag("v1.1"), AppVersion.parseTag("v1.1.0"))
    }

    @Test fun rejectsMalformedTags() {
        listOf(null, "", "v1", "v1.2.3.4", "v1.2.3-beta", "v1.1-beta", "latest", "v1.x.3", "v1.200.0", "vv1.2.3", "v-1.2.3",
            "CS75 Plus 2023 v1.1")
            .forEach { assertNull("'$it' must not parse", AppVersion.parseTag(it)) }
    }

    @Test fun readsThisBuildsVersionName() {
        assertEquals(AppVersion(1, 1), AppVersion.parseVersionName("CS75 Plus 2023 v1.1"))
        assertEquals(AppVersion(1, 2, 3), AppVersion.parseVersionName("CS75 Plus 2023 v1.2.3"))
        assertEquals(AppVersion(1, 1), AppVersion.parseVersionName("CS75 Plus 2023 v1.1-hud-test2"))
        assertEquals(AppVersion(1, 0, 17), AppVersion.parseVersionName("1.0.17"))
        assertNull(AppVersion.parseVersionName("CS75 Plus 2023"))
        assertNull(AppVersion.parseVersionName("Another car v1.1"))
    }

    @Test fun comparesSemanticallyNotAsText() {
        assertTrue(AppVersion(1, 10) > AppVersion(1, 9))
        assertTrue(AppVersion(1, 1, 1) > AppVersion(1, 1))
        assertTrue(AppVersion(2, 0) > AppVersion(1, 99, 99))
        assertEquals(listOf("v1.0.2", "v1.0.10", "v1.2", "v10.0"),
            listOf("10.0.0", "1.0.10", "1.2", "1.0.2").map { AppVersion.parseTag(it)!! }.sorted().map { it.tag })
    }

    @Test fun namesAndTags() {
        assertEquals("v1.1", AppVersion(1, 1).tag)
        assertEquals("v1.1.2", AppVersion(1, 1, 2).tag)
        assertEquals("CS75 Plus 2023 v1.1", AppVersion(1, 1).toString())
        assertEquals("CarLink-CS75Plus2023-v1.1.apk", ReleaseRepo.apkName(AppVersion(1, 1)))
        assertEquals("CarLink-CS75Plus2023-v1.1.apk.sha256", ReleaseRepo.checksumName(AppVersion(1, 1)))
        assertEquals(AppVersion(1, 1), ReleaseRepo.apkVersion("CarLink-CS75Plus2023-v1.1.apk"))
        assertEquals(AppVersion(1, 1), ReleaseRepo.apkVersion("CarLink-CS75Plus2023-v1.1.0.apk"))
        // Upstream CarLink's APKs are never picked.
        assertNull(ReleaseRepo.apkVersion("CarLink-1.1.0.apk"))
        assertNull(ReleaseRepo.apkVersion("CarLink-CS75Plus2023-v1.1.apk.sha256"))
    }

    @Test fun releasesComeOnlyFromThisBuildsRepository() {
        assertEquals("0nezar0/carlink-releases", ReleaseRepo.OWNER_REPO)
        listOf(ReleaseRepo.LATEST_URL, ReleaseRepo.LIST_URL, ReleaseRepo.DOWNLOAD_PREFIX).forEach {
            assertTrue(it, it.contains(ReleaseRepo.OWNER_REPO) && !it.contains("F-e-n-y-x"))
        }
    }
}
