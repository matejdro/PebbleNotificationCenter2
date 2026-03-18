package com.matejdro.pebblenotificationcenter.rules

import com.matejdro.pebblenotificationcenter.rules.sqldelight.generated.DbRule

fun DbRule.toRuleMetadata() = RuleMetadata(id.toInt(), name)
