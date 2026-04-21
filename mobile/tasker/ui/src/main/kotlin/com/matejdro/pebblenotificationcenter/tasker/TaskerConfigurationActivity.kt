package com.matejdro.pebblenotificationcenter.tasker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import com.matejdro.pebblenotificationcenter.common.di.NavigationInjectingApplication
import com.matejdro.pebblenotificationcenter.ui.theme.NotificationCenterTheme
import si.inova.kotlinova.navigation.di.NavigationContext
import si.inova.kotlinova.navigation.di.NavigationInjection
import si.inova.kotlinova.navigation.navigation3.NavDisplay
import si.inova.kotlinova.navigation.screenkeys.ScreenKey

@Stable
abstract class TaskerConfigurationActivity : ComponentActivity() {
   private lateinit var navigationInjectionFactory: NavigationInjection.Factory
   private lateinit var navigationContext: NavigationContext

   lateinit var existingData: Bundle

   protected abstract fun getInitialHistory(): List<ScreenKey>

   override fun onCreate(savedInstanceState: Bundle?) {
      val appGraph = (requireNotNull(application) as NavigationInjectingApplication).applicationGraph
      navigationInjectionFactory = appGraph.getNavigationInjectionFactory()
      navigationContext = appGraph.getNavigationContext()

      super.onCreate(savedInstanceState)

      enableEdgeToEdge()

      existingData = intent?.getBundleExtra(TaskerPluginConstants.EXTRA_BUNDLE) ?: Bundle()

      setContent {
         NavigationRoot(getInitialHistory())
      }
   }

   @Composable
   private fun NavigationRoot(initialHistory: List<ScreenKey>) {
      NotificationCenterTheme {
         Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
         ) {
            navigationInjectionFactory.NavDisplay(
               initialHistory = { initialHistory },
               entryDecorators = listOf(
                  rememberSaveableStateHolderNavEntryDecorator(),
                  NavEntryDecorator<ScreenKey>(
                     decorate = { entry ->
                        Surface {
                           entry.Content()
                        }
                     }
                  )
               ),
            )
         }
      }
   }

   fun clearConfiguration() {
      existingData = Bundle()
      setResult(Activity.RESULT_CANCELED)
   }

   fun saveConfiguration(bundle: Bundle, message: String) {
      val intent = Intent().apply {
         putExtra(TaskerPluginConstants.EXTRA_STRING_BLURB, message)
         putExtra(TaskerPluginConstants.EXTRA_BUNDLE, bundle)
      }

      setResult(RESULT_OK, intent)
   }
}
