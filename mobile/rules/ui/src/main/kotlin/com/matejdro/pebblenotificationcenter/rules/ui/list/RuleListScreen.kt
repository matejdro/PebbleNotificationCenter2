package com.matejdro.pebblenotificationcenter.rules.ui.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.matejdro.pebblenotificationcenter.navigation.instructions.OpenScreenOrReplaceExistingType
import com.matejdro.pebblenotificationcenter.navigation.keys.RuleDetailsScreenKey
import com.matejdro.pebblenotificationcenter.navigation.keys.RuleListScreenKey
import com.matejdro.pebblenotificationcenter.navigation.util.rememberNavigationPopup
import com.matejdro.pebblenotificationcenter.navigation.util.trigger
import com.matejdro.pebblenotificationcenter.rules.RULE_ID_DEFAULT_SETTINGS
import com.matejdro.pebblenotificationcenter.rules.RuleMetadata
import com.matejdro.pebblenotificationcenter.rules.ui.R
import com.matejdro.pebblenotificationcenter.rules.ui.details.appPickingDialog
import com.matejdro.pebblenotificationcenter.rules.ui.dialogs.NameEntryScreenKey
import com.matejdro.pebblenotificationcenter.ui.animations.LocalSharedTransitionScope
import com.matejdro.pebblenotificationcenter.ui.components.ProgressErrorSuccessScaffold
import com.matejdro.pebblenotificationcenter.ui.debugging.FullScreenPreviews
import com.matejdro.pebblenotificationcenter.ui.debugging.PreviewTheme
import com.matejdro.pebblenotificationcenter.ui.lists.ReorderableListContainer
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import si.inova.kotlinova.compose.components.itemsWithDivider
import si.inova.kotlinova.compose.flow.collectAsStateWithLifecycleAndBlinkingPrevention
import si.inova.kotlinova.core.activity.requireActivity
import si.inova.kotlinova.navigation.navigator.Navigator
import si.inova.kotlinova.navigation.screenkeys.ScreenKey
import si.inova.kotlinova.navigation.screens.InjectNavigationScreen
import si.inova.kotlinova.navigation.screens.Screen
import com.matejdro.pebblenotificationcenter.sharedresources.R as sharedR

@InjectNavigationScreen
class RuleListScreen(
   private val viewModel: RuleListViewModel,
   private val navigator: Navigator,
   private val backstack: StateFlow<List<ScreenKey>>,
) : Screen<RuleListScreenKey>() {
   @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
   @Composable
   override fun Content(key: RuleListScreenKey) {
      val stateOutcome = viewModel.uiState.collectAsStateWithLifecycleAndBlinkingPrevention()

      val addDialog = navigator.rememberNavigationPopup(
         navigationKey = { _: Unit, resultKey -> NameEntryScreenKey(getString(R.string.new_rule), resultKey) },
         onResult = { name ->
            if (name !is NameEntryScreenKey.Result.Text) {
               return@rememberNavigationPopup
            }

            if (!name.text.isBlank()) {
               viewModel.addRule(name.text)
            }
         }
      )

      var isNextQuickAddMute by rememberSaveable { mutableStateOf(false) }
      val appPickerDialog = appPickingDialog(
         navigator,
         { pkg, channels ->
            if (isNextQuickAddMute) {
               viewModel.addRuleWithAppMute(pkg, channels)
            } else {
               viewModel.addRuleWithAppHide(pkg, channels)
            }
         },
      )

      val lastDetailsFlow = remember {
         backstack
            .map { list -> list.filterIsInstance<RuleDetailsScreenKey>().lastOrNull() }
      }

      val selectedDetailsKey = lastDetailsFlow.collectAsStateWithLifecycle(null).value
      val windowSizeClass = calculateWindowSizeClass(LocalContext.current.requireActivity())
      // Do not highlight selected item in the phone mode
      val selectedRuleId = selectedDetailsKey?.id.takeIf {
         windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
      }

      ProgressErrorSuccessScaffold(
         stateOutcome::value,
         errorProgressModifier = Modifier.safeDrawingPadding()
      ) { state ->
         RuleListScreenContent(
            state,
            selectedRuleId,
            addEmptyRule = { addDialog.trigger() },
            addHideRule = {
               isNextQuickAddMute = false
               appPickerDialog.trigger(false)
            },
            addMuteRule = {
               isNextQuickAddMute = true
               appPickerDialog.trigger(false)
            },
            setOrder = viewModel::reorder,
            openDetails = { id ->
               navigator.navigate(
                  OpenScreenOrReplaceExistingType(RuleDetailsScreenKey(id))
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
   selectedRuleId: Int?,
   addMuteRule: () -> Unit,
   addHideRule: () -> Unit,
   addEmptyRule: () -> Unit,
   setOrder: (Int, Int) -> Unit,
   openDetails: (Int) -> Unit,
   addButtonsShown: Boolean = false,
) = with(LocalSharedTransitionScope.current) {
   Scaffold(
      Modifier.fillMaxSize(),
      contentWindowInsets = WindowInsets(),
      floatingActionButton = {
         AddButtons(
            addMuteRule,
            addHideRule,
            addEmptyRule,
            addButtonsShown
         )
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
                        .run {
                           if (rule.id == selectedRuleId) {
                              background(MaterialTheme.colorScheme.primary)
                           } else {
                              this
                           }
                        }
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
                        },
                     color = if (rule.id == selectedRuleId) {
                        MaterialTheme.colorScheme.onPrimary
                     } else {
                        MaterialTheme.colorScheme.onSurface
                     },
                  )
               }
            }
         }
      }
   }
}

