#pragma once
#include "pebble.h"

void window_notification_action_list_init(const Window* window);
void window_notification_action_list_deinit();

void window_notification_action_list_show();
void window_notification_action_list_hide();

void window_notification_action_list_move_up();
void window_notification_action_list_move_down();
uint16_t window_notification_action_list_get_selected_index();