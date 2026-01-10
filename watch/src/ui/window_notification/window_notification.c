#include "window_notification.h"

#include <pebble.h>

#include "buttons.h"
#include "data_loading.h"
#include "../layers/dots.h"
#include "../layers/status_bar.h"

const int16_t HORIZONTAL_TEXT_PADDING = 2;
const int16_t MID_TEXT_VERTICAL_PADDING = 4;

NotificationWindowData window_notification_data;

static CustomStatusBarLayer* status_bar_layer;
static DotsLayer* dots_layer;
static ScrollLayer* scroll_layer;
static Layer* scroll_content_layer;

static TextParameters title;
static TextParameters subtitle;
static TextParameters body;


void window_notification_ui_redraw_scroller_content()
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
    if (subtitle.bounds.size.h > 0)
    {
        y += subtitle.bounds.size.h + MID_TEXT_VERTICAL_PADDING;
    }

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

static void scroll_content_paint(Layer* layer, GContext* ctx)
{
    graphics_context_set_text_color(ctx, GColorBlack);
    graphics_draw_text(ctx, title.text, title.font, title.bounds, GTextOverflowModeWordWrap, GTextAlignmentLeft, NULL);
    graphics_draw_text(ctx, subtitle.text, subtitle.font, subtitle.bounds, GTextOverflowModeWordWrap, GTextAlignmentLeft, NULL);
    graphics_draw_text(ctx, body.text, body.font, body.bounds, GTextOverflowModeWordWrap, GTextAlignmentLeft, NULL);
}


// ReSharper disable once CppParameterMayBeConstPtrOrRef
static void window_load(Window* window)
{
    window_notification_data.currently_selected_bucket = 0;
    window_notification_data.currently_selected_bucket_index = 0;
    window_notification_data.bucket_count = 0;

    Layer* window_layer = window_get_root_layer(window);
    const GRect screen_bounds = layer_get_bounds(window_layer);
    status_bar_layer = custom_status_bar_layer_create(screen_bounds);

    const GRect dots_bounds = custom_status_bar_get_left_space(status_bar_layer);
    dots_layer = dots_layer_create(dots_bounds);

    scroll_layer = scroll_layer_create(
        GRect(
            HORIZONTAL_TEXT_PADDING,
            dots_bounds.size.h,
            screen_bounds.size.w - HORIZONTAL_TEXT_PADDING * 2,
            screen_bounds.size.h - dots_bounds.size.h
        )
    );

    title.font = fonts_get_system_font(FONT_KEY_GOTHIC_24_BOLD);
    title.text = window_notification_data.title_text;
    subtitle.font = fonts_get_system_font(FONT_KEY_GOTHIC_14_BOLD);
    subtitle.text = window_notification_data.subtitle_text;
    body.font = fonts_get_system_font(FONT_KEY_GOTHIC_14);
    body.text = window_notification_data.body_text;

    scroll_content_layer = layer_create(GRect(0, 0, 0, 0));
    layer_set_update_proc(scroll_content_layer, scroll_content_paint);

    layer_add_child(window_layer, status_bar_layer->layer);
    layer_add_child(window_layer, dots_layer->layer);
    layer_add_child(window_layer, scroll_layer_get_layer(scroll_layer));
    scroll_layer_add_child(scroll_layer, scroll_content_layer);
    scroll_layer_set_content_size(scroll_layer, GSize(screen_bounds.size.w - HORIZONTAL_TEXT_PADDING * 2, 0));

    window_set_click_config_provider(window, window_notification_buttons_config);

    custom_status_bar_set_active(status_bar_layer, true);

    window_notification_data_init();
}

static void window_unload(Window* window)
{
    window_notification_data_deinit();
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
        .
        load = window_load,
        .
        unload = window_unload,
    }
    )
    ;

    window_stack_push(window, true);
}

void window_notification_ui_on_bucket_selected()
{
    dots_layer_set_selected_dot(dots_layer, window_notification_data.currently_selected_bucket_index);
    dots_layer_set_number_of_dots(dots_layer, window_notification_data.bucket_count);

    //First scroll with animation to override animation caused by pressing buttons. Then scroll without animation to speed it up.
    scroll_layer_set_content_offset(scroll_layer, GPoint(0, 0), true);
    scroll_layer_set_content_offset(scroll_layer, GPoint(0, 0), false);
}


void window_notification_ui_scroll_by(const int16_t amount)
{
    const int16_t current_position = scroll_layer_get_content_offset(scroll_layer).y;
    scroll_layer_set_content_offset(scroll_layer, GPoint(0, current_position + amount), true);
}