#pragma once

#include "pebble.h"

void send_watch_welcome();
bool send_notification_opened(uint8_t id);
bool send_action_trigger(uint8_t notification_id, uint8_t action_id, uint8_t menu_id, const char* text);
void send_close_me();
bool send_setting(uint8_t id, uint8_t value);
bool send_reload_notifications();
bool send_request_image(uint8_t notification_id, bool crop);
void packets_init();