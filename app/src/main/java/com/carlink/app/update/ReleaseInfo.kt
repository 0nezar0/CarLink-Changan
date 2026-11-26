package com.carlink.app.update

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Where this build's releases are published. [OWNER_REPO] is the only place to change: point it at
 * any GitHub repository whose releases carry `CarLink-CS75Plus2023-vX.Y.apk` and its `.sha256`.
 * The app asks GitHub without signing in, so the repository's releases must be public; a private
 * repository answers 404 ([CheckOutcome.NotFound]). Upstream CarLink (F-e-n-y-x) is never asked:
 * its APKs are another package and key and could not install over this build.
 */
object ReleaseRepo {
    const val OWNER_REPO = "0nezar0/carlink-releases"
    const val LATEST_URL = "https://api.github.com/repos/$OWNER_REPO/releases/latest"
    /** The newest releases, pre-releases included (drafts are visible only when signed in): "Get test versions". */
    const val LIST_URL = "https://api.github.com/repos/$OWNER_REPO/releases?per_page=20"
    const val DOWNLOAD_PREFIX = "https://github.com/$OWNER_REPO/releases/download/"
    const val ACCEPT = "application/vnd.github+json"
    /** The release page, for people who install by hand. */
    const val RELEASES_PAGE = "https://github.com/$OWNER_REPO/releases"

    private const val APK_PREFIX = "CarLink-CS75Plus2023-"

    fun apkName(version: AppVersion) = "$APK_PREFIX${version.tag}.apk"
    fun checksumName(version: AppVersion) = "${apkName(version)}.sha256"

    /** The version in an APK asset's name (`CarLink-CS75Plus2023-v1.1.apk`, `-v1.1.0.apk`), or null for any other file. */
    fun apkVersion(assetName: String): AppVersion? =
        assetName.takeIf { it.startsWith(APK_PREFIX) && it.endsWith(".apk") }
            ?.removePrefix(APK_PREFIX)?.removeSuffix(".apk")?.let(AppVersion::parseTag)
}

/** A published release that carries an installable APK and its checksum. */
data class ReleaseInfo(
    val tag: String,
    val version: AppVersion,
    val title: String,
    /** The release notes (Markdown as written on GitHub). */
    val notes: String,
    val apkUrl: String,
    /** The APK's size in bytes, or 0 when GitHub did not say. */
    val apkSize: Long,
    val checksumUrl: String,
)

/** The result of reading GitHub's "latest release" answer. */
sealed interface ParsedRelease {
    data class Usable(val release: ReleaseInfo) : ParsedRelease
    data class Unusable(val reason: Reason) : ParsedRelease

    enum class Reason { NOT_JSON, DRAFT, PRERELEASE, MALFORMED_TAG, NO_APK, NO_CHECKSUM }
}

/**
 * Parses `GET /repos/{owner}/{repo}/releases/latest`. That endpoint already skips drafts and
 * pre-releases; they are rejected here as well. A release is usable only when its tag is
 * `vX.Y` or `vX.Y.Z` and it has the assets `CarLink-CS75Plus2023-vX.Y.apk` (the same version as
 * the tag) and that name plus `.sha256`, both served from [ReleaseRepo]'s release downloads.
 */
object ReleaseParser {
    fun parse(json: String): ParsedRelease {
        val release = try { JSONObject(json) } catch (_: JSONException) {
            return ParsedRelease.Unusable(ParsedRelease.Reason.NOT_JSON)
        }
        return parse(release, allowPrerelease = false)
    }

    /**
     * "Get test versions": the highest usable version in `GET /repos/{owner}/{repo}/releases` (a
     * JSON array), pre-releases included; drafts never. A pre-release uses the same `vX.Y.Z` tag and
     * assets as a release, so it is verified exactly the same way. [ParsedRelease.Unusable] when none is.
     */
    fun newest(json: String): ParsedRelease {
        val list = try { JSONArray(json) } catch (_: JSONException) {
            return ParsedRelease.Unusable(ParsedRelease.Reason.NOT_JSON)
        }
        val parsed = (0 until list.length()).mapNotNull { list.optJSONObject(it) }.map { parse(it, allowPrerelease = true) }
        return parsed.filterIsInstance<ParsedRelease.Usable>().maxByOrNull { it.release.version }
            ?: parsed.firstOrNull() ?: ParsedRelease.Unusable(ParsedRelease.Reason.NO_APK)
    }

