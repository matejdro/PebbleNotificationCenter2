#include "action_list.h"

#include <pebble.h>

#include "window_notification.h"
#include "commons/connection/bluetooth.h"
#include "connection/packets.h"

static const int16_t menu_outside_margin_top = 24;
static const int16_t menu_outside_margin_bottom = 8;
static const int16_t menu_outside_margin_horizontal = 8;
static const int16_t item_padding = 2;

static Layer* menu_background;
static MenuLayer* menu_layer;
static bool frozen = false;

static void send_notification_voice();
static void confirm_action(uint8_t notification_id, uint8_t action_index, uint8_t menu_id, const char* text);

static void menu_paint_background(Layer* layer, GContext* ctx)
{
    const GRect background_bounds = layer_get_bounds(menu_background);

    graphics_context_set_fill_color(ctx, GColorWhite);
    graphics_context_set_stroke_color(ctx, GColorBlack);
    graphics_fill_rect(ctx, background_bounds, 0, GCornerNone);
    graphics_draw_rect(ctx, background_bounds);
}

static uint16_t menu_get_num_sections_callback(MenuLayer* me, void* data)
{
    return 1;
}

static uint16_t get_num_actions(void)
{
    if (window_notification_data.currently_displayed_menu_id == 0)
    {
        return window_notification_data.num_actions;
    }
    else
    {
        return window_notification_data.num_submenu_actions;
    }
}

static uint16_t menu_get_num_rows_callback(MenuLayer* me, uint16_t section_index, void* data)
{
    return get_num_actions();
}


static int16_t menu_get_row_height_callback(MenuLayer* me, MenuIndex* cell_index, void* data)
{
    return 30;
}

// ReSharper disable once CppParameterMayBeConstPtrOrRef
static void menu_draw_row_callback(GContext* ctx, const Layer* cell_layer, MenuIndex* cell_index, void* data)
{
    GRect bounds = layer_get_bounds(cell_layer);
    bounds.origin.x += item_padding;
    bounds.origin.y += item_padding;
    bounds.size.w -= item_padding * 2;
    bounds.size.h -= item_padding * 2;

    const Action* actions;
    if (window_notification_data.currently_displayed_menu_id == 0)
    {
        actions = window_notification_data.actions;
    }
    else
    {
        actions = window_notification_data.submenu_actions;
    }


    graphics_draw_text(
        ctx,
        actions[cell_index->row].text,
        fonts_get_system_font(FONT_KEY_GOTHIC_18_BOLD),
        bounds,
        GTextOverflowModeTrailingEllipsis,
        GTextAlignmentLeft,
        NULL
    );
}

static void menu_freeze()
{
    frozen = true;
    menu_layer_set_highlight_colors(menu_layer, GColorWhite, GColorBlack);
}

static void menu_unfreeze()
{
    frozen = false;
    menu_layer_set_highlight_colors(menu_layer, GColorBlack, GColorWhite);
}

void window_notification_action_list_init(const Window* window)
{
    Layer* window_layer = window_get_root_layer(window);
    const GRect screen_bounds = layer_get_bounds(window_layer);

    const GRect menu_outer_frame = GRect(
        menu_outside_margin_horizontal,
        menu_outside_margin_top,
        screen_bounds.size.w - menu_outside_margin_horizontal * 2,
        screen_bounds.size.h - menu_outside_margin_top - menu_outside_margin_bottom
    );

    menu_background = layer_create(menu_outer_frame);
    layer_set_update_proc(menu_background, menu_paint_background);

    menu_layer = menu_layer_create(
        GRect(
            2,
            2,
            menu_outer_frame.size.w - 4,
            menu_outer_frame.size.h - 4
        )
    );

    menu_layer_set_callbacks(menu_layer,
                             NULL,
                             (MenuLayerCallbacks)
    {
        .
        get_num_sections = menu_get_num_sections_callback,
        .
        get_num_rows = menu_get_num_rows_callback,
        .
        get_cell_height = menu_get_row_height_callback,
        .
        draw_row = menu_draw_row_callback,
    }
    )
    ;

    layer_add_child(window_layer, menu_background);
    layer_add_child(menu_background, menu_layer_get_layer(menu_layer));
    layer_set_hidden(menu_background, true);
    layer_set_hidden(menu_background, true);
}

