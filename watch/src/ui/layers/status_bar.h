#pragma once
#include <pebble.h>

typedef struct
{
    Layer* layer;
    TextLayer* clock_layer;
} CustomStatusBarLayer;

CustomStatusBarLayer* custom_status_bar_layer_create(GRect window_frame);
void custom_status_bar_set_active(CustomStatusBarLayer* layer, bool active);
void custom_status_bar_layer_destroy(CustomStatusBarLayer* layer);
GRect custom_status_bar_get_left_space(CustomStatusBarLayer* layer);