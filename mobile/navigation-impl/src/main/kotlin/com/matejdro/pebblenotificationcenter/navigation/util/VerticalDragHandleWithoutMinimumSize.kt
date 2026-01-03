package com.matejdro.pebblenotificationcenter.navigation.util

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DragHandleColors
import androidx.compose.material3.DragHandleShapes
import androidx.compose.material3.DragHandleSizes
import androidx.compose.material3.VerticalDragHandleDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.util.fastRoundToInt

// A copy of the VerticalDragHandle with the minimum size removed
@Composable
internal fun VerticalDragHandleWithoutMinimumSize(
   interactionSource: MutableInteractionSource,
   modifier: Modifier = Modifier,
   sizes: DragHandleSizes = VerticalDragHandleDefaults.sizes(),
   colors: DragHandleColors = VerticalDragHandleDefaults.colors(),
   shapes: DragHandleShapes = VerticalDragHandleDefaults.shapes(),
) {
   val isDragged by interactionSource.collectIsDraggedAsState()
   var isPressed by remember { mutableStateOf(false) }
   Box(
      modifier = modifier
         .hoverable(interactionSource)
         .pressable(interactionSource, { isPressed = true }, { isPressed = false })
         .graphicsLayer {
            shape =
               when {
                  isDragged -> shapes.draggedShape
                  isPressed -> shapes.pressedShape
                  else -> shapes.shape
               }
            clip = true
         }
         .layout { measurable, _ ->
            val dragHandleSize =
               when {
                  isDragged -> sizes.draggedSize
                  isPressed -> sizes.pressedSize
                  else -> sizes.size
               }.toSize()
            // set constraints here to be the size needed
            val placeable =
               measurable.measure(
                  Constraints.fixed(
                     dragHandleSize.width.fastRoundToInt(),
                     dragHandleSize.height.fastRoundToInt(),
                  )
               )
            layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
         }
         .drawBehind {
            drawRect(
               when {
                  isDragged -> colors.draggedColor
                  isPressed -> colors.pressedColor
                  else -> colors.color
               }
            )
         }
         .indication(interactionSource, ripple())
   )
}

private fun Modifier.pressable(
   interactionSource: MutableInteractionSource,
   onPressed: () -> Unit,
   onReleasedOrCancelled: () -> Unit,
): Modifier =
   pointerInput(interactionSource) {
      awaitEachGesture {
         awaitFirstDown(pass = PointerEventPass.Initial)
         onPressed()
         waitForUpOrCancellation(pass = PointerEventPass.Initial)
         onReleasedOrCancelled()
      }
   }
