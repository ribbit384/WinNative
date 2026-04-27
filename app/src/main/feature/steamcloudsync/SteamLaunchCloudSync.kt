package com.winlator.cmod.feature.steamcloudsync

import android.app.Activity
import com.winlator.cmod.R
import com.winlator.cmod.feature.sync.google.GameSaveBackupManager
import com.winlator.cmod.feature.sync.google.GoogleAuthMode
import com.winlator.cmod.runtime.container.Shortcut
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.util.concurrent.CountDownLatch

object SteamLaunchCloudSync {
    fun interface StatusSink {
        fun show(text: String)
    }

    /**
     * Steam provider cloud saves need launch-time reconciliation. This is Steam-only
     * and never starts Google Play Games or Drive consent.
     */
    @JvmStatic
    fun syncBeforeLaunch(
        activity: Activity,
        shortcut: Shortcut?,
        cloudSyncEnabled: Boolean,
        statusSink: StatusSink,
    ) {
        if (shortcut == null) return
        if (shortcut.getExtra("game_source") != "STEAM") return
        if (!cloudSyncEnabled || SteamCloudSyncHelper.isOfflineMode(shortcut)) return

        SteamCloudSyncHelper.forceDownloadOnContainerSwap(activity, shortcut)

        if (!SteamCloudSyncHelper.hasLocalCloudSaves(activity, shortcut)) {
            statusSink.show(activity.getString(R.string.preloader_downloading_cloud))
            SteamCloudSyncHelper.downloadCloudSaves(activity, shortcut)
            statusSink.show(activity.getString(R.string.preloader_initializing))
            return
        }

        if (!SteamCloudSyncHelper.cloudSavesDiffer(activity, shortcut)) return

        val dialogLatch = CountDownLatch(1)
        var useCloud = false
        var keepBackup = false
        val timestamps = SteamCloudSyncHelper.getConflictTimestamps(activity, shortcut)
        activity.runOnUiThread {
            CloudSyncConflictDialog.show(
                activity,
                timestamps,
                onUseCloud = { keep ->
                    useCloud = true
                    keepBackup = keep
                    dialogLatch.countDown()
                },
                onUseLocal = { keep ->
                    useCloud = false
                    keepBackup = keep
                    dialogLatch.countDown()
                },
            )
        }

        try {
            dialogLatch.await()
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            return
        }

        if (!useCloud) return
        if (keepBackup) {
            backupDiscardedSave(activity, shortcut, GameSaveBackupManager.BackupOrigin.LOCAL)
        }
        statusSink.show(activity.getString(R.string.preloader_syncing_cloud))
        SteamCloudSyncHelper.downloadCloudSaves(activity, shortcut)
        statusSink.show(activity.getString(R.string.preloader_initializing))
    }

    private fun backupDiscardedSave(
        activity: Activity,
        shortcut: Shortcut,
        origin: GameSaveBackupManager.BackupOrigin,
    ) {
        val gameId = shortcut.getExtra("app_id").takeIf { it.isNotEmpty() } ?: return
        val gameName = shortcut.name ?: "Unknown"
        try {
            val result =
                runBlocking(Dispatchers.IO) {
                    GameSaveBackupManager.backupDiscardedSave(
                        activity = activity,
                        gameSource = GameSaveBackupManager.GameSource.STEAM,
                        gameId = gameId,
                        gameName = gameName,
                        origin = origin,
                        authMode = GoogleAuthMode.SILENT,
                    )
                }
            Timber.tag("SteamLaunchCloudSync").i("Discarded Steam save backup: %s", result.message)
        } catch (e: Exception) {
            Timber.tag("SteamLaunchCloudSync").w(e, "Failed to back up discarded Steam save")
        }
    }
}
