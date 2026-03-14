package com.matejdro.pebblenotificationcenter.notification

import com.matejdro.pebblenotificationcenter.notification.model.Action
import kotlinx.coroutines.flow.Flow

class FakeActionOrderRepository : ActionOrderRepository {
   private val orderOverrides = HashMap<String, Int>()

   /**
    * Sort the provided list with the order of the actions in this repository
    */
   override suspend fun sort(list: List<Action>): List<Action> {
      return list.sortedWith { first, second ->
         (orderOverrides[first.title] ?: 0).compareTo((orderOverrides[second.title] ?: 0))
      }
   }

   /**
    * Move the item in the list of all known actions onto another index
    */
   override suspend fun moveOrder(value: String, toIndex: Int) {
      orderOverrides[value] = toIndex
   }

   /**
    * Get a list of all known action names, sorted
    */
   override fun getList(): Flow<List<String>> {
      throw UnsupportedOperationException("Not supported in tests ")
   }
}
