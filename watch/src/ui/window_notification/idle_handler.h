#pragma once

#include "pebble.h"

extern bool idle_handler_has_user_interacted_since_app_start;
extern bool idle_handler_has_user_interacted_since_last_vibration;

void idle_handler_register_timers();
void idle_handler_notify_user_interacted();
void idle_handler_notify_received_new_vibration();
void idle_handler_notify_notifications_updated();
