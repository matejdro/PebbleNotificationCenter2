package com.matejdro.pebblenotificationcenter.common.flow

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import si.inova.kotlinova.core.outcome.Outcome

fun <T> Outcome<T>.unwrap(): T = when (this) {
   is Outcome.Error -> throw exception
   is Outcome.Progress -> error("Outcome should not be progress")
   is Outcome.Success -> data
}

suspend fun <T> Flow<Outcome<T>>.firstData() = first { it !is Outcome.Progress }.unwrap()