@Composable
private fun AddButtons(
   addMuteRule: () -> Unit,
   addHideRule: () -> Unit,
   addEmptyRule: () -> Unit,
   shown: Boolean = false,
) {
   var showFabs by rememberSaveable { mutableStateOf(shown) }
   val fabRotation by animateFloatAsState(if (showFabs) ROTATION_QUARTER_CIRCLE_DEG else 0f)

   Column(
      modifier = Modifier
         .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
      horizontalAlignment = Alignment.End,
   ) {
      AnimatedVisibility(
         visible = showFabs,
         enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
         exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut()
      ) {
         Column(
            Modifier.padding(end = 10.dp),
            horizontalAlignment = Alignment.End,
         ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
               Text(stringResource(R.string.mute_an_app))

               FloatingActionButton(
                  containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                  onClick = {
                     showFabs = false
                     addMuteRule()
                  },
                  modifier = Modifier
                     .padding(start = 8.dp)
                     .size(48.dp)
               ) {
                  Icon(painterResource(R.drawable.ic_mute), null)
               }
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
               Text(stringResource(R.string.hide_an_app))

               FloatingActionButton(
                  containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                  onClick = {
                     showFabs = false
                     addHideRule()
                  },
                  modifier = Modifier
                     .padding(start = 8.dp)
                     .size(48.dp)
               ) {
                  Icon(painterResource(R.drawable.ic_hide), null)
               }
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
               Text(stringResource(R.string.create_a_blank_rule))

               FloatingActionButton(
                  containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                  onClick = {
                     showFabs = false
                     addEmptyRule()
                  },
                  modifier = Modifier
                     .padding(start = 8.dp)
                     .size(48.dp)
               ) {
                  Icon(painterResource(sharedR.drawable.rule), null)
               }
            }
         }
      }

      FloatingActionButton(
         onClick = {
            showFabs = !showFabs
         },
         modifier = Modifier
            .graphicsLayer {
               rotationZ = fabRotation
            }
            .padding(16.dp)
      ) {
         Icon(painterResource(R.drawable.ic_add), stringResource(R.string.new_rule))
      }
   }
}

private const val ROTATION_QUARTER_CIRCLE_DEG = 45f

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
         null,
         {},
         {},
         {},
         { _, _ -> },
         {},
      )
   }
}

@Preview
@Composable
private fun RuleListScreenWithFabExpandedPreview() {
   PreviewTheme {
      RuleListScreenContent(
         RuleListState(
            listOf(
               RuleMetadata(1, "Rule A"),
               RuleMetadata(2, "Rule B"),
            )
         ),
         null,
         {},
         {},
         {},
         { _, _ -> },
         {},
         addButtonsShown = true,
      )
   }
}

@Preview
@Composable
private fun RuleListScreenWithSelectionPreview() {
   PreviewTheme {
      RuleListScreenContent(
         RuleListState(
            listOf(
               RuleMetadata(1, "Rule A"),
               RuleMetadata(2, "Rule B"),
            )
         ),
         2,
         {},
         {},
         {},
         { _, _ -> },
         {},
      )
   }
}
