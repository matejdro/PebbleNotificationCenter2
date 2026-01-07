package com.matejdro.pebblenotificationcenter

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.DialogSceneStrategy
import com.matejdro.pebblenotificationcenter.navigation.scenes.TabListDetailScene
import com.matejdro.pebblenotificationcenter.navigation.scenes.rememberTabListDetailSceneStrategy
import com.matejdro.pebblenotificationcenter.notification.NotificationServiceStatus
import com.matejdro.pebblenotificationcenter.ui.theme.NotificationCenterTheme
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import si.inova.kotlinova.compose.result.LocalResultPassingStore
import si.inova.kotlinova.compose.result.ResultPassingStore
import si.inova.kotlinova.compose.time.ComposeAndroidDateTimeFormatter
import si.inova.kotlinova.compose.time.LocalDateFormatter
import si.inova.kotlinova.core.time.AndroidDateTimeFormatter
import si.inova.kotlinova.navigation.deeplink.HandleNewIntentDeepLinks
import si.inova.kotlinova.navigation.deeplink.MainDeepLinkHandler
import si.inova.kotlinova.navigation.di.NavigationContext
import si.inova.kotlinova.navigation.di.NavigationInjection
import si.inova.kotlinova.navigation.navigation3.NavDisplay
import si.inova.kotlinova.navigation.screenkeys.ScreenKey
import kotlin.time.Duration.Companion.seconds

class MainActivity : ComponentActivity() {
   private lateinit var navigationInjectionFactory: NavigationInjection.Factory
   private lateinit var mainDeepLinkHandler: MainDeepLinkHandler
   private lateinit var navigationContext: NavigationContext
   private lateinit var dateFormatter: AndroidDateTimeFormatter
   private lateinit var mainViewModelFactory: MainViewModel.Factory

   private lateinit var notificationServiceStatus: NotificationServiceStatus

   private lateinit var tabListDetailSceneFactory: TabListDetailScene.Factory

   private val viewModel by viewModels<MainViewModel> { ViewModelFactory() }
   private var initComplete = false

   override fun onCreate(savedInstanceState: Bundle?) {
      val appGraph = (requireNotNull(application) as NotificationCenterApplication).applicationGraph

      navigationInjectionFactory = appGraph.getNavigationInjectionFactory()
      mainDeepLinkHandler = appGraph.getMainDeepLinkHandler()
      navigationContext = appGraph.getNavigationContext()
      dateFormatter = appGraph.getDateFormatter()
      mainViewModelFactory = appGraph.getMainViewModelFactory()
      tabListDetailSceneFactory = appGraph.getTabListDetailSceneFactory()
      notificationServiceStatus = appGraph.getNotificationServiceStatus()

      super.onCreate(savedInstanceState)
      enableEdgeToEdge()

      val splashScreen = installSplashScreen()
      splashScreen.setKeepOnScreenCondition { !initComplete }

      beginInitialisation(savedInstanceState == null)
   }

   private fun beginInitialisation(startup: Boolean) {
      lifecycleScope.launch {
         val initialHistory = viewModel.startingScreens.filterNotNull().first()

         val deepLinkTarget = if (startup) {
            intent?.data?.let { mainDeepLinkHandler.handleDeepLink(it, startup = true) }
         } else {
            null
         }

         val overridenInitialHistoryFromDeepLink = if (deepLinkTarget != null) {
            deepLinkTarget.performNavigation(initialHistory, navigationContext).newBackstack.toPersistentList()
         } else {
            initialHistory
         }

         setContent {
            NavigationRoot(overridenInitialHistoryFromDeepLink)
         }

         initComplete = true
      }
   }

   @Composable
   private fun NavigationRoot(initialHistory: List<ScreenKey>) {
      NotificationCenterTheme {
         // A surface container using the 'background' color from the theme
         Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
         ) {
            val resultPassingStore = rememberSaveable { ResultPassingStore() }
            CompositionLocalProvider(
               LocalDateFormatter provides ComposeAndroidDateTimeFormatter(dateFormatter),
               LocalResultPassingStore provides resultPassingStore
            ) {
               val backstack = navigationInjectionFactory.NavDisplay(
                  initialHistory = { initialHistory },
                  entryDecorators = listOf(
                     rememberSaveableStateHolderNavEntryDecorator(),
                     NavEntryDecorator<ScreenKey>(
                        decorate = {
                           Surface {
                              it.Content()
                           }
                        }
                     )
                  ),
                  sceneStrategy = rememberTabListDetailSceneStrategy(tabListDetailSceneFactory) then
                     remember { DialogSceneStrategy() }
               )

               mainDeepLinkHandler.HandleNewIntentDeepLinks(this@MainActivity, backstack)
            }
         }
      }
   }

   private inner class ViewModelFactory : ViewModelProvider.Factory {
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
         @Suppress("UNCHECKED_CAST")
         return mainViewModelFactory.create() as T
      }
   }

   override fun onStart() {
      super.onStart()

      lifecycleScope.launch {
         // Give the service some time to start
         delay(1.seconds)
         if (!notificationServiceStatus.isEnabled()) {
            showServiceDeadPopup()
         }
      }
   }

   private fun showServiceDeadPopup() {
      val builder = AlertDialog.Builder(this)

      builder.setTitle(getString(R.string.service_not_running)).setNegativeButton(getString(R.string.cancel), null)
      builder.setMessage(getString(R.string.notification_service_is_not_running_you_must_enable_it_to_get_this_app_to_work))
      builder.setPositiveButton(
         getString(R.string.open_settings)
      ) { _, _ -> startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }

      builder.show()
   }
}
