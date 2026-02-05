package com.matejdro.notificationcenter.rules.ui.list

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.matejdro.notificationcenter.rules.RuleMetadata
import com.matejdro.pebblenotificationcenter.navigation.keys.RuleListScreenKey
import com.matejdro.pebblenotificationcenter.ui.components.ProgressErrorSuccessScaffold
import com.matejdro.pebblenotificationcenter.ui.debugging.FullScreenPreviews
import com.matejdro.pebblenotificationcenter.ui.debugging.PreviewTheme
import si.inova.kotlinova.compose.components.itemsWithDivider
import si.inova.kotlinova.compose.flow.collectAsStateWithLifecycleAndBlinkingPrevention
import si.inova.kotlinova.navigation.screens.InjectNavigationScreen
import si.inova.kotlinova.navigation.screens.Screen

@InjectNavigationScreen
class RuleListScreen(
   private val viewModel: RuleListViewModel,
) : Screen<RuleListScreenKey>() {
   @Composable
   override fun Content(key: RuleListScreenKey) {
      val stateOutcome = viewModel.uiState.collectAsStateWithLifecycleAndBlinkingPrevention().value

      ProgressErrorSuccessScaffold(stateOutcome, modifier = Modifier.safeDrawingPadding()) { state ->
         RuleListScreenContent(state)
      }
   }
}

@Composable
private fun RuleListScreenContent(
   state: RuleListState,
) {
   Scaffold(
      Modifier.fillMaxSize(),
      contentWindowInsets = WindowInsets(),
      floatingActionButton = {
      },
   ) { paddingValues ->
      LazyColumn(
         contentPadding = WindowInsets.safeDrawing.asPaddingValues(),
         modifier = Modifier.padding(paddingValues)
      ) {
         itemsWithDivider(state.rules, key = { it.id }) {
            Text(
               it.name,
               Modifier
                  .padding(32.dp)
                  .fillMaxWidth()
                  .animateItem()
            )
         }
      }
   }
}

@FullScreenPreviews
@Composable
private fun RuleListScreenPreview() {
   PreviewTheme {
      RuleListScreenContent(
         RuleListState(
            listOf(
               RuleMetadata(1, "Rule A"),
               RuleMetadata(2, "Rule B"),
            )
         )
      )
   }
}
