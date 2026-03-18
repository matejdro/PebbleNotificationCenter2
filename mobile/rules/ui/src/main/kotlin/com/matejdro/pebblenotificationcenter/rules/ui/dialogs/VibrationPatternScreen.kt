package com.matejdro.pebblenotificationcenter.rules.ui.dialogs

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.core.os.bundleOf
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.matejdro.pebblenotificationcenter.common.NotificationsKeys.CHANNEL_ID_TESTS
import com.matejdro.pebblenotificationcenter.common.NotificationsKeys.NOTIFICATION_ID_PATTERN_TEST
import com.matejdro.pebblenotificationcenter.notification.NotificationConstants
import com.matejdro.pebblenotificationcenter.notification.utils.parseVibrationPattern
import com.matejdro.pebblenotificationcenter.rules.ui.R
import com.matejdro.pebblenotificationcenter.ui.components.AlertDialogInnerContent
import com.matejdro.pebblenotificationcenter.ui.debugging.FullScreenPreviews
import com.matejdro.pebblenotificationcenter.ui.debugging.PreviewTheme
import kotlinx.serialization.Serializable
import si.inova.kotlinova.compose.result.LocalResultPassingStore
import si.inova.kotlinova.compose.result.ResultKey
import si.inova.kotlinova.navigation.instructions.goBack
import si.inova.kotlinova.navigation.navigator.Navigator
import si.inova.kotlinova.navigation.screenkeys.DialogKey
import si.inova.kotlinova.navigation.screenkeys.ScreenKey
import si.inova.kotlinova.navigation.screens.InjectNavigationScreen
import si.inova.kotlinova.navigation.screens.Screen
import com.matejdro.pebblenotificationcenter.sharedresources.R as sharedR

@InjectNavigationScreen
class VibrationPatternScreen(private val navigator: Navigator) : Screen<VibrationPatternScreenKey>() {
   @Composable
   override fun Content(key: VibrationPatternScreenKey) {
      val resultPassingStore = LocalResultPassingStore.current
      val context = LocalContext.current
      val resources = LocalResources.current

      VibrationPatternScreenContent(
         key,
         accept = { pattern ->
            navigator.goBack()
            resultPassingStore.sendResult(key.result, pattern)
         },
         dismiss = { navigator.goBack() },
         test = { pattern ->
            val notification = NotificationCompat.Builder(context, CHANNEL_ID_TESTS)
               .setContentTitle(resources.getString(R.string.vibration_pattern_test))
               .setContentText(resources.getString(R.string.feel_the_new_pattern))
               .setSmallIcon(R.drawable.ic_watch_vibrate)
               .addExtras(
                  bundleOf(
                     NotificationConstants.KEY_FORCE_VIBRATE to true,
                     NotificationConstants.KEY_VIBRATION_PATTERN to pattern.toShortArray(),
                  )
               )

            val notificationManager = context.getSystemService<NotificationManager>()!!
            notificationManager.notify(NOTIFICATION_ID_PATTERN_TEST, notification.build())
         }
      )
   }
}