    private fun parse(release: JSONObject, allowPrerelease: Boolean): ParsedRelease {
        if (release.optBoolean("draft")) return ParsedRelease.Unusable(ParsedRelease.Reason.DRAFT)
        if (!allowPrerelease && release.optBoolean("prerelease")) return ParsedRelease.Unusable(ParsedRelease.Reason.PRERELEASE)
        val tag = release.optString("tag_name")
        val version = AppVersion.parseTag(tag) ?: return ParsedRelease.Unusable(ParsedRelease.Reason.MALFORMED_TAG)
        val assets = release.optJSONArray("assets")
        var apk: Pair<String, Long>? = null
        var checksum: String? = null
        val usable = (0 until (assets?.length() ?: 0)).mapNotNull { assets!!.optJSONObject(it) }
            .filter { it.optString("browser_download_url").startsWith(ReleaseRepo.DOWNLOAD_PREFIX) }
        // The APK named for the tag's version (v1.1 and v1.1.0 are the same), then its checksum.
        val apkAsset = usable.firstOrNull { ReleaseRepo.apkVersion(it.optString("name")) == version }
        if (apkAsset != null) {
            apk = apkAsset.optString("browser_download_url") to apkAsset.optLong("size", 0L)
            checksum = usable.firstOrNull { it.optString("name") == apkAsset.optString("name") + ".sha256" }
                ?.optString("browser_download_url")
        }
        if (apk == null) return ParsedRelease.Unusable(ParsedRelease.Reason.NO_APK)
        if (checksum == null) return ParsedRelease.Unusable(ParsedRelease.Reason.NO_CHECKSUM)
        return ParsedRelease.Usable(ReleaseInfo(
            tag = tag.trim(),
            version = version,
            title = release.optString("name").takeIf { it.isNotBlank() && it != "null" } ?: "CarLink $version",
            notes = release.optString("body").takeIf { it != "null" }.orEmpty(),
            apkUrl = apk.first,
            apkSize = apk.second.coerceAtLeast(0L),
            checksumUrl = checksum,
        ))
    }

    /**
     * The hex SHA-256 from a `.sha256` file: the bare digest, or `sha256sum` output
     * (`<digest>  CarLink-CS75Plus2023-v1.1.apk`). Null when the file holds anything else.
     */
    fun parseChecksum(text: String): String? =
        text.trim().split(Regex("\\s+")).firstOrNull()?.lowercase()?.takeIf { it.matches(Regex("[0-9a-f]{64}")) }
}

/**
 * Release notes as plain text for the update dialog: Markdown marks removed, lists as bullets.
 * Only the part before a line that is exactly [MORE_MARKER] is shown, so the release page can
 * carry install steps, screenshots and checksums below it.
 */
object ReleaseNotes {
    const val MORE_MARKER = "<!-- more -->"

    fun plain(markdown: String, maxChars: Int = 4_000): String {
        val all = markdown.replace("\r\n", "\n").lines()
        val cut = all.indexOfFirst { it.trim() == MORE_MARKER }
        val lines = (if (cut < 0) all else all.take(cut)).map { raw ->
            var line = raw.trimEnd()
            line = line.replace(Regex("""^\s{0,3}#{1,6}\s+"""), "")
            line = line.replace(Regex("""^(\s*)[-*+]\s+"""), "$1• ")
            line = line.replace(Regex("""\*\*(.+?)\*\*"""), "$1").replace(Regex("""__(.+?)__"""), "$1")
            line = line.replace(Regex("""`([^`]*)`"""), "$1")
            line = line.replace(Regex("""!?\[([^\]]*)]\([^)]*\)"""), "$1")
            line
        }
        // No more than one blank line in a row, none at the ends.
        val text = lines.joinToString("\n").replace(Regex("\n{3,}"), "\n\n").trim()
        return if (text.length <= maxChars) text else text.take(maxChars).trimEnd() + "…"
    }
}
