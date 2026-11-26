package com.carlink.app.update

/**
 * A release version of this private build, `major.minor[.patch]`: versionName
 * `CS75 Plus 2023 v1.1`, tag `v1.1`, asset `CarLink-CS75Plus2023-v1.1.apk`.
 *
 * Its Android versionCode is set by hand in app/build.gradle.kts (10031 for v1.1) and must go up
 * with every release: the updater installs an APK only when its versionCode is higher than the
 * installed one ([UpdateVerifier]).
 */
data class AppVersion(val major: Int, val minor: Int, val patch: Int = 0) : Comparable<AppVersion> {

    override fun compareTo(other: AppVersion): Int =
        compareValuesBy(this, other, AppVersion::major, AppVersion::minor, AppVersion::patch)

    /** `1.1` or `1.1.2`: the patch only when it is not zero. */
    val number: String get() = if (patch == 0) "$major.$minor" else "$major.$minor.$patch"

    /** The release tag on GitHub, `v1.1`. */
    val tag: String get() = "v$number"

    /** As the build names itself: `CS75 Plus 2023 v1.1`. */
    override fun toString(): String = "$BUILD_NAME $tag"

    companion object {
        /** The car this build is for, in front of every version name. */
        const val BUILD_NAME = "CS75 Plus 2023"

        private val TAG = Regex("""[vV]?(\d{1,4})\.(\d{1,2})(?:\.(\d{1,2}))?""")
        private val NAME = Regex("""(?:${Regex.escape(BUILD_NAME)}\s+)?[vV]?(\d{1,4})\.(\d{1,2})(?:\.(\d{1,2}))?(?:-.*)?""")

        /**
         * A release tag (`v1.1`, `v1.1.2` or `1.1`), or null when it is anything else: pre-release
         * suffixes, four parts, words. Malformed tags are never offered as updates.
         */
        fun parseTag(tag: String?): AppVersion? = tag?.trim()?.let(TAG::matchEntire)?.toVersion()

        /** The installed versionName (`CS75 Plus 2023 v1.1`); a build suffix (`-hud-test2`) is ignored. */
        fun parseVersionName(name: String?): AppVersion? = name?.trim()?.let(NAME::matchEntire)?.toVersion()

        private fun MatchResult.toVersion(): AppVersion? {
            val (major, minor, patch) = destructured
            return runCatching { AppVersion(major.toInt(), minor.toInt(), patch.ifEmpty { "0" }.toInt()) }.getOrNull()
        }
    }
}
