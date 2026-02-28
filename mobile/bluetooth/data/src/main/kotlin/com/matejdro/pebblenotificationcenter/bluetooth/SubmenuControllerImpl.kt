package com.matejdro.pebblenotificationcenter.bluetooth

import com.matejdro.pebble.bluetooth.common.PacketQueue
import com.matejdro.pebble.bluetooth.common.di.WatchappConnectionScope
import com.matejdro.pebble.bluetooth.common.util.LimitingStringEncoder
import com.matejdro.pebble.bluetooth.common.util.writeUByte
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.rebble.pebblekit2.common.model.PebbleDictionaryItem
import okio.Buffer
import java.util.concurrent.ConcurrentHashMap

@Inject
@ContributesBinding(WatchappConnectionScope::class)
@SingleIn(WatchappConnectionScope::class)
class SubmenuControllerImpl(
   private val packetQueue: PacketQueue,
) : SubmenuController {
   private val stringEncoder = LimitingStringEncoder()

   private val menuItems = ConcurrentHashMap<MenuItemKey, List<SubmenuItem<*>>>()

   override suspend fun showSubmenuOnTheWatch(
      notificationId: UByte,
      type: SubmenuType,
      items: List<SubmenuItem<*>>,
   ) {
      val buffer = Buffer()

      val numActionsToSend = items.size.coerceAtMost(MAX_ACTIONS)
      val limitedItems = items.take(numActionsToSend)
      val menuId = (type.ordinal + 1).toUByte()

      buffer.writeUByte(notificationId)
      buffer.writeUByte(menuId)
      buffer.writeUByte(numActionsToSend.toUByte())

      for (item in limitedItems) {
         val limitedString = stringEncoder.encodeSizeLimited(item.text, MAX_ACTION_TEXT_BYTES).encodedString
         buffer.write(limitedString)
         buffer.writeByte(0)
         buffer.writeByte(if (item.voiceInput) 1 else 0)
      }

      menuItems[MenuItemKey(notificationId, type)] = limitedItems

      packetQueue.sendPacket(
         mapOf(
            0u to PebbleDictionaryItem.UInt8(9u),
            1u to PebbleDictionaryItem.Bytes(buffer.readByteArray())
         ),
         priority = PRIORITY_USER_INTERACTION
      )
   }

   override fun <T> getPayloadForMenuItem(notificationId: UByte, menu: SubmenuType, index: Int): T? {
      val menuItemsForThisMenu = menuItems.remove(MenuItemKey(notificationId, menu))

      @Suppress("UNCHECKED_CAST")
      return menuItemsForThisMenu?.elementAtOrNull(index)?.payload as T?
   }

   private data class MenuItemKey(val notificationId: UByte, val menu: SubmenuType)
}

private const val MAX_ACTION_TEXT_BYTES = 20
private const val MAX_ACTIONS = 20
