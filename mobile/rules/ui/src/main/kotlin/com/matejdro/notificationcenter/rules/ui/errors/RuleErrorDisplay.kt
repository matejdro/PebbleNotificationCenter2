package com.matejdro.notificationcenter.rules.ui.errors

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.matejdro.notificationcenter.rules.ui.R
import com.matejdro.pebblenotificationcenter.ui.errors.commonUserFriendlyMessage
import si.inova.kotlinova.core.outcome.CauseException

@Composable
fun CauseException.ruleUserFriendlyMessage(
   hasExistingData: Boolean = false,
): String {
   return if (this is RuleMissingException) {
      stringResource(R.string.this_rule_does_not_exist)
   } else {
      commonUserFriendlyMessage(hasExistingData)
   }
}
