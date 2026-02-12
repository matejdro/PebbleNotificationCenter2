package com.matejdro.notificationcenter.rules.ui.list

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.matejdro.notificationcenter.rules.RULE_ID_DEFAULT_SETTINGS
import com.matejdro.notificationcenter.rules.RuleMetadata
import com.matejdro.notificationcenter.rules.ui.R
import com.matejdro.notificationcenter.rules.ui.dialogs.NameEntryScreenKey
import com.matejdro.pebblenotificationcenter.navigation.instructions.OpenScreenOrReplaceExistingType
import com.matejdro.pebblenotificationcenter.navigation.keys.RuleDetailsScreenKey
import com.matejdro.pebblenotificationcenter.navigation.keys.RuleListScreenKey
import com.matejdro.pebblenotificationcenter.navigation.util.rememberNavigationPopup
import com.matejdro.pebblenotificationcenter.navigation.util.trigger
import com.matejdro.pebblenotificationcenter.ui.animations.LocalSharedTransitionScope
import com.matejdro.pebblenotificationcenter.ui.components.ProgressErrorSuccessScaffold
import com.matejdro.pebblenotificationcenter.ui.debugging.FullScreenPreviews
import com.matejdro.pebblenotificationcenter.ui.debugging.PreviewTheme
import com.matejdro.pebblenotificationcenter.ui.lists.ReorderableListContainer
import si.inova.kotlinova.compose.components.itemsWithDivider
import si.inova.kotlinova.compose.flow.collectAsStateWithLifecycleAndBlinkingPrevention
import si.inova.kotlinova.navigation.navigator.Navigator
import si.inova.kotlinova.navigation.screens.InjectNavigationScreen
import si.inova.kotlinova.navigation.screens.Screen

@InjectNavigationScreen
class RuleListScreen(
   private val viewModel: RuleListViewModel,
   private val navigator: Navigator,
) : Screen<RuleListScreenKey>() {
   @Composable
   override fun Content(key: RuleListScreenKey) {
      val stateOutcome = viewModel.uiState.collectAsStateWithLifecycleAndBlinkingPrevention().value
      val addDialog = navigator.rememberNavigationPopup(
         navigationKey = { _: Unit, resultKey -> NameEntryScreenKey(getString(R.string.new_rule), resultKey) },
         onResult = {
            if (!it.isBlank()) {
               viewModel.addRule(it)
            }
         }
      )

      ProgressErrorSuccessScaffold(stateOutcome, modifier = Modifier.safeDrawingPadding()) { state ->
         RuleListScreenContent(
            state,
            addNew = { addDialog.trigger() },
            setOrder = viewModel::reorder,
            openDetails = {
               navigator.navigate(
                  OpenScreenOrReplaceExistingType(RuleDetailsScreenKey(it))
               )
            }
         )
      }
   }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun RuleListScreenContent(
   state: RuleListState,
   addNew: () -> Unit,
   setOrder: (id: Int, toIndex: Int) -> Unit,
   openDetails: (id: Int) -> Unit,
) = with(LocalSharedTransitionScope.current) {
   Scaffold(
      Modifier.fillMaxSize(),
      contentWindowInsets = WindowInsets(),
      floatingActionButton = {
         FloatingActionButton(
            onClick = addNew,
            modifier = Modifier
               .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
         ) {
            Icon(painterResource(R.drawable.ic_add), stringResource(R.string.new_rule))
         }
      },
   ) { paddingValues ->
      val listState = rememberLazyListState()
      ReorderableListContainer(state.rules, listState) { rules ->
         LazyColumn(
            contentPadding = WindowInsets.safeDrawing.asPaddingValues(),
            modifier = Modifier.padding(paddingValues)
         ) {
            itemsWithDivider(rules, key = { it.id }) { rule ->
               ReorderableListItem(
                  rule.id,
                  rule,
                  minReorderableIndex = RULE_ID_DEFAULT_SETTINGS,
                  enabled = rule.id > RULE_ID_DEFAULT_SETTINGS,
                  setOrder = setOrder
               ) { modifier, isDragging ->
                  Text(
                     rule.name,
                     modifier
                        .clickable(onClick = { openDetails(rule.id) })
                        .padding(32.dp)
                        .fillMaxWidth()
                        .animateItem()
                        .run {
                           if (!isDragging()) {
                              sharedElement(
                                 rememberSharedContentState("ruleName-${rule.id}"),
                                 LocalNavAnimatedContentScope.current
                              )
                           } else {
                              this
                           }
                        }

                  )
               }
            }
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
         ),
         {},
         { _, _ -> },
         {},
      )
   }
}
