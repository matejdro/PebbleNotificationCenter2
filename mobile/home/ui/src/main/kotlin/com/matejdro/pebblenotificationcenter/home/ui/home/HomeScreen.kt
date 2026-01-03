package com.matejdro.pebblenotificationcenter.home.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.matejdro.pebblenotificationcenter.home.ui.R
import com.matejdro.pebblenotificationcenter.navigation.instructions.ReplaceTabContentWith
import com.matejdro.pebblenotificationcenter.navigation.keys.HistoryScreenKey
import com.matejdro.pebblenotificationcenter.navigation.keys.HomeScreenKey
import com.matejdro.pebblenotificationcenter.navigation.keys.RuleListScreenKey
import com.matejdro.pebblenotificationcenter.navigation.keys.ToolsScreenKey
import com.matejdro.pebblenotificationcenter.navigation.keys.base.LocalSelectedTabContent
import com.matejdro.pebblenotificationcenter.navigation.keys.base.SelectedTabContent
import com.matejdro.pebblenotificationcenter.ui.debugging.FullScreenPreviews
import com.matejdro.pebblenotificationcenter.ui.debugging.PreviewTheme
import si.inova.kotlinova.core.activity.requireActivity
import si.inova.kotlinova.navigation.instructions.NavigationInstruction
import si.inova.kotlinova.navigation.navigator.Navigator
import si.inova.kotlinova.navigation.screenkeys.ScreenKey
import si.inova.kotlinova.navigation.screens.InjectNavigationScreen
import si.inova.kotlinova.navigation.screens.Screen

@InjectNavigationScreen
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
class HomeScreen(
   private val navigator: Navigator,
) : Screen<HomeScreenKey>() {
   @Composable
   override fun Content(key: HomeScreenKey) {
      val sizeClass = calculateWindowSizeClass(activity = LocalContext.current.requireActivity())

      HomeScreenContent(
         LocalSelectedTabContent.current,
         sizeClass.widthSizeClass == WindowWidthSizeClass.Expanded,
         navigator::navigate
      )
   }
}

@Composable
private fun HomeScreenContent(
   selectedScreen: SelectedTabContent,
   tabletMode: Boolean,
   navigate: (NavigationInstruction) -> Unit,
) {
   if (tabletMode) {
      NavigationRailContent(selectedScreen.content, selectedScreen.key, navigate)
   } else {
      NavigationBarContent(selectedScreen.content, selectedScreen.key, navigate)
   }
}

@Composable
private fun NavigationBarContent(
   mainContent: @Composable () -> Unit,
   selectedScreenKey: ScreenKey,
   navigate: (NavigationInstruction) -> Unit,
) {
   Column {
      Box(
         Modifier
            .fillMaxWidth()
            .weight(1f)
            .consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
      ) {
         mainContent()
      }

      NavigationBar {
         NavigationBarItem(
            selected = selectedScreenKey is RuleListScreenKey,
            onClick = { navigate(ReplaceTabContentWith(RuleListScreenKey)) },
            icon = { Icon(painter = painterResource(id = R.drawable.rule), contentDescription = null) },
            label = { Text(stringResource(R.string.rules)) }
         )

         NavigationBarItem(
            selected = selectedScreenKey is HistoryScreenKey,
            onClick = { navigate(ReplaceTabContentWith(HistoryScreenKey)) },
            icon = { Icon(painter = painterResource(id = R.drawable.history), contentDescription = null) },
            label = { Text(stringResource(R.string.history)) }
         )

         NavigationBarItem(
            selected = selectedScreenKey is ToolsScreenKey,
            onClick = { navigate(ReplaceTabContentWith(ToolsScreenKey)) },
            icon = { Icon(painter = painterResource(id = R.drawable.tools), contentDescription = null) },
            label = { Text(stringResource(R.string.tools)) }
         )
      }
   }
}

@Composable
private fun NavigationRailContent(
   mainContent: @Composable () -> Unit,
   selectedScreenKey: ScreenKey,
   navigate: (NavigationInstruction) -> Unit,
) {
   Row {
      NavigationRail {
         NavigationRailItem(
            selected = selectedScreenKey is RuleListScreenKey,
            onClick = { navigate(ReplaceTabContentWith(RuleListScreenKey)) },
            icon = { Icon(painter = painterResource(id = R.drawable.rule), contentDescription = null) },
            label = { Text(stringResource(R.string.rules)) }
         )

         NavigationRailItem(
            selected = selectedScreenKey is HistoryScreenKey,
            onClick = { navigate(ReplaceTabContentWith(HistoryScreenKey)) },
            icon = { Icon(painter = painterResource(id = R.drawable.history), contentDescription = null) },
            label = { Text(stringResource(R.string.history)) }
         )

         NavigationRailItem(
            selected = selectedScreenKey is ToolsScreenKey,

            onClick = { navigate(ReplaceTabContentWith(ToolsScreenKey)) },
            icon = { Icon(painter = painterResource(id = R.drawable.tools), contentDescription = null) },
            label = { Text(stringResource(R.string.tools)) }
         )
      }

      Box(
         Modifier
            .fillMaxHeight()
            .weight(1f)
      ) {
         mainContent()
      }
   }
}

@FullScreenPreviews
@Composable
@ShowkaseComposable(group = "Test")
internal fun HomePhonePreview() {
   PreviewTheme {
      HomeScreenContent(
         tabletMode = false,
         selectedScreen = SelectedTabContent(
            {
               Box(
                  Modifier
                     .fillMaxSize()
                     .background(Color.Red)
               )
            },
            HistoryScreenKey,
         ),
         navigate = {},
      )
   }
}

@Preview(device = Devices.TABLET)
@Composable
@ShowkaseComposable(group = "Test")
internal fun HomeTabletPreview() {
   PreviewTheme {
      HomeScreenContent(
         tabletMode = true,
         selectedScreen = SelectedTabContent(
            {
               Box(
                  Modifier
                     .fillMaxSize()
                     .background(Color.Red)
               )
            },
            RuleListScreenKey,
         ),
         navigate = {},
      )
   }
}
