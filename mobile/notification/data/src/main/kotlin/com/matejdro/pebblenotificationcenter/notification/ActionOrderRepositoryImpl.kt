package com.matejdro.pebblenotificationcenter.notification

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.matejdro.pebblenotificationcenter.notification.model.Action
import com.matejdro.pebblenotificationcenter.rules.GlobalPreferenceKeys
import com.matejdro.pebblenotificationcenter.rules.keys.get
import com.matejdro.pebblenotificationcenter.rules.keys.set
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
class ActionOrderRepositoryImpl(
   context: Context,
   private val preferenceStore: DataStore<Preferences>,
) : ActionOrderRepository {
   private val otherActionsEntry = context.getString(R.string.other_actions)

   private val defaultOrder = listOf(
      context.getString(R.string.dismiss),
      context.getString(R.string.show_image),
      otherActionsEntry,
      context.getString(R.string.snooze),
      context.getString(R.string.pause_app),
      context.getString(R.string.unpause_app),
      context.getString(R.string.pause_conversation),
      context.getString(R.string.unpause_conversation),
   )

   private var checkedForDefaultItems: Boolean = false

   override fun getList(): Flow<List<String>> {
      return preferenceStore.data.map { it[GlobalPreferenceKeys.actionOrder] }
         .distinctUntilChanged()
         .onEach { list ->
            if (!checkedForDefaultItems) {
               val anyMissing = defaultOrder.any { !list.contains(it) }
               if (anyMissing) {
                  insertDefaultItems()
               }
               checkedForDefaultItems = true
            }
         }
         .map { it.toList() }
         .filter { it.isNotEmpty() }
   }

   private suspend fun insertDefaultItems() {
      preferenceStore.edit { prefs ->
         val existing = prefs[GlobalPreferenceKeys.actionOrder]
         val newList = existing.toMutableList().apply {
            for (item in defaultOrder) {
               if (!existing.contains(item)) {
                  add(item)
               }
            }
         }
         prefs[GlobalPreferenceKeys.actionOrder] = newList
      }
   }

   override suspend fun moveOrder(value: String, toIndex: Int) {
      preferenceStore.edit { prefs ->
         val existing = prefs[GlobalPreferenceKeys.actionOrder]
         val newList = existing.toMutableList().apply {
            remove(value)
            add(toIndex, value)
         }
         prefs[GlobalPreferenceKeys.actionOrder] = newList.toList()
      }
   }

   override suspend fun sort(list: List<Action>): List<Action> {
      val orderList = getList().first()
      val unknownIndexOrder = orderList.indexOf(otherActionsEntry) + 1

      val unknownElements = list.map { it.title } - orderList
      val updatedOrderList = if (unknownElements.isNotEmpty()) {
         preferenceStore.edit { prefs ->
            val existing = prefs[GlobalPreferenceKeys.actionOrder]
            val newList = existing.toMutableList().apply {
               for (element in unknownElements) {
                  add(unknownIndexOrder, element)
               }
            }
            prefs[GlobalPreferenceKeys.actionOrder] = newList
         }[GlobalPreferenceKeys.actionOrder]
      } else {
         orderList
      }

      return list.sortedWith { first, second ->
         updatedOrderList.indexOf(first.title).compareTo(updatedOrderList.indexOf(second.title))
      }
   }
}
