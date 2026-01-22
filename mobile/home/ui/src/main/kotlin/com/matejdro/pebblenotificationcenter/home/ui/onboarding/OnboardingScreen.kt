package com.matejdro.pebblenotificationcenter.home.ui.onboarding

import android.Manifest
import android.companion.AssociationInfo
import android.companion.AssociationRequest
import android.companion.CompanionDeviceManager
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.matejdro.pebblenotificationcenter.home.ui.R
import com.matejdro.pebblenotificationcenter.home.ui.R.string.grant
import com.matejdro.pebblenotificationcenter.navigation.keys.HomeScreenKey
import com.matejdro.pebblenotificationcenter.navigation.keys.OnboardingKey
import com.matejdro.pebblenotificationcenter.navigation.keys.RuleListScreenKey
import com.matejdro.pebblenotificationcenter.notification.NotificationServiceStatus
import com.matejdro.pebblenotificationcenter.ui.debugging.FullScreenPreviews
import com.matejdro.pebblenotificationcenter.ui.debugging.PreviewTheme
import si.inova.kotlinova.navigation.instructions.ReplaceBackstack
import si.inova.kotlinova.navigation.navigator.Navigator
import si.inova.kotlinova.navigation.screens.InjectNavigationScreen
import si.inova.kotlinova.navigation.screens.Screen

@InjectNavigationScreen
class OnboardingScreen(
   private val navigator: Navigator,
   private val serviceStatus: NotificationServiceStatus,
) : Screen<OnboardingKey>() {
   @Composable
   override fun Content(key: OnboardingKey) {
      OnboardingContent(
         serviceStatus,
         {
            navigator.navigate(
               ReplaceBackstack(
                  HomeScreenKey,
                  RuleListScreenKey,
               )
            )
         }
      )
   }
}

@Composable
private fun OnboardingContent(
   serviceStatus: NotificationServiceStatus,
   continueToApp: () -> Unit,
) {
   Column(
      Modifier
         .fillMaxSize()
         .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
   ) {
      Column(
         modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .weight(1f)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)),
         verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
         OnboardingScrollContent(serviceStatus)
      }

      Surface(
         modifier = Modifier
            .fillMaxWidth(),
         color = MaterialTheme.colorScheme.secondaryContainer,
         shadowElevation = 16.dp
      ) {
         Button(
            onClick = continueToApp,
            Modifier
               .wrapContentWidth()
               .padding(16.dp)
         ) {
            Text(stringResource(R.string.continue_to_the_app))
         }
      }
   }
}

@Composable
private fun ColumnScope.OnboardingScrollContent(serviceStatus: NotificationServiceStatus) {
   Text(stringResource(R.string.onboarding_title))
   Text(stringResource(R.string.onboarding_intro))

   if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      NotificationPermission()
   }
   if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      NotificationAccessPermissionCompanion(serviceStatus)
   } else {
      NotificationAccessPermissionLegacy(serviceStatus)
   }
}

@Composable
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun NotificationPermission() {
   var rejectedPermission by remember { mutableStateOf(false) }
   val permissionState = rememberPermissionState(
      Manifest.permission.POST_NOTIFICATIONS,
   ) {
      if (!it) {
         rejectedPermission = true
      }
   }

   Card(Modifier.fillMaxWidth()) {
      Column(
         Modifier.padding(8.dp),
         verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
         Text(stringResource(R.string.notifications_permission_title), style = MaterialTheme.typography.headlineSmall)
         Text(stringResource(R.string.notification_permission_description))

         SinglePermissionButton(permissionState, rejectedPermission)
      }
   }
}

@Composable
@RequiresApi(Build.VERSION_CODES.O)
private fun NotificationAccessPermissionCompanion(
   serviceStatus: NotificationServiceStatus,
) {
   var permissionGranted by remember { mutableStateOf(false) }
   val context = LocalContext.current

   val companionManager = remember(context) { context.getSystemService<CompanionDeviceManager>()!! }

   LifecycleResumeEffect(serviceStatus) {
      permissionGranted = serviceStatus.isPermissionGranted()

      onPauseOrDispose { }
   }

   fun associateWithCompanionManager() {
      companionManager.associate(
         AssociationRequest.Builder()
            .build(),
         object : CompanionDeviceManager.Callback() {
            override fun onFailure(error: CharSequence?) {}

            // New method is only available in the SDK 33, so we
            // have to use the old one for now.
            @Suppress("DEPRECATION")
            @Deprecated("Deprecated in Java")
            override fun onDeviceFound(intentSender: IntentSender) {
               super.onDeviceFound(intentSender)
               context.startIntentSender(intentSender, null, 0, 0, 0)
            }

            override fun onAssociationCreated(associationInfo: AssociationInfo) {
               serviceStatus.requestNotificationAccess()
            }
         },
         null
      )
   }
   Card(Modifier.fillMaxWidth()) {
      Column(
         Modifier.padding(8.dp),
         verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
         Text(stringResource(R.string.permission_notification_access_title), style = MaterialTheme.typography.headlineSmall)
         Text(stringResource(R.string.permission_notification_access_companion_description))

         if (permissionGranted) {
            Text("✅")
         } else {
            Button(onClick = { associateWithCompanionManager() }) { Text(stringResource(grant)) }
         }
      }
   }
}

@Composable
private fun NotificationAccessPermissionLegacy(
   serviceStatus: NotificationServiceStatus,
) {
   var permissionGranted by remember { mutableStateOf(serviceStatus.isPermissionGranted()) }

   LifecycleResumeEffect(serviceStatus) {
      permissionGranted = serviceStatus.isPermissionGranted()

      onPauseOrDispose { }
   }

   Card(Modifier.fillMaxWidth()) {
      Column(
         Modifier.padding(8.dp),
         verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
         Text(stringResource(R.string.permission_notification_access_title), style = MaterialTheme.typography.headlineSmall)
         Text(stringResource(R.string.permission_notification_access_legacy_description))

         if (permissionGranted) {
            Text("✅")
         } else {
            Button(onClick = { serviceStatus.requestNotificationAccess() }) { Text(stringResource(grant)) }
         }
      }
   }
}

@Composable
private fun SinglePermissionButton(
   permissionState: PermissionState,
   rejectedPermission: Boolean,
) {
   val context = LocalContext.current

   if (permissionState.status == PermissionStatus.Granted) {
      Text("✅")
   } else if (rejectedPermission) {
      Button(
         onClick = {
            openSystemPermissionSettings(context)
         }
      ) { Text(stringResource(R.string.open_settings)) }
   } else {
      Button(onClick = { permissionState.launchPermissionRequest() }) { Text(stringResource(R.string.grant)) }
   }
}

private fun openSystemPermissionSettings(context: Context) {
   context.startActivity(
      Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
         .setData(
            Uri.fromParts("package", context.getPackageName(), null)
         )
   )
}

@FullScreenPreviews
@Composable
@ShowkaseComposable(group = "test")
internal fun OnboardingContentWithWatchPairedPreview() {
   PreviewTheme {
      OnboardingContent(FAKE_SERVICE_STATUS, {})
   }
}

@Preview(heightDp = 1200)
@Composable
private fun OnboardingWholeListPreview() {
   PreviewTheme {
      Column(
         Modifier.padding(8.dp),
         verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
         OnboardingScrollContent(FAKE_SERVICE_STATUS)
      }
   }
}

private val FAKE_SERVICE_STATUS = object : NotificationServiceStatus {
   override fun isEnabled(): Boolean {
      return false
   }

   override fun isPermissionGranted(): Boolean {
      return false
   }

   override fun requestNotificationAccess() {}
}
