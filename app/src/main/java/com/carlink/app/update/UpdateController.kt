package com.carlink.app.update

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import kotlin.coroutines.CoroutineContext

/** Where the updater is; the update dialog and About's update row are drawn from it. */
sealed interface UpdateState {
    data object Idle : UpdateState
    /** A check the driver asked for (automatic checks run silently). */
    data object Checking : UpdateState
    /** The answer to a check the driver asked for. */
    data object UpToDate : UpdateState
    data class CheckFailed(val rateLimited: Boolean, val notFound: Boolean = false) : UpdateState

    data class Available(val release: ReleaseInfo) : UpdateState
    data class Downloading(val release: ReleaseInfo, val bytes: Long, val total: Long?) : UpdateState
    data class Verifying(val release: ReleaseInfo) : UpdateState
    /** Android must allow CarLink to install apps first ("Install unknown apps"). */
    data class NeedsPermission(val release: ReleaseInfo) : UpdateState
    data class Installing(val release: ReleaseInfo) : UpdateState
    data class Failed(val release: ReleaseInfo, val error: UpdateError, val detail: String? = null) : UpdateState
}

/** The update dialog is open in these states. */
val UpdateState.showsDialog: Boolean
    get() = this is UpdateState.Available || this is UpdateState.Downloading || this is UpdateState.Verifying ||
        this is UpdateState.NeedsPermission || this is UpdateState.Installing || this is UpdateState.Failed

/**
 * Why an update did not install. The downloaded file is deleted in every case except [INSTALL] and
 * [INSTALL_CANCELLED]: it passed every check, so it is kept for [UpdateController.installManually].
 */
enum class UpdateError(val canRetry: Boolean) {
    DOWNLOAD(true),
    CHECKSUM_MISSING(true),
    CHECKSUM_MISMATCH(false),
    UNREADABLE(false),
    WRONG_PACKAGE(false),
    NOT_NEWER(false),
    WRONG_SIGNER(false),
    INSTALL(true),
    INSTALL_CANCELLED(true),
}

private fun VerifyFailure.toError(): UpdateError = when (this) {
    VerifyFailure.CHECKSUM_MISMATCH -> UpdateError.CHECKSUM_MISMATCH
    VerifyFailure.UNREADABLE -> UpdateError.UNREADABLE
    VerifyFailure.WRONG_PACKAGE -> UpdateError.WRONG_PACKAGE
    VerifyFailure.NOT_NEWER -> UpdateError.NOT_NEWER
    VerifyFailure.WRONG_SIGNER -> UpdateError.WRONG_SIGNER
}

/** What [UpdateController.installManually] managed to open. */
enum class ManualInstall {
    /** Android's installer (or a file manager) took the verified APK. */
    INSTALLER,
    /** No app took the APK; the folder that holds it was opened. */
    FOLDER,
    /** Nothing opened: the dialog still shows the file's path. */
    NONE,
}

/**
 * The updater: check GitHub, offer a newer release, download it into [downloadDir] (Downloads/CarLink,
 * [UpdateFiles]), verify it ([UpdateVerifier]) and hand it to [installer]. A verified APK stays in
 * that folder after it is installed; a file that fails verification is deleted. UI-free; call it on the main thread. Every
 * dependency is an interface, so a launcher can reuse it and tests can drive it.
 *
 * Automatic checks ([maybeAutoCheck]) run at most every 6 hours, only when [supported] (the
 * official release build), only with "Automatically check for updates" on, and only while no
 * projection session runs (the caller passes [UpdatePolicy.sessionQuiet]). "Ignore this
 * version" silences that tag for good; "Later" closes the dialog until the next check.
 */
