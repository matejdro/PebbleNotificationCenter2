package com.matejdro.pebblenotificationcenter.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrain
import androidx.compose.ui.unit.dp
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.matejdro.pebblenotificationcenter.ui.debugging.PreviewTheme
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertDialogWithContent(
   onDismissRequest: () -> Unit,
   confirmButton: @Composable () -> Unit,
   modifier: Modifier = Modifier,
   dismissButton: @Composable (() -> Unit) = { },
   neutralButton: @Composable (() -> Unit) = { },
   title: @Composable (() -> Unit)? = null,
   content: @Composable () -> Unit,
) {
   BasicAlertDialog(
      onDismissRequest = onDismissRequest,
      modifier = modifier,
      content = {
         AlertDialogInnerContent(
            title = title,
            neutralButton = neutralButton,
            dismissButton = dismissButton,
            confirmButton = confirmButton,
            content = content
         )
      }
   )
}

@Composable
fun AlertDialogInnerContent(
   modifier: Modifier = Modifier,
   title: @Composable (() -> Unit)? = null,
   neutralButton: @Composable (() -> Unit) = {},
   dismissButton: @Composable (() -> Unit) = {},
   confirmButton: @Composable (() -> Unit) = {},
   content: @Composable (() -> Unit),
) {
   Surface(
      modifier = modifier,
      shape = MaterialTheme.shapes.large,
      color = AlertDialogDefaults.containerColor,
      tonalElevation = AlertDialogDefaults.TonalElevation,
   ) {
      Layout(
         modifier = Modifier
            .padding(24.dp),
         measurePolicy = DialogMeasuePolicy,
         content = {
            Box(Modifier.padding(bottom = 16.dp)) {
               title?.let {
                  CompositionLocalProvider(
                     LocalTextStyle provides MaterialTheme.typography.headlineSmall
                  ) {
                     it()
                  }
               }
            }

            Box {
               content()
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
               neutralButton()
               Spacer(Modifier.weight(1f))
               dismissButton()
               confirmButton()
            }
         }
      )
   }
}

private object DialogMeasuePolicy : MeasurePolicy {
   override fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult {
      val title = measurables.elementAt(0)
      val content = measurables.elementAt(1)
      val buttons = measurables.elementAt(2)

      val titlePlaceable = title.measure(constraints)
      val buttonsPlaceable = buttons.measure(constraints)

      val contentConstraints = constraints.constrain(
         Constraints(
            maxHeight = constraints.maxHeight - titlePlaceable.height - buttonsPlaceable.height
         )
      )

      val contentPlaceable = content.measure(contentConstraints)

      return layout(
         width = max(titlePlaceable.width, max(contentPlaceable.width, buttonsPlaceable.width)),
         height = titlePlaceable.height + contentPlaceable.height + buttonsPlaceable.height
      ) {
         titlePlaceable.place(0, 0)
         contentPlaceable.place(0, titlePlaceable.height)
         buttonsPlaceable.place(0, titlePlaceable.height + contentPlaceable.height)
      }
   }
}

@Preview
@Composable
@ShowkaseComposable(group = "Components")
internal fun AlertDialogWithContentPreview() {
   PreviewTheme(fill = false) {
      AlertDialogWithContent(
         onDismissRequest = {},
         confirmButton = { TextButton(onClick = {}) { Text("OK") } },
         dismissButton = { TextButton(onClick = {}) { Text("Cancel") } },
         title = { Text("Title") },
      ) {
         Button(onClick = {}) {
            Text("A button inside dialog")
         }
      }
   }
}

@Preview
@Composable
@ShowkaseComposable(group = "Components")
internal fun AlertDialogWithContentWithoutTitleAndCancelButtonPreview() {
   PreviewTheme(fill = false) {
      AlertDialogWithContent(
         onDismissRequest = {},
         confirmButton = { TextButton(onClick = {}) { Text("OK") } },
      ) {
         Button(onClick = {}) {
            Text("A button inside dialog")
         }
      }
   }
}
