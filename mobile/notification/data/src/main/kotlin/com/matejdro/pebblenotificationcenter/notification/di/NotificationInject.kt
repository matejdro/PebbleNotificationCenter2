package com.matejdro.pebblenotificationcenter.notification.di

import com.matejdro.pebblenotificationcenter.notification.NotificationService

interface NotificationInject {
   fun inject(target: NotificationService)
}
