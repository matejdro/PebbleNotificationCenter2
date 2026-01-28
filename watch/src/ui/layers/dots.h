#pragma once
#include <pebble.h>

typedef struct
{
    Layer* layer;
    uint8_t selected_dot;
    uint8_t number_of_dots;
    uint8_t current_page_first_dot_index;
    uint8_t max_dots_without_pages;
    uint8_t max_dots_per_page;
    GRect bounds;
} DotsLayer;

DotsLayer* dots_layer_create(GRect bounds);
void dots_layer_set_selected_dot(DotsLayer* layer, uint8_t selected_dot);
void dots_layer_set_number_of_dots(DotsLayer* layer, uint8_t number_of_dots);
void dots_layer_destroy(DotsLayer* layer);