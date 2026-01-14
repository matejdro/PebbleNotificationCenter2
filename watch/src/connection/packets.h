#pragma once

#include "pebble.h"

void send_watch_welcome();
void send_trigger_action(uint16_t id);
bool send_notification_opened(uint8_t id);
void packets_init();