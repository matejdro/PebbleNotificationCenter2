#pragma once
#include "window_notification.h"

void window_notification_data_select_bucket_on_index(uint8_t target_index);
void window_notification_data_receive_more_text(uint8_t bucket_id, const uint8_t* data, size_t data_size);
void window_notification_data_app_started();
void window_notification_data_init();
void window_notification_data_deinit();