class UpdateController(
    private val scope: CoroutineScope,
    private val io: CoroutineContext,
    private val source: ReleaseSource,
    private val http: UpdateHttp,
    private val verifier: UpdateVerifier,
    private val installer: UpdateInstaller,
    private val prefs: UpdatePrefs,
    val current: AppVersion,
    /** Release build signed with the release key: the only build that checks and installs. */
    val supported: Boolean,
    /** Resolved at each download: storage access can be granted between two downloads. */
    private val downloadDir: () -> File,
    private val clock: () -> Long = System::currentTimeMillis,
    private val log: (String) -> Unit = {},
    /**
     * Suspends until no CarPlay or Android Auto session runs: Android's install screen must never
     * cover a projection while driving, and installing ends both engines (lifecycle audit #7).
     */
    private val awaitSessionQuiet: suspend () -> Unit = {},
) {
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private val _autoCheck = MutableStateFlow(prefs.autoCheckUpdates)
    /** "Automatically check for updates". */
    val autoCheck: StateFlow<Boolean> = _autoCheck.asStateFlow()

    private val _lastCheck = MutableStateFlow(prefs.lastUpdateCheckMillis)
    /** Wall-clock time of the last check, 0 for never. */
    val lastCheck: StateFlow<Long> = _lastCheck.asStateFlow()

    private val _newer = MutableStateFlow<ReleaseInfo?>(null)
    /** The newest release a check found above [current], even one the driver chose Later or Ignore for (the report sheet names it). */
    val newer: StateFlow<ReleaseInfo?> = _newer.asStateFlow()

    private val _manualApk = MutableStateFlow<File?>(null)
    /**
     * The downloaded, verified APK while the update is in the install steps (Installing, needs
     * permission, install failed or cancelled): what "Install manually" opens, and the path the
     * dialog shows. Some head units block PackageInstaller sessions silently, so the dialog offers
     * it while Android's install screen may never come. Null in every other state.
     */
    val manualApk: StateFlow<File?> = _manualApk.asStateFlow()

    private var job: Job? = null
    private var verifiedFile: File? = null

    init {
        // Update APKs older than this version (installed long ago); never the reports in the same folder.
        runCatching { UpdateFiles.stale(downloadDir(), current).forEach { it.delete() } }
    }

    fun setAutoCheck(enabled: Boolean) {
        prefs.autoCheckUpdates = enabled
        _autoCheck.value = enabled
    }

    private val _testVersions = MutableStateFlow(prefs.testVersions)
    /** "Get test versions" (GitHub pre-releases). */
    val testVersions: StateFlow<Boolean> = _testVersions.asStateFlow()

    /** Turning it on makes the next automatic check due at once, so the driver sees a test version soon. */
    fun setTestVersions(enabled: Boolean) {
        prefs.testVersions = enabled
        _testVersions.value = enabled
        if (enabled) prefs.lastUpdateCheckMillis = 0L
    }

    /**
     * Home is in front: check if one is due and [sessionQuiet]. Also continues an install that
     * waited for "Install unknown apps". Returns true when a check started.
     */
    fun maybeAutoCheck(sessionQuiet: Boolean): Boolean {
        resumeAfterPermission()
        if (!sessionQuiet || job?.isActive == true || _state.value != UpdateState.Idle) return false
        if (!UpdatePolicy.autoCheckDue(clock(), prefs.lastUpdateCheckMillis, prefs.autoCheckUpdates, supported)) return false
        check(manual = false)
        return true
    }

    /** About › Check for updates. Answers with [UpdateState.UpToDate], [UpdateState.CheckFailed] or the dialog. */
    fun checkNow() {
        if (!supported || job?.isActive == true) return
        if (_state.value.showsDialog) return
        check(manual = true)
    }

    private fun check(manual: Boolean) {
        val now = clock()
        prefs.lastUpdateCheckMillis = now
        _lastCheck.value = now
        if (manual) _state.value = UpdateState.Checking
        job = scope.launch {
            val outcome = withContext(io) { source.latest() }
            log("check (${if (manual) "manual" else "automatic"}): $outcome")
            if (outcome is CheckOutcome.Newer) _newer.value = outcome.release
            _state.value = when (outcome) {
                is CheckOutcome.Newer ->
                    if (UpdatePolicy.shouldOffer(outcome.release, current, prefs.ignoredUpdateTag, manual)) UpdateState.Available(outcome.release)
                    else UpdateState.Idle
                CheckOutcome.UpToDate -> if (manual) UpdateState.UpToDate else UpdateState.Idle
                CheckOutcome.RateLimited -> if (manual) UpdateState.CheckFailed(rateLimited = true) else UpdateState.Idle
                is CheckOutcome.Failed -> if (manual) UpdateState.CheckFailed(rateLimited = false) else UpdateState.Idle
                CheckOutcome.NotFound -> if (manual) UpdateState.CheckFailed(rateLimited = false, notFound = true) else UpdateState.Idle
            }
        }
    }

    /**
     * The report sheet opened: find out quietly whether a newer release exists (fills [newer], never
     * opens the dialog), unless one is known already. Old versions sent reports fixed long before (#78-#80).
     */
    fun refreshNewer() {
        if (!supported || _newer.value != null || job?.isActive == true) return
        job = scope.launch {
            val outcome = withContext(io) { source.latest() }
            log("check (report): $outcome")
            if (outcome is CheckOutcome.Newer) _newer.value = outcome.release
        }
    }

    /** "Update first" on the report sheet: offer [newer] in the update dialog, even when its tag was ignored. */
    fun offerNewer() {
        val release = _newer.value ?: return
        if (job?.isActive == true || _state.value.showsDialog) return
        _state.value = UpdateState.Available(release)
    }

    /** "Later": close until the next check window. */
    fun later() {
        if (_state.value is UpdateState.Available) _state.value = UpdateState.Idle
    }

    /** "Ignore this version": never offer this tag again automatically; a newer tag is offered. */
    fun ignore() {
        val available = _state.value as? UpdateState.Available ?: return
        prefs.ignoredUpdateTag = available.release.tag
        _state.value = UpdateState.Idle
    }

    /** Close an answer or a failure. */
    fun dismiss() {
        when (_state.value) {
            UpdateState.UpToDate, is UpdateState.CheckFailed, is UpdateState.Failed -> { _manualApk.value = null; _state.value = UpdateState.Idle }
            // Android's install screen is up (or never came); closing ours leaves the decision there.
            // The verified APK stays in Downloads/CarLink for a later manual install.
            is UpdateState.Installing -> { _manualApk.value = null; _state.value = UpdateState.Idle }
            is UpdateState.Available -> later()
            else -> Unit
        }
    }

    /** "Download & install" (and "Try again" after a failure that allows it). */
    fun download() {
        val release = when (val s = _state.value) {
            is UpdateState.Available -> s.release
            is UpdateState.Failed -> s.release.takeIf { s.error.canRetry }
            else -> null
        } ?: return
        if (job?.isActive == true) return
        val file = File(downloadDir(), ReleaseRepo.apkName(release.version))
        _manualApk.value = null
        _state.value = UpdateState.Downloading(release, 0L, release.apkSize.takeIf { it > 0 })
        job = scope.launch {
            try {
                val checksum = withContext(io) { http.getText(release.checksumUrl) }
                val expected = checksum.takeIf { it.code in 200..299 }?.body?.let(ReleaseParser::parseChecksum)
                if (expected == null) {
                    fail(release, UpdateError.CHECKSUM_MISSING, file)
                    return@launch
                }
                val context = coroutineContext
                var lastPercent = -1L
                withContext(io) {
                    http.download(release.apkUrl, file, isCancelled = { !context.isActive }) { bytes, total ->
                        val percent = if (total != null && total > 0) bytes * 100 / total else bytes / (512 * 1024)
                        if (percent != lastPercent) {
                            lastPercent = percent
                            _state.update { s ->
                                if (s is UpdateState.Downloading && s.release == release) s.copy(bytes = bytes, total = total ?: s.total) else s
                            }
                        }
                    }
                }
                _state.value = UpdateState.Verifying(release)
                val failure = withContext(io) { verifier.verify(file, expected) }
                if (failure != null) {
                    log("verification failed: $failure")
                    fail(release, failure.toError(), file)
                    return@launch
                }
                verifiedFile = file
                _manualApk.value = file
                installOrAsk(release, file)
            } catch (cancelled: CancellationException) {
                file.delete()
                throw cancelled
            } catch (_: DownloadCancelledException) {
                file.delete()
            } catch (e: IOException) {
                log("download failed: ${e.message}")
                fail(release, UpdateError.DOWNLOAD, file, e.message)
            }
        }
    }

    /** Cancel a download or a pending permission; the offer stays open. */
    fun cancel() {
        val release = when (val s = _state.value) {
            is UpdateState.Downloading -> s.release
            is UpdateState.Verifying -> s.release
            is UpdateState.NeedsPermission -> s.release
            else -> return
        }
        job?.cancel()
        verifiedFile?.delete()
        verifiedFile = null
        _manualApk.value = null
        _state.value = UpdateState.Available(release)
    }

    /** Opens Android's "Install unknown apps" page; the install continues when Home resumes. */
    fun openInstallPermission() = installer.openInstallPermissionSettings()

    /**
     * "Install manually": hands the kept, verified APK to Android's installer (ACTION_VIEW on a
     * content:// URI), or to a file manager. When no app takes the file, opens its folder; when that
     * fails too the dialog's path line is what is left. The APK is never deleted by this.
     */
    fun installManually(): ManualInstall {
        val file = _manualApk.value ?: return ManualInstall.NONE
        if (!file.isFile) { _manualApk.value = null; return ManualInstall.NONE }
        if (installer.openForManualInstall(file)) {
            log("manual install: opened ${file.name}")
            return ManualInstall.INSTALLER
        }
        if (installer.openContainingFolder(file)) {
            log("manual install: no installer took ${file.name}; opened its folder")
            return ManualInstall.FOLDER
        }
        log("manual install: nothing could be opened for ${file.name}")
        return ManualInstall.NONE
    }

    private fun resumeAfterPermission() {
        val waiting = _state.value as? UpdateState.NeedsPermission ?: return
        val file = verifiedFile ?: return
        if (!installer.canInstall()) return
        job = scope.launch { installOrAsk(waiting.release, file) }
    }

    private suspend fun installOrAsk(release: ReleaseInfo, file: File) {
        if (!installer.canInstall()) {
            _state.value = UpdateState.NeedsPermission(release)
            return
        }
        awaitSessionQuiet()
        _state.value = UpdateState.Installing(release)
        try {
            withContext(io) { installer.install(file) }
            // The session holds its own copy now; the verified APK stays in Downloads/CarLink.
            verifiedFile = null
        } catch (e: IOException) {
            log("install failed: ${e.message}")
            // The file passed every check: keep it, so it can be installed by hand.
            verifiedFile = null
            fail(release, UpdateError.INSTALL, null, e.message, keepApk = file)
        }
    }

    /** The PackageInstaller session's answer (UpdateInstallReceiver). */
    fun onInstallStatus(status: InstallStatus) {
        val installing = _state.value as? UpdateState.Installing ?: run {
            // The dialog was closed while Android's install screen was up: only tidy up.
            if (status !is InstallStatus.WaitingForUser) verifiedFile = null
            if (status == InstallStatus.Success) _manualApk.value = null
            return
        }
        log("install: $status")
        when (status) {
            InstallStatus.WaitingForUser -> Unit
            // Usually not seen: Android stops CarLink to replace it.
            InstallStatus.Success -> { verifiedFile = null; _manualApk.value = null; _state.value = UpdateState.Idle }
            // A verified APK is kept in Downloads/CarLink when Android's install did not finish,
            // and the Failed dialog offers "Install manually" for it.
            InstallStatus.Aborted -> { verifiedFile = null; fail(installing.release, UpdateError.INSTALL_CANCELLED, null, keepApk = _manualApk.value) }
            is InstallStatus.Failure -> { verifiedFile = null; fail(installing.release, UpdateError.INSTALL, null, status.message, keepApk = _manualApk.value) }
        }
    }

    /** [keepApk]: the verified APK an install failure leaves for the manual install; anything else deletes its file. */
    private fun fail(release: ReleaseInfo, error: UpdateError, file: File?, detail: String? = null, keepApk: File? = null) {
        file?.delete()
        deleteVerified()
        _manualApk.value = keepApk?.takeIf { it.isFile }
        _state.value = UpdateState.Failed(release, error, detail)
    }

    private fun deleteVerified() {
        verifiedFile?.delete()
        verifiedFile = null
    }
}
