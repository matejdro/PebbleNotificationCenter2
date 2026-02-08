package com.matejdro.pebblenotificationcenter.ui.lists

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import com.mohamedrejeb.compose.dnd.reorder.ReorderContainer
import com.mohamedrejeb.compose.dnd.reorder.ReorderableItem
import com.mohamedrejeb.compose.dnd.reorder.rememberReorderState
import kotlinx.coroutines.launch

private const val LEGACY_VIBRAITON_DURATION_REORDER_MS = 25L
private const val LEGACY_VIBRAITON_DURATION_START_MS = 100L

@Composable
fun <T> ReorderableListContainer(
   data: List<T>,
   lazyListState: LazyListState,
   modifier: Modifier = Modifier,
   enabled: Boolean = true,
   content: @Composable ReorderableListScope<T>.(List<T>) -> Unit,
) {
   val reorderState = rememberReorderState<T>(dragAfterLongPress = true)

   var reorderingList by remember(data) { mutableStateOf(data) }
   var dragging by remember { mutableStateOf(false) }
   val density = LocalDensity.current
   val vibrator = LocalContext.current.getSystemService<Vibrator>()
   val coroutineScope = rememberCoroutineScope()

   var lastDragIndex by remember { mutableIntStateOf(-1) }

   val scope = object : ReorderableListScope<T> {
      @Composable
      override fun <K : Any> ReorderableListItem(
         key: K,
         data: T,
         setOrder: (key: K, toIndex: Int) -> Unit,
         modifier: Modifier,
         minReorderableIndex: Int,
         enabled: Boolean,
         content: @Composable ((Modifier, isDragging: () -> Boolean) -> Unit),
      ) {
         ReorderableItem(
            state = reorderState,
            key = key,
            data = data,
            enabled = enabled,
            onDragEnter = { state ->
               reorderingList = reorderingList.toMutableList().apply {
                  val index = indexOf(data)
                  if (index < minReorderableIndex) return@ReorderableItem
                  remove(state.data)
                  add(index, state.data)

                  if (lastDragIndex != index) {
                     // Sometimes onDragEnter is called twice. Wrap in this check to ensure we don't vibrate twice
                     if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        vibrator?.vibrate(
                           VibrationEffect.createPredefined(
                              if (dragging) {
                                 VibrationEffect.EFFECT_TICK
                              } else {
                                 VibrationEffect.EFFECT_CLICK
                              }
                           )
                        )
                     } else {
                        @Suppress("DEPRECATION")
                        vibrator?.vibrate(
                           if (dragging) {
                              LEGACY_VIBRAITON_DURATION_REORDER_MS
                           } else {
                              LEGACY_VIBRAITON_DURATION_START_MS
                           }
                        )
                     }
                  }
                  dragging = true
                  lastDragIndex = index

                  coroutineScope.launch {
                     handleLazyListScroll(
                        lazyListState = lazyListState,
                        dropIndex = index + 3,
                        density = density,
                     )
                  }
               }
            },
            onDrop = {
               if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                  vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
               } else {
                  @Suppress("DEPRECATION")
                  vibrator?.vibrate(LEGACY_VIBRAITON_DURATION_START_MS)
               }

               dragging = false
               lastDragIndex = -1
               @Suppress("UNCHECKED_CAST")
               setOrder(it.key as K, reorderingList.indexOf(it.data))
            },
            draggableContent = {
               content(
                  Modifier
                     .dropShadow(
                        RectangleShape,
                        Shadow(
                           radius = 5.dp,
                           spread = 3.dp,
                           color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                           offset = DpOffset(x = 2.dp, 2.dp),
                        )
                     )
                     .background(MaterialTheme.colorScheme.surface),
                  { true }
               )
            },
            modifier = modifier,
         ) {
            content(
               Modifier.graphicsLayer {
                  alpha = if (isDragging) 0f else 1f
               },
               { false },
            )
         }
      }
   }

   ReorderContainer(
      reorderState,
      enabled = enabled,
      modifier = modifier,
   ) {
      scope.content(reorderingList)
   }
}

@Stable
interface ReorderableListScope<T> {
   @Composable
   fun <K : Any> ReorderableListItem(
      key: K,
      data: T,
      setOrder: (key: K, toIndex: Int) -> Unit,
      modifier: Modifier = Modifier,
      minReorderableIndex: Int = 0,
      enabled: Boolean = true,
      content: @Composable ((Modifier, isDragging: () -> Boolean) -> Unit),
   )
}
