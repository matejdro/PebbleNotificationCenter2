package com.matejdro.pebblenotificationcenter.notification

import com.matejdro.pebblenotificationcenter.notification.model.Action
import kotlinx.coroutines.flow.Flow

interface ActionOrderRepository {

   /**
    * Get a list of all known action names, sorted
    */
   fun getList(): Flow<List<String>>

   /**
    * Move the item in the list of all known actions onto another index
    */
   suspend fun moveOrder(value: String, toIndex: Int)

   /**
    * Sort the provided list with the order of the actions in this repository
    */
   suspend fun sort(list: List<Action>): List<Action>
}