void window_notification_action_list_deinit()
{
    menu_layer_destroy(menu_layer);
    layer_destroy(menu_background);
}

void window_notification_action_list_show()
{
    menu_unfreeze();
    layer_set_hidden(menu_background, false);
    window_notification_data.menu_displayed = true;
    menu_layer_reload_data(menu_layer);
    menu_layer_set_selected_index(menu_layer, MenuIndex(0, 0), MenuRowAlignTop, false);
}

void window_notification_action_list_hide()
{
    layer_set_hidden(menu_background, true);
    window_notification_data.menu_displayed = false;
}

void window_notification_action_list_move_up()
{
    if (frozen)
    {
        return;
    }

    const MenuIndex index = menu_layer_get_selected_index(menu_layer);
    if (index.row == 0)
    {
        menu_layer_set_selected_index(
            menu_layer,
            MenuIndex(0, get_num_actions() - 1),
            MenuRowAlignCenter,
            true
        );
        return;
    }

    menu_layer_set_selected_next(menu_layer, true, MenuRowAlignCenter, true);
}

void window_notification_action_list_move_down()
{
    if (frozen)
    {
        return;
    }

    const MenuIndex index = menu_layer_get_selected_index(menu_layer);
    if (index.row == get_num_actions() - 1)
    {
        menu_layer_set_selected_index(
            menu_layer,
            MenuIndex(0, 0),
            MenuRowAlignCenter,
            true
        );
        return;
    }

    menu_layer_set_selected_next(menu_layer, false, MenuRowAlignCenter, true);
}

static void on_sending_finished(const bool success)
{
    if (success)
    {
        if (window_notification_data.open_menu_on_success != 0)
        {
            window_notification_data.currently_displayed_menu_id = window_notification_data.open_menu_on_success;
            window_notification_action_list_show();

            window_notification_data.open_menu_on_success = 0;
        }
        else
        {
            window_notification_action_list_hide();
        }
    }
    else
    {
        menu_unfreeze();
        vibes_double_pulse();
    }
}

void window_notification_action_select()
{
    if (frozen)
    {
        return;
    }
    const uint8_t action_index = menu_layer_get_selected_index(menu_layer).row;
    const uint8_t menu_id = window_notification_data.currently_displayed_menu_id;

    Action* action;

    if (menu_id == 0)
    {
        action = &window_notification_data.actions[action_index];
    }
    else
    {
        action = &window_notification_data.submenu_actions[action_index];
    }

    if (action->voice)
    {
        send_notification_voice();
        return;
    }

    const uint8_t notification_id = window_notification_data.currently_selected_bucket;
    confirm_action(notification_id, action->id, menu_id, NULL);
}

static void confirm_action(const uint8_t notification_id, const uint8_t action_id, const uint8_t menu_id, const char* text)
{
    if (!send_action_trigger(notification_id, action_id, menu_id, text))
    {
        vibes_double_pulse();
        return;
    }
    bluetooth_register_sending_finish(on_sending_finished);

    menu_freeze();
}

static void voice_callback(DictationSession* session, DictationSessionStatus status, char* transcription, void* context)
{
    if (status == DictationSessionStatusSuccess)
    {
        const uint8_t action_index = menu_layer_get_selected_index(menu_layer).row;
        uint8_t action_id;
        if (window_notification_data.currently_displayed_menu_id == 0)
        {
            action_id = window_notification_data.actions[action_index].id;
        }
        else
        {
            action_id = window_notification_data.submenu_actions[action_index].id;
        }
        const uint8_t menu_id = window_notification_data.currently_displayed_menu_id;
        const uint8_t notification_id = window_notification_data.currently_selected_bucket;
        confirm_action(notification_id, action_id, menu_id, transcription);
    }

    dictation_session_destroy(session);
}

static void send_notification_voice()
{
    DictationSession* session = dictation_session_create(300, voice_callback, NULL);

    if (session == NULL)
    {
        vibes_double_pulse();
        return;
    }

    const bool start_success = dictation_session_start(session);

    if (!start_success)
    {
        vibes_double_pulse();
    }
}