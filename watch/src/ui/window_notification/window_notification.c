#include "window_notification.h"

#include <pebble.h>

#include "action_list.h"
#include "buttons.h"
#include "data_loading.h"
#include "idle_handler.h"
#include "../layers/dots.h"
#include "../layers/status_bar.h"
#include "commons/math.h"

const int16_t HORIZONTAL_TEXT_PADDING = 2;
const int16_t MID_TEXT_VERTICAL_PADDING = 4;
#define ICON_SIZE 32
#define ICON_PADDING 4
#define ICON_SIZE_AND_BOUNDS (ICON_SIZE + ICON_PADDING * 2)

NotificationWindowData window_notification_data = {
    .active = false,
    .num_actions = 0,
    .menu_displayed = false,
    .title_font = 0,
    .subtitle_font = 0,
    .body_font = 0,
    .currently_selected_bucket = 0,
    .currently_selected_bucket_index = 0,
    .bucket_count = 0,
    .open_menu_on_success = 0,
    .icon = NULL,
};

static CustomStatusBarLayer* status_bar_layer;
static DotsLayer* dots_layer;
static ScrollLayer* scroll_layer;
static Layer* scroll_content_layer;

static TextParameters title;
static TextParameters subtitle;
static TextParameters body;

static const char* fonts[] = {
    FONT_KEY_GOTHIC_14,
    FONT_KEY_GOTHIC_14_BOLD,
    FONT_KEY_GOTHIC_18,
    FONT_KEY_GOTHIC_18_BOLD,
    FONT_KEY_GOTHIC_24,
    FONT_KEY_GOTHIC_24_BOLD,
    FONT_KEY_GOTHIC_28,
    FONT_KEY_GOTHIC_28_BOLD,
    FONT_KEY_BITHAM_30_BLACK,
    FONT_KEY_BITHAM_42_BOLD,
    FONT_KEY_BITHAM_42_LIGHT,
    FONT_KEY_BITHAM_42_MEDIUM_NUMBERS,
    FONT_KEY_BITHAM_34_MEDIUM_NUMBERS,
    FONT_KEY_BITHAM_34_LIGHT_SUBSET,
    FONT_KEY_BITHAM_18_LIGHT_SUBSET,
    FONT_KEY_ROBOTO_CONDENSED_21,
    FONT_KEY_ROBOTO_BOLD_SUBSET_49,
    FONT_KEY_DROID_SERIF_28_BOLD
};


void window_notification_ui_redraw_scroller_content()
{
    const int16_t scroller_width = scroll_layer_get_content_size(scroll_layer).w;
    const int16_t max_title_width = scroller_width - ICON_SIZE_AND_BOUNDS;

    title.font = fonts_get_system_font(fonts[window_notification_data.title_font]);
    subtitle.font = fonts_get_system_font(fonts[window_notification_data.subtitle_font]);
    body.font = fonts_get_system_font(fonts[window_notification_data.body_font]);

    int16_t y = 0;
    title.bounds.origin = GPoint(HORIZONTAL_TEXT_PADDING, 0);
    title.bounds.size = graphics_text_layout_get_content_size(
        title.text,
        title.font,
        GRect(HORIZONTAL_TEXT_PADDING, 0, max_title_width, 3000),
        GTextOverflowModeWordWrap,
        GTextAlignmentLeft
    );
    y += title.bounds.size.h + MID_TEXT_VERTICAL_PADDING;

    subtitle.bounds.origin = GPoint(HORIZONTAL_TEXT_PADDING, y);
    subtitle.bounds.size = graphics_text_layout_get_content_size(
        subtitle.text,
        subtitle.font,
        GRect(HORIZONTAL_TEXT_PADDING, 0, max_title_width, 3000),
        GTextOverflowModeWordWrap,
        GTextAlignmentLeft
    );
    if (subtitle.bounds.size.h > 0)
    {
        y += subtitle.bounds.size.h + MID_TEXT_VERTICAL_PADDING;
    }

    // Even if the title and subtitle are very small, reserve at least ICON_SIZE_AND_BOUNDS height for the icon
    y = MAX(y, ICON_SIZE_AND_BOUNDS);

    body.bounds.origin = GPoint(HORIZONTAL_TEXT_PADDING, y);
    body.bounds.size = graphics_text_layout_get_content_size(
        body.text,
        body.font,
        GRect(HORIZONTAL_TEXT_PADDING, 0, scroller_width, 3000),
        GTextOverflowModeWordWrap,
        GTextAlignmentLeft
    );
    y += body.bounds.size.h + MID_TEXT_VERTICAL_PADDING;

    scroll_layer_set_content_size(scroll_layer, GSize(scroller_width, y));
    layer_set_frame(scroll_content_layer, GRect(0, 0, scroller_width, y));
    layer_mark_dirty(scroll_content_layer);
}

