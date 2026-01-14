#pragma once
#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>

void notification_details_fetcher_init();

void notification_details_fetcher_fetch(uint8_t bucket_id);
void notification_details_fetcher_on_text_received(const uint8_t* data, size_t data_size);

bool notification_details_fetcher_is_fetching(void);
void notification_details_fetcher_register_fetching_status_callback(void (*callback)());
void notification_details_fetcher_unregister_fetching_status_callback(void (*callback)());