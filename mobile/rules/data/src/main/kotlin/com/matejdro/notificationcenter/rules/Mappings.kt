package com.matejdro.notificationcenter.rules

import com.matejdro.notificationcenter.rules.sqldelight.generated.DbRule

fun DbRule.toRuleMetadata() = RuleMetadata(id.toInt(), name)
