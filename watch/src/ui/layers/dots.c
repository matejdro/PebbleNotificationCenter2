#include "dots.h"

static void dots_layer_paint(Layer* layer, GContext* ctx)
{
    DotsLayer* dots_layer = *((DotsLayer**)layer_get_data(layer));

    graphics_context_set_fill_color(ctx, GColorWhite);

    const uint8_t number_of_dots = dots_layer->number_of_dots;
    const uint8_t selected_dot = dots_layer->selected_dot;

    const uint16_t width = dots_layer->bounds.size.w;
    const uint16_t available_space_per_dot = width / number_of_dots;

    uint8_t radius;
    uint8_t padding;

    if (available_space_per_dot >= 11)
    {
        radius = 4;
        padding = 2;
    }
    else if (available_space_per_dot >= 9)
    {
        radius = 3;
        padding = 2;
    }
    else if (available_space_per_dot >= 6)
    {
        radius = 2;
        padding = 1;
    }
    else
    {
        radius = 1;
        padding = 1;
    }

    const uint8_t total_width_per_Dot = radius * 2 + 1 + padding;


    const GColor circlesColor = GColorWhite;
    graphics_context_set_stroke_color(ctx, circlesColor);
    graphics_context_set_fill_color(ctx, circlesColor);

    uint16_t x = radius;

    for (int i = 0; i < number_of_dots; i++)
    {
        if (selected_dot == i)
            graphics_fill_circle(ctx, GPoint(x, 8), radius);
            // graphics_fill_rect(ctx, GRect(x - radius, 8 - radius, radius * 2 + 1, radius * 2 + 1), 0, GCornerNone);
        else
            graphics_draw_circle(ctx, GPoint(x, 8), radius);

        x += total_width_per_Dot;
    }
}

DotsLayer* dots_layer_create(GRect bounds)
{
    DotsLayer* dots = malloc(sizeof(DotsLayer));

    dots->layer = layer_create_with_data(bounds, sizeof(dots));
    DotsLayer** layer_data = layer_get_data(dots->layer);
    *layer_data = dots;

    layer_set_update_proc(dots->layer, dots_layer_paint);

    dots->bounds = bounds;

    return dots;
}

void dots_layer_set_selected_dot(DotsLayer* layer, uint8_t selected_dot)
{
    layer->selected_dot = selected_dot;
    layer_mark_dirty(layer->layer);
}

void dots_layer_set_number_of_dots(DotsLayer* layer, uint8_t number_of_dots)
{
    layer->number_of_dots = number_of_dots;
    layer_mark_dirty(layer->layer);
}

void dots_layer_destroy(DotsLayer* layer)
{
    layer_destroy(layer->layer);
    free(layer);
}