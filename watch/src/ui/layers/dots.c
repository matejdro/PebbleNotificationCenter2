#include "dots.h"

#include "commons/math.h"

#define LARGE_DOT_RADIUS 4
#define SMALL_DOT_RADIUS 3
#define LARGE_DOT_DIAMETER LARGE_DOT_RADIUS * 2 + 1
#define SMALL_DOT_DIAMETER SMALL_DOT_RADIUS * 2 + 1
#define INTER_DOT_PADDING 2
#define ARROW_HEIGHT 6
#define ARROW_WIDTH ARROW_HEIGHT / 2
#define ARROW_PADDING 4

static GBitmap* indicator_unread_large = NULL;
static GBitmap* indicator_unread_large_selected = NULL;
static GBitmap* indicator_unread_small = NULL;
static GBitmap* indicator_unread_small_selected = NULL;

static void draw_left_arrow(const int16_t pos_x, const int16_t pos_y, GContext* ctx)
{
    graphics_draw_line(ctx, GPoint(pos_x, pos_y + ARROW_HEIGHT / 2), GPoint(pos_x + ARROW_WIDTH, pos_y));
    graphics_draw_line(ctx, GPoint(pos_x, pos_y + ARROW_HEIGHT / 2), GPoint(pos_x + ARROW_WIDTH, pos_y + ARROW_HEIGHT));
}

static void draw_right_arrow(const int16_t pos_x, const int16_t pos_y, GContext* ctx)
{
    graphics_draw_line(ctx, GPoint(pos_x, pos_y), GPoint(pos_x + ARROW_WIDTH, pos_y + ARROW_HEIGHT / 2));
    graphics_draw_line(ctx, GPoint(pos_x, pos_y + ARROW_HEIGHT), GPoint(pos_x + ARROW_WIDTH, pos_y + ARROW_HEIGHT / 2));
}

// ReSharper disable once CppParameterMayBeConstPtrOrRef
static void dots_layer_paint(Layer* layer, GContext* ctx)
{
    const DotsLayer* dots_layer = *((DotsLayer**)layer_get_data(layer));

    graphics_context_set_antialiased(ctx, true);
    graphics_context_set_fill_color(ctx, GColorWhite);

    const uint8_t number_of_dots = dots_layer->number_of_dots;
    const uint8_t selected_dot = dots_layer->selected_dot;

    const uint16_t width = dots_layer->bounds.size.w;
    const uint16_t available_space_per_dot = width / number_of_dots;

    uint8_t radius;

    if (available_space_per_dot >= LARGE_DOT_DIAMETER * 2 + INTER_DOT_PADDING)
    {
        radius = LARGE_DOT_RADIUS;
    }
    else
    {
        radius = SMALL_DOT_RADIUS;
    }

    const uint8_t diameter = radius * 2 + 1;
    const uint8_t total_width_per_Dot = diameter + INTER_DOT_PADDING;


    const bool paginated = dots_layer->number_of_dots > dots_layer->max_dots_without_pages;

    uint16_t x = 0;

    if (paginated)
    {
        x += INTER_DOT_PADDING;
        if (dots_layer->current_page_first_dot_index != 0)
        {
            graphics_context_set_stroke_color(ctx, GColorWhite);
            draw_left_arrow(x, dots_layer->bounds.size.h / 2 - ARROW_HEIGHT / 2, ctx);
        }
        x += ARROW_WIDTH + ARROW_PADDING + radius;
    }
    else
    {
        x += radius;
    }

    uint8_t draw_until;
    if (paginated)
    {
        draw_until = MIN(dots_layer->current_page_first_dot_index + dots_layer->max_dots_per_page, dots_layer->number_of_dots);
    }
    else
    {
        draw_until = dots_layer->number_of_dots;
    }

    for (int i = dots_layer->current_page_first_dot_index; i < draw_until; i++)
    {
        GColor circlesColor;

        const enum DotState state = dots_layer->dot_states[i];

#ifdef  PBL_COLOR
        if (state == UNREAD)
        {
            circlesColor = GColorWhite;
        }
        else
        {
            circlesColor = GColorWhite;
        }
# else
        circlesColor = GColorWhite;
#endif

        graphics_context_set_stroke_color(ctx, circlesColor);
        graphics_context_set_fill_color(ctx, circlesColor);

        if (state == UNREAD)
        {
            if (selected_dot == i)
            {
                GBitmap* indicator;
                if (radius == SMALL_DOT_RADIUS)
                {
                    indicator = indicator_unread_small_selected;
                }
                else
                {
                    indicator = indicator_unread_large_selected;
                }

                graphics_draw_bitmap_in_rect(ctx, indicator,
                                             GRect(x - radius, 8 - radius, diameter, diameter));
            }
            else
            {
                GBitmap* indicator;
                if (radius == SMALL_DOT_RADIUS)
                {
                    indicator = indicator_unread_small;
                }
                else
                {
                    indicator = indicator_unread_large;
                }

                graphics_draw_bitmap_in_rect(ctx, indicator, GRect(x - radius, 8 - radius, diameter, diameter));
            }
        }
        else
        {
            if (selected_dot == i)
                graphics_draw_circle(ctx, GPoint(x, 8), radius);
            else
                graphics_fill_circle(ctx, GPoint(x, 8), radius);
        }

        x += total_width_per_Dot;
    }

    if (paginated && draw_until < dots_layer->number_of_dots)
    {
        draw_right_arrow(x + ARROW_PADDING - INTER_DOT_PADDING - radius, dots_layer->bounds.size.h / 2 - ARROW_HEIGHT / 2, ctx);
    }
}

