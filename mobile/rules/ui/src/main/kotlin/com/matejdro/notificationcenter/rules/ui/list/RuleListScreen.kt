package com.matejdro.notificationcenter.rules.ui.list

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.matejdro.notificationcenter.rules.RuleMetadata
import com.matejdro.notificationcenter.rules.ui.R
import com.matejdro.pebblenotificationcenter.navigation.instructions.OpenScreenOrReplaceExistingType
import com.matejdro.pebblenotificationcenter.navigation.keys.RuleDetailsScreenKey
import com.matejdro.pebblenotificationcenter.navigation.keys.RuleListScreenKey
import com.matejdro.pebblenotificationcenter.ui.animations.LocalSharedTransitionScope
import com.matejdro.pebblenotificationcenter.ui.components.AlertDialogWithContent
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

      var showAddDialog by remember { mutableStateOf(false) }
      if (showAddDialog) {
         NameEntryDialog(
            stringResource(R.string.add_a_new_rule),
            dismiss = { showAddDialog = false },
            accept = {
               if (!it.isBlank()) {
                  viewModel.addRule(it)
               }
               showAddDialog = false
            }
         )
      }

      ProgressErrorSuccessScaffold(stateOutcome, modifier = Modifier.safeDrawingPadding()) { state ->
         RuleListScreenContent(
            state,
            addNew = { showAddDialog = true },
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
                  minReorderableIndex = 1,
                  enabled = rule.id > 1,
                  setOrder = setOrder
               ) {
                  Text(
                     rule.name,
                     Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable(onClick = { openDetails(rule.id) })
                        .padding(32.dp)
                        .fillMaxWidth()
                        .animateItem()
                        .sharedElement(
                           rememberSharedContentState("ruleName-${rule.id}"),
                           LocalNavAnimatedContentScope.current
                        )

                  )
               }
            }
         }
      }
   }
}

@Composable
private fun NameEntryDialog(
   title: String,
   dismiss: () -> Unit,
   accept: (text: String) -> Unit,
) {
   val textFieldState = rememberTextFieldState("")

   AlertDialogWithContent(
      title = {
         Text(text = title)
      },
      onDismissRequest = {
         dismiss()
      },
      confirmButton = {
         TextButton(
            onClick = {
               accept(textFieldState.text.toString())
            }
         ) {
            Text(stringResource(R.string.ok))
         }
      },
      dismissButton = {
         TextButton(
            onClick = {
               dismiss()
            }
         ) {
            Text(stringResource(R.string.cancel))
         }
      },
   ) {
      val focusRequester = remember { FocusRequester() }

      TextField(
         textFieldState,
         Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
         onKeyboardAction = { accept(textFieldState.text.toString()) },
         keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
         lineLimits = TextFieldLineLimits.SingleLine,
      )

      LaunchedEffect(Unit) {
         focusRequester.requestFocus()
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
