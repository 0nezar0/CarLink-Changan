package com.carlink.app.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class UpdateVerifierTest {
    @get:Rule val temp = TemporaryFolder()

    private val pin = ReleaseKey.normalized
    private val good = ApkFacts(Updates.RELEASE_PACKAGE, 10100L, setOf(pin))

    private fun apk(bytes: String = "apk bytes") = temp.newFile().apply { writeText(bytes) }
    private fun verifier(facts: ApkFacts?, installed: Long = 10000L) =
        UpdateVerifier(JavaSha256, { facts }, Updates.RELEASE_PACKAGE, installed)

    @Test fun sha256OfAKnownInput() {
        // printf 'abc' | sha256sum
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            JavaSha256.sha256Hex(temp.newFile().apply { writeText("abc") }))
    }

    @Test fun pinnedFingerprintNormalizes() {
        assertEquals(64, pin.length)
        // The key of the CS75 Plus 2023 releases (CarLink-CS75Plus2023-v1.1.apk), not upstream CarLink's.
        assertEquals("4b194c0c5d3f25bb65927771c30e5a78bb57a4aca1e85babd763b03dd9554a6b", pin)
        assertEquals("com.carlink.app.hudtest", Updates.RELEASE_PACKAGE)
        assertEquals(pin, ReleaseKey.normalize("4b 19 4c 0c 5d 3f 25 bb 65 92 77 71 c3 0e 5a 78 bb 57 a4 ac a1 e8 5b ab d7 63 b0 3d d9 55 4a 6b"))
    }

    @Test fun passesWhenAllThreeChecksPass() {
        val file = apk()
        assertNull(verifier(good).verify(file, JavaSha256.sha256Hex(file).uppercase()))
    }

    @Test fun checksumMismatch() {
        val file = apk()
        assertEquals(VerifyFailure.CHECKSUM_MISMATCH, verifier(good).verify(file, "0".repeat(64)))
    }

    @Test fun unreadableApk() {
        val file = apk()
        assertEquals(VerifyFailure.UNREADABLE, verifier(null).verify(file, JavaSha256.sha256Hex(file)))
        assertEquals(VerifyFailure.UNREADABLE, verifier(good).verify(java.io.File(temp.root, "missing.apk"), "0".repeat(64)))
    }

    @Test fun wrongPackage() {
        val file = apk()
        assertEquals(VerifyFailure.WRONG_PACKAGE,
            // Upstream CarLink's package is not this build.
            verifier(good.copy(packageName = "com.carlink.app")).verify(file, JavaSha256.sha256Hex(file)))
    }

    @Test fun notNewer() {
        val file = apk()
        val sha = JavaSha256.sha256Hex(file)
        assertEquals(VerifyFailure.NOT_NEWER, verifier(good.copy(versionCode = 10000L)).verify(file, sha))
        assertEquals(VerifyFailure.NOT_NEWER, verifier(good.copy(versionCode = 9999L)).verify(file, sha))
    }

    @Test fun wrongOrExtraSigner() {
        val file = apk()
        val sha = JavaSha256.sha256Hex(file)
        val other = "f".repeat(64)
        assertEquals(VerifyFailure.WRONG_SIGNER, verifier(good.copy(signerSha256 = setOf(other))).verify(file, sha))
        assertEquals(VerifyFailure.WRONG_SIGNER, verifier(good.copy(signerSha256 = setOf(pin, other))).verify(file, sha))
        assertEquals(VerifyFailure.WRONG_SIGNER, verifier(good.copy(signerSha256 = emptySet())).verify(file, sha))
    }

    @Test fun inspectorThatThrowsIsUnreadable() {
        val file = apk()
        val verifier = UpdateVerifier(JavaSha256, { error("broken zip") }, Updates.RELEASE_PACKAGE, 10000L)
        assertEquals(VerifyFailure.UNREADABLE, verifier.verify(file, JavaSha256.sha256Hex(file)))
    }
}