@Composable
private fun VibrationPatternScreenContent(
   key: VibrationPatternScreenKey,
   accept: (String) -> Unit,
   dismiss: () -> Unit,
   test: (List<Short>) -> Unit,
) {
   val textFieldState = rememberTextFieldState(key.existingPattern)
   var parsedPattern by remember { mutableStateOf<List<Short>?>(parseVibrationPattern(key.existingPattern)) }

   @Suppress("ComplexCondition") // Understandable in this case
   val limitToNumbersAndCommas = InputTransformation {
      for (i in 0 until length) {
         val char = this.charAt(i)
         if (
            !char.isDigit() &&
            char != '.' &&
            char != ',' &&
            char != ' '
         ) {
            revertAllChanges()
            return@InputTransformation
         }
      }

      parsedPattern = parseVibrationPattern(toString())
   }

   AlertDialogInnerContent(
      title = {
         Text(text = "Vibration pattern")
      },
      dismissButton = {
         TextButton(
            onClick = {
               dismiss()
            }
         ) {
            Text(stringResource(sharedR.string.cancel))
         }
      },
      confirmButton = {
         TextButton(
            onClick = {
               accept(textFieldState.text.toString())
            },
            enabled = parsedPattern != null
         ) {
            Text(stringResource(sharedR.string.ok))
         }
      },
      content = {
         val focusRequester = remember { FocusRequester() }
         var tapping by remember { mutableStateOf(false) }
         val vibrator = LocalContext.current.getSystemService<Vibrator>()!!
         val tapperInteractionSource = remember { MutableInteractionSource() }
         val lastTransition = remember { mutableLongStateOf(-1) }

         Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
               onClick = {
                  lastTransition.longValue = -1
                  textFieldState.clearText()
                  tapping = true
               },
               modifier = Modifier.fillMaxWidth(),
            ) {
               if (tapping) {
                  Text(stringResource(R.string.restart_tapping))
               } else {
                  Text(stringResource(R.string.start_tapping_pattern))
               }
            }

            Button(
               onClick = {},
               enabled = tapping,
               modifier = Modifier
                  .fillMaxWidth()
                  .height(100.dp),
               colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
               interactionSource = tapperInteractionSource,
            ) {
               if (tapping) {
                  Text(stringResource(R.string.tap))
               } else {
                  Text(stringResource(R.string.tap_above_button_to_start))
               }
            }

            TextField(
               textFieldState,
               Modifier
                  .fillMaxWidth()
                  .focusRequester(focusRequester),
               onKeyboardAction = { accept(textFieldState.text.toString()) },
               keyboardOptions = KeyboardOptions(
                  imeAction = ImeAction.Done,
                  keyboardType = KeyboardType.Number,
               ),
               lineLimits = TextFieldLineLimits.SingleLine,
               inputTransformation = limitToNumbersAndCommas
            )

            Button(
               onClick = { parsedPattern?.let { test(it) } },
               enabled = parsedPattern != null,
               modifier = Modifier.fillMaxWidth(),
            ) {
               Text(stringResource(R.string.test_notification))
            }
         }

         LaunchedEffect(Unit) {
            focusRequester.requestFocus()

            var pressed: Boolean = false

            tapperInteractionSource.interactions.collect { interaction ->
               val nowPressed = interaction is PressInteraction.Press
               if (nowPressed != pressed) {
                  if (nowPressed) {
                     if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(LONG_VIBRATION, VibrationEffect.DEFAULT_AMPLITUDE))
                     } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(LONG_VIBRATION)
                     }
                  } else {
                     vibrator.cancel()
                  }

                  if (lastTransition.longValue >= 0) {
                     val msDiff = SystemClock.uptimeMillis() - lastTransition.longValue
                     textFieldState.edit {
                        if (length != 0) {
                           append(", ")
                        }
                        append(msDiff.toString())
                     }
                     parsedPattern = parseVibrationPattern(textFieldState.text.toString())
                  }
                  lastTransition.longValue = SystemClock.uptimeMillis()
                  pressed = nowPressed
               }
            }
         }

         DisposableEffect(Unit) {
            onDispose {
               vibrator.cancel()
            }
         }
      },
   )
}

@ShowkaseComposable(group = "test")
@Composable
@FullScreenPreviews
@SuppressLint("VisibleForTests") // Previews are sort of tests
internal fun VibrationPatternScreenPreview() {
   PreviewTheme {
      Box {
         VibrationPatternScreenContent(
            VibrationPatternScreenKey("100, 200", ResultKey(0, 0)),
            {},
            {},
            {},
         )
      }
   }
}

@ShowkaseComposable(group = "test")
@Composable
@Preview
internal fun VibrationPatternScreenBlankPreview() {
   PreviewTheme {
      Box {
         VibrationPatternScreenContent(
            VibrationPatternScreenKey("", ResultKey(0, 0)),
            {},
            {},
            {},
         )
      }
   }
}

@Serializable
data class VibrationPatternScreenKey(
   val existingPattern: String,
   val result: ResultKey<String>,
) : ScreenKey(), DialogKey

private const val LONG_VIBRATION = 10_000L
