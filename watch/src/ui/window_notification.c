#include "window_notification.h"

#include <pebble.h>

#include "window_status.h"
#include "commons/connection/bucket_sync.h"
#include "layers/dots.h"
#include "layers/status_bar.h"

typedef struct
{
    char* text;
    GFont font;
    GRect bounds;
} TextParameters;

static CustomStatusBarLayer* status_bar_layer;
static DotsLayer* dots_layer;
static BucketList* buckets;
static ScrollLayer* scroll_layer;
static Layer* scroll_content_layer;
static TextParameters title;
static TextParameters subtitle;
static TextParameters body;

static uint8_t currently_selected_bucket = 0;
static uint8_t currently_selected_bucket_index = 0;
static uint8_t bucket_count = 0;

static char title_text[21];
static char subtitle_text[21];
static char body_text[256];

const int16_t HORIZONTAL_TEXT_PADDING = 2;
const int16_t MID_TEXT_VERTICAL_PADDING = 4;

static void redraw_scroller();

static void reload_data_for_current_bucket()
{
    uint8_t bucket_data[256];

    if (!bucket_sync_load_bucket(currently_selected_bucket, bucket_data))
    {
        window_status_show_error("Missing bucket\n\nReport Bug");
        return;
    }
    uint8_t size = bucket_sync_get_bucket_size(currently_selected_bucket);

    uint8_t position = 4;
    strcpy(title_text, (char*)&bucket_data[position]);
    position += strlen(title_text) + 1;
    strcpy(subtitle_text, (char*)&bucket_data[position]);
    position += strlen(subtitle_text) + 1;
    const uint8_t body_bytes = size - position;
    strncpy(body_text, (char*)&bucket_data[position], body_bytes);
    body_text[body_bytes] = '\0';

    APP_LOG(APP_LOG_LEVEL_INFO, "Got title %s %d %s", title_text, body_bytes, body_text);

    redraw_scroller();
}

static void select_bucket_on_index(uint8_t target_index)
{
    uint8_t index_without_settings = 0;

    for (int i = 0; i < buckets->count; i++)
    {
        const uint8_t id = buckets->data[i].id;

        if (id != 1)
        {
            if (index_without_settings == target_index)
            {
                currently_selected_bucket = id;
                currently_selected_bucket_index = target_index;

                dots_layer_set_selected_dot(dots_layer, target_index);
                reload_data_for_current_bucket();
                //First scroll with animation to override animation caused by pressing buttons. Then scroll without animation to speed it up.
                scroll_layer_set_content_offset(scroll_layer, GPoint(0, 0), true);
                scroll_layer_set_content_offset(scroll_layer, GPoint(0, 0), false);
                return;
            }

            index_without_settings++;
        }
    }

    // If target_index was out of bounds, just select current bucket
    select_bucket_on_index(bucket_count - 1);
}

static void ingest_bucket_metadata()
{
    uint8_t count_without_settings = 0;
    int16_t current_bucket_index = -1;
    for (int i = 0; i < buckets->count; i++)
    {
        const uint8_t id = buckets->data[i].id;

        if (id != 1)
        {
            count_without_settings++;
        }

        if (id == currently_selected_bucket)
        {
            current_bucket_index = i;
        }
    }

    if (count_without_settings == 0)
    {
        window_stack_pop(true);
        window_status_show_empty();
        return;
    }

    dots_layer_set_number_of_dots(dots_layer, count_without_settings);
    bucket_count = count_without_settings;

    if (current_bucket_index != -1)
    {
        select_bucket_on_index(current_bucket_index);
    }
    else
    {
        select_bucket_on_index(currently_selected_bucket_index);
    }
}

static void redraw_scroller()
{
    const int16_t scroller_width = scroll_layer_get_content_size(scroll_layer).w;

    int16_t y = 0;
    title.bounds.origin = GPoint(0, 0);
    title.bounds.size = graphics_text_layout_get_content_size(
        title.text,
        title.font,
        GRect(0, 0, scroller_width, 3000),
        GTextOverflowModeWordWrap,
        GTextAlignmentLeft
    );
    y += title.bounds.size.h + MID_TEXT_VERTICAL_PADDING;

    subtitle.bounds.origin = GPoint(0, y);
    subtitle.bounds.size = graphics_text_layout_get_content_size(
        subtitle.text,
        subtitle.font,
        GRect(0, 0, scroller_width, 3000),
        GTextOverflowModeWordWrap,
        GTextAlignmentLeft
    );
    y += subtitle.bounds.size.h + MID_TEXT_VERTICAL_PADDING;

    body.bounds.origin = GPoint(0, y);
    body.bounds.size = graphics_text_layout_get_content_size(
        body.text,
        body.font,
        GRect(0, 0, scroller_width, 3000),
        GTextOverflowModeWordWrap,
        GTextAlignmentLeft
    );
    y += body.bounds.size.h + MID_TEXT_VERTICAL_PADDING;

    scroll_layer_set_content_size(scroll_layer, GSize(scroller_width, y));
    layer_set_frame(scroll_content_layer, GRect(0, 0, scroller_width, y));
    layer_mark_dirty(scroll_content_layer);
}

