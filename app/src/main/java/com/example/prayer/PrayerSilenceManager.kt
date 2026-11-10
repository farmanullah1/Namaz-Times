package com.example.prayer

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.example.data.repository.SettingsRepository
import java.time.Instant

data class SilenceResult(
    val changed: Boolean,
    val message: String,
    val needsPolicyAccess: Boolean = false
)

/**
 * Optional, opt-in automatic silence. Never pretends the phone was silenced without policy access.
 * Only the app's own change is restored, and a user's newer manual Do Not Disturb choice wins.
 */
class PrayerSilenceManager(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val alarmScheduler: PrayerAlarmScheduler
) {

    private val notificationManager: NotificationManager? =
        context.getSystemService(NotificationManager::class.java)

    fun hasPolicyAccess(): Boolean {
        val manager = notificationManager ?: return false
        return manager.isNotificationPolicyAccessGranted
    }

    fun openPolicyAccessSettings(): Intent =
        Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    suspend fun silenceForPrayer(): SilenceResult {
        val settings = settingsRepository.current()
        if (!settings.prayerSilenceEnabled) {
            return SilenceResult(false, "Automatic prayer silence is off.")
        }
        if (!hasPolicyAccess()) {
            return SilenceResult(
                false,
                "Do Not Disturb access is not granted. The phone was not silenced.",
                needsPolicyAccess = true
            )
        }
        val manager = notificationManager ?: return SilenceResult(false, "Notification service unavailable.")
        val currentFilter = manager.currentInterruptionFilter
        if (currentFilter == NotificationManager.INTERRUPTION_FILTER_PRIORITY ||
            currentFilter == NotificationManager.INTERRUPTION_FILTER_NONE
        ) {
            return SilenceResult(false, "Do Not Disturb is already active; leaving your own setting untouched.")
        }
        manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        val restoreAt = System.currentTimeMillis() + settings.prayerSilenceMinutes * 60_000L
        settingsRepository.setDndState(
            previousFilter = currentFilter,
            silencedAt = System.currentTimeMillis(),
            restoreAt = restoreAt
        )
        alarmScheduler.scheduleSilenceRestore(restoreAt)
        return SilenceResult(
            true,
            "Phone silenced for ${settings.prayerSilenceMinutes} minutes. It will be restored automatically."
        )
    }

    /**
     * Restores the previous interruption filter only when the app's silence is still in effect.
     * If the user changed DND in the meantime, the newer choice is preserved.
     */
    suspend fun restoreIfDue(force: Boolean = false): Boolean {
        val settings = settingsRepository.current()
        val previous = settings.previousDndFilter ?: return false
        val restoreAt = settings.dndRestoreAtMillis ?: return false
        if (!force && Instant.now().toEpochMilli() < restoreAt) return false

        val manager = notificationManager ?: return false
        val current = manager.currentInterruptionFilter
        val appOwned = current == NotificationManager.INTERRUPTION_FILTER_PRIORITY
        if (appOwned) {
            manager.setInterruptionFilter(previous)
        }
        settingsRepository.setDndState(previousFilter = null, silencedAt = null, restoreAt = null)
        return appOwned
    }

    suspend fun restoreNow(): Boolean = restoreIfDue(force = true)

    fun canSilence(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && hasPolicyAccess()
}
