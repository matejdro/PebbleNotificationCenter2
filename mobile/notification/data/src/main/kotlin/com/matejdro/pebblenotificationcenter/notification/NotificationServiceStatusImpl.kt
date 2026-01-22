package com.matejdro.pebblenotificationcenter.notification

import android.app.NotificationManager
import android.companion.CompanionDeviceManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.content.getSystemService
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@Inject
@ContributesBinding(AppScope::class)
class NotificationServiceStatusImpl(private val context: Context) : NotificationServiceStatus {
   override fun isEnabled(): Boolean {
      return NotificationService.instance != null
   }

   @Suppress("CognitiveComplexMethod") // Lots of if statements for different SDK versions, can't help
   override fun isPermissionGranted(): Boolean {
      // Sigh Google, did you really have to change this so many times?

      return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
         context.getSystemService<CompanionDeviceManager>()
            ?.myAssociations?.isNotEmpty() == true &&
            context.getSystemService<NotificationManager>()
               ?.isNotificationListenerAccessGranted(getListenerComponent()) == true
      } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
         @Suppress("DEPRECATION")
         context.getSystemService<CompanionDeviceManager>()
            ?.associations?.isNotEmpty() == true &&
            context.getSystemService<NotificationManager>()
               ?.isNotificationListenerAccessGranted(getListenerComponent()) == true
      } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
         val companionManager = context.getSystemService<CompanionDeviceManager>()
         @Suppress("DEPRECATION")
         companionManager
            ?.associations?.isNotEmpty() == true && companionManager.hasNotificationAccess(getListenerComponent())
      } else {
         val enabledNotificationListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
         )

         if (!enabledNotificationListeners.isNullOrEmpty()) {
            val names = enabledNotificationListeners.split(":")
            for (name in names) {
               val cn = ComponentName.unflattenFromString(name)
               if (cn != null && cn.packageName == context.packageName) {
                  return true
               }
            }
            false
         } else {
            false
         }
      }
   }

   override fun requestNotificationAccess() {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
         context.getSystemService<CompanionDeviceManager>()
            ?.requestNotificationAccess(getListenerComponent())
      } else {
         context.startActivity(
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
               .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
         )
      }
   }

   private fun getListenerComponent(): ComponentName = ComponentName(context, NotificationService::class.java)
}