// ReSharper disable once CppParameterMayBeConstPtrOrRef
static void scroll_content_paint(Layer* layer, GContext* ctx)
{
    graphics_context_set_text_color(ctx, GColorBlack);
    const GRect bounds = layer_get_bounds(layer);

    graphics_draw_text(ctx, title.text, title.font, title.bounds, GTextOverflowModeWordWrap, GTextAlignmentLeft, NULL);
    graphics_draw_text(ctx, subtitle.text, subtitle.font, subtitle.bounds, GTextOverflowModeWordWrap, GTextAlignmentLeft, NULL);
    graphics_draw_text(ctx, body.text, body.font, body.bounds, GTextOverflowModeWordWrap, GTextAlignmentLeft, NULL);
    if (window_notification_data.icon != NULL)
    {
        graphics_draw_bitmap_in_rect(
            ctx,
            window_notification_data.icon,
            GRect(bounds.size.w - ICON_SIZE - ICON_PADDING, ICON_PADDING, ICON_SIZE, ICON_SIZE)
        );
    }
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
            0,
            dots_bounds.size.h,
            screen_bounds.size.w,
            screen_bounds.size.h - dots_bounds.size.h
        )
    );

    title.text = window_notification_data.title_text;
    subtitle.text = window_notification_data.subtitle_text;
    body.text = window_notification_data.body_text;

    scroll_content_layer = layer_create(GRect(0, 0, 0, 0));
    layer_set_update_proc(scroll_content_layer, scroll_content_paint);

    layer_add_child(window_layer, status_bar_layer->layer);
    layer_add_child(window_layer, dots_layer->layer);
    layer_add_child(window_layer, scroll_layer_get_layer(scroll_layer));
    scroll_layer_add_child(scroll_layer, scroll_content_layer);
    scroll_layer_set_content_size(scroll_layer, GSize(screen_bounds.size.w - HORIZONTAL_TEXT_PADDING, 0));

    window_set_click_config_provider(window, window_notification_buttons_config);

    custom_status_bar_set_active(status_bar_layer, true);

    window_notification_data.active = true;
    window_notification_action_list_init(window);

    idle_handler_register_timers();
}

static void window_unload(Window* window)
{
    window_notification_data.active = false;
    if (window_notification_data.icon != NULL)
    {
        gbitmap_destroy(window_notification_data.icon);
        window_notification_data.icon = NULL;
    }

    window_notification_action_list_deinit();
    custom_status_bar_set_active(status_bar_layer, false);
    custom_status_bar_layer_destroy(status_bar_layer);
    scroll_layer_destroy(scroll_layer);
    layer_destroy(scroll_content_layer);
    dots_layer_destroy(dots_layer);
    window_destroy(window);
}

static void window_appear(Window* window)
{
    window_notification_data_init();
}

static void window_disappear(Window* window)
{
    window_notification_data_deinit();
}


void window_notification_show()
{
    Window* window = window_create();

    window_set_window_handlers(
        window,
        (WindowHandlers)
    {
        .
        load = window_load,
        .
        unload = window_unload,
        .
        appear = window_appear,
        .
        disappear = window_disappear,
    }
    )
    ;

    window_stack_push(window, true);
}

void window_notification_ui_on_bucket_selected()
{
    dots_layer_set_selected_dot(dots_layer, window_notification_data.currently_selected_bucket_index);

    //First scroll with animation to override animation caused by pressing buttons. Then scroll without animation to speed it up.
    scroll_layer_set_content_offset(scroll_layer, GPoint(0, 0), true);
    scroll_layer_set_content_offset(scroll_layer, GPoint(0, 0), false);
}

void window_notification_ui_on_bucket_list_updated()
{
    dots_layer_set_dots(dots_layer, window_notification_data.bucket_count, window_notification_data.dot_states);
}

void window_notification_ui_scroll_by(const int16_t amount, const bool repeating)
{
    const int16_t current_position = scroll_layer_get_content_offset(scroll_layer).y;
    const int16_t max_scroll = scroll_layer_get_content_size(scroll_layer).h -
        layer_get_bounds(scroll_layer_get_layer(scroll_layer)).size.h;

    if (amount > 0 && !repeating && current_position == 0)
    {
        scroll_layer_set_content_offset(scroll_layer, GPoint(0, -max_scroll), true);
    }
    else if (amount < 0 && !repeating && current_position == -max_scroll)
    {
        scroll_layer_set_content_offset(scroll_layer, GPointZero, true);
    }
    else
    {
        scroll_layer_set_content_offset(scroll_layer, GPoint(0, current_position + amount), true);
    }
}