package com.matejdro.pebblenotificationcenter.bluetooth.images

interface ImageSender {
   suspend fun showImageOnTheWatch(icon: Any)
}