static void button_up(ClickRecognizerRef recognizer, void* context)
{
    scroll_layer_scroll_up_click_handler(recognizer, scroll_layer);
}

static void button_down(ClickRecognizerRef recognizer, void* context)
{
    scroll_layer_scroll_down_click_handler(recognizer, scroll_layer);
}

static void button_up_multi(ClickRecognizerRef recognizer, void* context)
{
    if (click_number_of_clicks_counted(recognizer) != 2)
    {
        button_up(recognizer, context);
        return;
    }

    if (currently_selected_bucket_index > 0)
    {
        select_bucket_on_index(currently_selected_bucket_index - 1);
    }
    else
    {
        select_bucket_on_index(bucket_count - 1);
    }
}

static void button_down_multi(ClickRecognizerRef recognizer, void* context)
{
    if (click_number_of_clicks_counted(recognizer) != 2)
    {
        button_down(recognizer, context);
        return;
    }

    if (currently_selected_bucket_index < bucket_count - 1)
    {
        select_bucket_on_index(currently_selected_bucket_index + 1);
    }
    else
    {
        select_bucket_on_index(0);
    }
}

static void buttons_config(void* context)
{
    window_multi_click_subscribe(BUTTON_ID_UP, 1, 2, 150, false, button_up_multi);
    window_multi_click_subscribe(BUTTON_ID_DOWN, 1, 2, 150, false, button_down_multi);

    window_single_repeating_click_subscribe(BUTTON_ID_UP, 100, button_up);
    window_single_repeating_click_subscribe(BUTTON_ID_DOWN, 100, button_down);
}

static void scroll_content_paint(Layer* layer, GContext* ctx)
{
    graphics_context_set_text_color(ctx, GColorBlack);
    graphics_draw_text(ctx, title.text, title.font, title.bounds, GTextOverflowModeWordWrap, GTextAlignmentLeft, NULL);
    graphics_draw_text(ctx, subtitle.text, subtitle.font, subtitle.bounds, GTextOverflowModeWordWrap, GTextAlignmentLeft, NULL);
    graphics_draw_text(ctx, body.text, body.font, body.bounds, GTextOverflowModeWordWrap, GTextAlignmentLeft, NULL);
}

static void on_buckets_changed()
{
    buckets = bucket_sync_get_bucket_list();
    ingest_bucket_metadata();
}

// ReSharper disable once CppParameterMayBeConstPtrOrRef
static void window_load(Window* window)
{
    Layer* window_layer = window_get_root_layer(window);
    const GRect screen_bounds = layer_get_bounds(window_layer);
    status_bar_layer = custom_status_bar_layer_create(screen_bounds);

    const GRect dots_bounds = custom_status_bar_get_left_space(status_bar_layer);
    dots_layer = dots_layer_create(dots_bounds);

    scroll_layer = scroll_layer_create(
        GRect(
            HORIZONTAL_TEXT_PADDING,
            dots_bounds.size.h,
            screen_bounds.size.w,
            screen_bounds.size.h - dots_bounds.size.h - HORIZONTAL_TEXT_PADDING * 2
        )
    );

    title.font = fonts_get_system_font(FONT_KEY_GOTHIC_24_BOLD);
    title.text = title_text;
    subtitle.font = fonts_get_system_font(FONT_KEY_GOTHIC_14_BOLD);
    subtitle.text = subtitle_text;
    body.font = fonts_get_system_font(FONT_KEY_GOTHIC_14);
    body.text = body_text;

    scroll_content_layer = layer_create(GRect(0, 0, 0, 0));
    layer_set_update_proc(scroll_content_layer, scroll_content_paint);

    layer_add_child(window_layer, status_bar_layer->layer);
    layer_add_child(window_layer, dots_layer->layer);
    layer_add_child(window_layer, scroll_layer_get_layer(scroll_layer));
    scroll_layer_add_child(scroll_layer, scroll_content_layer);
    scroll_layer_set_content_size(scroll_layer, GSize(screen_bounds.size.w, 0));

    window_set_click_config_provider(window, buttons_config);

    custom_status_bar_set_active(status_bar_layer, true);

    on_buckets_changed();
    bucket_sync_set_bucket_list_change_callback(on_buckets_changed);
}

static void window_unload(Window* window)
{
    bucket_sync_set_bucket_list_change_callback(NULL);
    custom_status_bar_set_active(status_bar_layer, false);
    custom_status_bar_layer_destroy(status_bar_layer);
    scroll_layer_destroy(scroll_layer);
    layer_destroy(scroll_content_layer);
    dots_layer_destroy(dots_layer);
    window_destroy(window);
}

void window_notification_show()
{
    Window* window = window_create();

    window_set_window_handlers(window, (WindowHandlers)
                               {
                                   .load = window_load,
                                   .unload = window_unload,
                               }
    );

    window_stack_push(window, true);
}
