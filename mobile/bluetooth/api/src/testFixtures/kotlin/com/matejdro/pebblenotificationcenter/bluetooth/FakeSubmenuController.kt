package com.matejdro.pebblenotificationcenter.bluetooth

class FakeSubmenuController : SubmenuController {
   val sentMenus = HashMap<MenuItemKey, List<SubmenuItem<*>>>()

   override suspend fun showSubmenuOnTheWatch(
      notificationId: UByte,
      type: SubmenuType,
      items: List<SubmenuItem<*>>,
   ) {
      sentMenus[MenuItemKey(notificationId, type)] = items
   }

   override fun <T> getPayloadForMenuItem(notificationId: UByte, menu: SubmenuType, index: Int): T? {
      val menuItemsForThisMenu = sentMenus.remove(MenuItemKey(notificationId, menu))

      @Suppress("UNCHECKED_CAST")
      return menuItemsForThisMenu?.elementAtOrNull(index)?.payload as T?
   }

   data class MenuItemKey(val notificationId: UByte, val menu: SubmenuType)
}
