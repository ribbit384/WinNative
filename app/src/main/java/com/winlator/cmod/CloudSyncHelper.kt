package com.winlator.cmod

import android.content.Context
import com.winlator.cmod.container.Shortcut
import com.winlator.cmod.epic.service.EpicCloudSavesManager
import com.winlator.cmod.gog.service.GOGService
import com.winlator.cmod.steam.enums.PathType
import com.winlator.cmod.steam.enums.SaveLocation
import com.winlator.cmod.steam.enums.SyncResult
import com.winlator.cmod.steam.service.SteamService
import com.winlator.cmod.steam.utils.PrefManager
import kotlinx.coroutines.runBlocking
import timber.log.Timber

object CloudSyncHelper {
    @JvmStatic
    fun forceDownloadOnContainerSwap(context: Context, shortcut: Shortcut): Boolean {
        val forceFlag = shortcut.getExtra("cloud_force_download")
        if (forceFlag.isEmpty()) return false

        val result = runBlocking {
            when (shortcut.getExtra("game_source")) {
                "STEAM" -> forceSteamDownload(context, shortcut)
                "GOG" -> forceGogDownload(context, shortcut)
                "EPIC" -> forceEpicDownload(context, shortcut)
                else -> false
            }
        }

        if (result) {
            shortcut.putExtra("cloud_force_download", null)
            shortcut.saveData()
        }

        Timber.i(
            "Force cloud download for %s (source=%s): %s",
            shortcut.name,
            shortcut.getExtra("game_source"),
            result
        )
        return result
    }

    private suspend fun forceSteamDownload(context: Context, shortcut: Shortcut): Boolean {
        val appId = shortcut.getExtra("app_id").toIntOrNull() ?: return false
        return try {
            val accountId = SteamService.userSteamId?.accountID?.toLong()
                ?: PrefManager.steamUserAccountId.takeIf { it != 0 }?.toLong()
                ?: 0L
            val prefixToPath: (String) -> String = { prefix ->
                PathType.from(prefix).toAbsPath(context, appId, accountId)
            }

            val syncInfo = SteamService.forceSyncUserFiles(
                appId = appId,
                prefixToPath = prefixToPath,
                preferredSave = SaveLocation.Remote,
                overrideLocalChangeNumber = -1
            ).await()

            syncInfo?.syncResult == SyncResult.Success
        } catch (e: Exception) {
            Timber.e(e, "Failed to force Steam cloud download for appId=%d", appId)
            false
        }
    }

    private suspend fun forceGogDownload(context: Context, shortcut: Shortcut): Boolean {
        val rawAppId = shortcut.getExtra("app_id").ifEmpty { shortcut.getExtra("gog_id") }
        if (rawAppId.isEmpty()) return false
        val appId = if (rawAppId.startsWith("GOG_", ignoreCase = true)) rawAppId else "GOG_$rawAppId"
        return try {
            GOGService.syncCloudSaves(context, appId, "download")
        } catch (e: Exception) {
            Timber.e(e, "Failed to force GOG cloud download for appId=%s", appId)
            false
        }
    }

    private suspend fun forceEpicDownload(context: Context, shortcut: Shortcut): Boolean {
        val appId = shortcut.getExtra("app_id").toIntOrNull() ?: return false
        return try {
            EpicCloudSavesManager.syncCloudSaves(context, appId, "download")
        } catch (e: Exception) {
            Timber.e(e, "Failed to force Epic cloud download for appId=%d", appId)
            false
        }
    }
}