DotsLayer* dots_layer_create(const GRect bounds)
{
    if (indicator_unread_large == NULL)
    {
        indicator_unread_large = gbitmap_create_with_resource(RESOURCE_ID_INDICATOR_UNREAD_LARGE);
        indicator_unread_large_selected = gbitmap_create_with_resource(RESOURCE_ID_INDICATOR_UNREAD_LARGE_SELECTED);
        indicator_unread_small = gbitmap_create_with_resource(RESOURCE_ID_INDICATOR_UNREAD_SMALL);
        indicator_unread_small_selected = gbitmap_create_with_resource(RESOURCE_ID_INDICATOR_UNREAD_SMALL_SELECTED);
    }
    DotsLayer* dots = malloc(sizeof(DotsLayer));

    dots->layer = layer_create_with_data(bounds, sizeof(dots));
    DotsLayer** layer_data = layer_get_data(dots->layer);
    *layer_data = dots;

    const uint16_t width = bounds.size.w;
    dots->max_dots_without_pages = (width + INTER_DOT_PADDING) / (SMALL_DOT_DIAMETER + INTER_DOT_PADDING);
    dots->max_dots_per_page = (width - ARROW_WIDTH * 2 - ARROW_PADDING * 2) / (SMALL_DOT_DIAMETER + INTER_DOT_PADDING);

    layer_set_update_proc(dots->layer, dots_layer_paint);

    dots->bounds = bounds;

    return dots;
}

static void fix_pages(DotsLayer* layer)
{
    if (layer->number_of_dots <= layer->max_dots_without_pages)
    {
        layer->current_page_first_dot_index = 0;
    }
    else if (layer->selected_dot < layer->current_page_first_dot_index)
    {
        const uint8_t first_page_end = layer->max_dots_per_page;

        if (layer->selected_dot < first_page_end)
        {
            layer->current_page_first_dot_index = 0;
        }
        else
        {
            layer->current_page_first_dot_index = MAX(layer->selected_dot - layer->max_dots_per_page / 2, 0);
        }
    }
    else if (layer->selected_dot >= layer->current_page_first_dot_index + layer->max_dots_per_page)
    {
        const uint8_t last_page_start = layer->number_of_dots - layer->max_dots_per_page;

        if (layer->selected_dot >= last_page_start)
        {
            layer->current_page_first_dot_index = last_page_start;
        }
        else
        {
            layer->current_page_first_dot_index = MAX(layer->selected_dot - layer->max_dots_per_page / 2, 0);
        }
    }
}

void dots_layer_set_selected_dot(DotsLayer* layer, const uint8_t selected_dot)
{
    layer->selected_dot = selected_dot;
    fix_pages(layer);
    layer_mark_dirty(layer->layer);
}

void dots_layer_set_dots(DotsLayer* layer, const uint8_t number_of_dots, const enum DotState* states)
{
    layer->number_of_dots = number_of_dots;
    layer->dot_states = states;
    fix_pages(layer);
    layer_mark_dirty(layer->layer);
}

void dots_layer_destroy(DotsLayer* layer)
{
    layer_destroy(layer->layer);
    free(layer);
}
