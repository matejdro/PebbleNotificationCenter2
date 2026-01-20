#pragma once

#include "pebble.h"

void send_watch_welcome();
bool send_notification_opened(uint8_t id);
bool send_action_trigger(uint8_t notification_id, uint8_t action_index);
void packets_init();