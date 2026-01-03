package com.matejdro.pebblenotificationcenter.navigation.util

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.TransitionState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDragHandle
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.window.layout.FoldingFeature
import com.google.accompanist.adaptive.SplitResult
import com.google.accompanist.adaptive.TwoPane
import com.google.accompanist.adaptive.TwoPaneStrategy
import com.google.accompanist.adaptive.calculateDisplayFeatures
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import si.inova.kotlinova.core.activity.requireActivity
import si.inova.kotlinova.navigation.screenkeys.ScreenKey
import si.inova.kotlinova.navigation.screens.Screen

abstract class MasterDetailScreen<K : ScreenKey, D> : Screen<K>() {
   protected open fun getDefaultOpenDetails(key: K): D? {
      return null
   }

   @Composable
   @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
   override fun Content(key: K) {
      val windowSize = calculateWindowSizeClass(LocalContext.current.requireActivity())
      MasterDetail(key, windowSize.widthSizeClass)
   }

   @Composable
   private fun MasterDetail(key: K, widthSize: WindowWidthSizeClass) {
      val defaultOpenDetails = getDefaultOpenDetails(key)

      val currentDetailScreen = rememberSaveable { mutableStateOf<D?>(defaultOpenDetails) }
      val openState = rememberSaveable(
         saver = Saver(
            save = { it.currentState },
            restore = { SeekableTransitionState(it) }
         )
      ) { SeekableTransitionState(currentDetailScreen.value != null) }
      val lastKey = rememberSaveable { mutableStateOf(key) }
      val transition = rememberTransition(openState)

      val scope = rememberCoroutineScope()

      LaunchedEffect(defaultOpenDetails) {
         if (lastKey.value != key && defaultOpenDetails != currentDetailScreen.value) {
            currentDetailScreen.value = defaultOpenDetails
            openState.snapTo(defaultOpenDetails != null)
         }

         lastKey.value = key
      }

      fun openDetail(key: D) {
         currentDetailScreen.value = key
         scope.launch {
            openState.animateTo(true)
         }
      }

      val saveableStateHolder = rememberSaveableStateHolder()

      val master = remember {
         movableContentOf {
            saveableStateHolder.SaveableStateProvider(false) {
               Master(key, ::openDetail)
            }
         }
      }

      val detail = remember {
         movableContentOf<D> { detail ->
            if (detail != null) {
               saveableStateHolder.SaveableStateProvider(detail) {
                  Detail(detail)
               }
            }
         }
      }

      if (widthSize == WindowWidthSizeClass.Companion.Compact) {
         MasterDetailOnPhone(
            openState = openState,
            transition = transition,
            updateOpenState = openState::updateOpenState,
            currentDetailScreen = currentDetailScreen::value,
            master = master,
            detail = detail,
         )
      } else {
         MasterDetailOnLargerScreen(currentDetailScreen::value, master, detail)
      }
   }

   @Composable
   @OptIn(ExperimentalAnimationApi::class)
   private fun MasterDetailOnPhone(
      openState: TransitionState<Boolean>,
      transition: Transition<Boolean>,
      updateOpenState: suspend (Boolean, Float?) -> Unit,
      currentDetailScreen: () -> D?,
      master: @Composable () -> Unit,
      detail: @Composable (D) -> Unit,
   ) {
      transition.AnimatedContent(
         transitionSpec = {
            if (this.targetState) {
               slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) togetherWith
                  slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left)
            } else {
               slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right) togetherWith
                  slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right)
            }
         },
      ) { open ->
         if (open) {
            currentDetailScreen()?.let {
               Box(Modifier.fillMaxSize()) {
                  detail(it)
               }
            }
         } else {
            Box(Modifier.fillMaxSize()) {
               master()
            }
         }
      }

      PredictiveBackHandler(enabled = openState.currentState == true) { events ->
         var completed = false
         try {
            events.collectLatest {
               updateOpenState(false, it.progress)
            }
            completed = true
            updateOpenState(false, null)
         } catch (e: CancellationException) {
            if (!completed) {
               withContext(NonCancellable) {
                  updateOpenState(true, null)
               }
            }

            throw e
         }
      }
   }

   @Composable
   private fun MasterDetailOnLargerScreen(
      currentDetailScreen: () -> D?,
      master: @Composable () -> Unit,
      detail: @Composable (D) -> Unit,
   ) {
      var offsetX by remember { mutableFloatStateOf(0f) }
      var screenWidth by remember { mutableIntStateOf(0) }
      val density = LocalDensity.current

      val displayFeatures = calculateDisplayFeatures(LocalContext.current.requireActivity())

      val verticalFold = displayFeatures.find {
         it is FoldingFeature
      } as FoldingFeature?

      val canSeparatorMove = verticalFold != null &&
         !verticalFold.isSeparating &&
         verticalFold.occlusionType != FoldingFeature.OcclusionType.FULL

      // When fold is in half opened mode, master should be at the bottom, so user can select things on the bottom
      // and watch detail on the top
      val flipMasterDetail = verticalFold != null && verticalFold.state == FoldingFeature.State.HALF_OPENED

      val detailPane: @Composable () -> Unit = {
         Row(Modifier.fillMaxHeight()) {
            if (canSeparatorMove) {
               VerticalDragHandle(
                  Modifier
                     .fillMaxHeight()
                     .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                     .wrapContentHeight()
                     .draggable(
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { delta ->
                           with(density) {
                              offsetX =
                                 (offsetX + delta).coerceIn(
                                    MIN_PANE_WIDTH.toPx(),
                                    screenWidth - MIN_PANE_WIDTH.toPx(),
                                 )
                           }
                        },
                     )
                     .systemGestureExclusion() // To avoid colliding with the back gesture
               )
            }

            Crossfade(
               currentDetailScreen(),
               Modifier
                  .fillMaxHeight(),
               label = "Master Detail"
            ) { value ->
               if (value != null) {
                  Box(Modifier.fillMaxSize()) {
                     detail(value)
                  }
               }
            }
         }
      }

      val twoPaneStrategy = remember {
         TwoPaneStrategy {
               _,
               _,
               layoutCoordinates: LayoutCoordinates,
            ->

            SplitResult(
               gapOrientation = Orientation.Vertical,
               gapBounds = Rect(
                  left = offsetX,
                  top = 0f,
                  right = offsetX,
                  bottom = layoutCoordinates.size.height.toFloat(),
               )
            )
         }
      }

      TwoPane(
         if (flipMasterDetail) detailPane else master,
         if (flipMasterDetail) master else detailPane,
         twoPaneStrategy,
         displayFeatures = displayFeatures,
         modifier = Modifier
            .fillMaxSize()
            .layout
            { measurable, constraints ->
               val placeable = measurable.measure(constraints)
               if (screenWidth == 0) {
                  offsetX = placeable.width * DEFAULT_PANE_SPLIT
               }

               screenWidth = placeable.width

               layout(placeable.width, placeable.height) {
                  placeable.place(0, 0)
               }
            }
      )
   }

   @Composable
   protected abstract fun Master(key: K, openDetail: (D) -> Unit)

   @Composable
   protected abstract fun Detail(key: D)
}

private suspend fun SeekableTransitionState<Boolean>.updateOpenState(targetState: Boolean, progress: Float?) {
   if (progress != null) {
      seekTo(progress, targetState)
   } else {
      if (targetState) {
         snapTo(targetState)
      } else {
         animateTo(targetState)
      }
   }
}

private const val DEFAULT_PANE_SPLIT = 0.3f
private val MIN_PANE_WIDTH = 200.dp
