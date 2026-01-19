#include "action_list.h"

#include <pebble.h>

#include "window_notification.h"

static const int16_t menu_outside_margin_horizontal = 8;
static const int16_t menu_outside_margin_vertical = 12;
static const int16_t item_padding = 2;

static Layer* menu_background;
static MenuLayer* menu_layer;

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

static uint16_t menu_get_num_rows_callback(MenuLayer* me, uint16_t section_index, void* data)
{
    return window_notification_data.num_actions;
}


static int16_t menu_get_row_height_callback(MenuLayer* me, MenuIndex* cell_index, void* data)
{
    return 30;
}

// ReSharper disable once CppParameterMayBeConstPtrOrRef
static void menu_draw_row_callback(GContext* ctx, const Layer* cell_layer, MenuIndex* cell_index, void* data)
{
    graphics_context_set_text_color(ctx, menu_cell_layer_is_highlighted(cell_layer) ? GColorWhite : GColorBlack);

    GRect bounds = layer_get_bounds(cell_layer);
    bounds.origin.x += item_padding;
    bounds.origin.y += item_padding;
    bounds.size.w -= item_padding * 2;
    bounds.size.h -= item_padding * 2;

    graphics_draw_text(
        ctx,
        window_notification_data.actions[cell_index->row].text,
        fonts_get_system_font(FONT_KEY_GOTHIC_18_BOLD),
        bounds,
        GTextOverflowModeTrailingEllipsis,
        GTextAlignmentLeft,
        NULL
    );
}


void window_notification_action_list_init(const Window* window)
{
    Layer* window_layer = window_get_root_layer(window);
    const GRect screen_bounds = layer_get_bounds(window_layer);

    const GRect menu_outer_frame = GRect(
        menu_outside_margin_horizontal,
        menu_outside_margin_vertical,
        screen_bounds.size.w - menu_outside_margin_horizontal * 2,
        screen_bounds.size.h - menu_outside_margin_vertical * 2
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
                             (MenuLayerCallbacks){
                                 .get_num_sections = menu_get_num_sections_callback,
                                 .get_num_rows = menu_get_num_rows_callback,
                                 .get_cell_height = menu_get_row_height_callback,
                                 .draw_row = menu_draw_row_callback,
                             });

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
    const MenuIndex index = menu_layer_get_selected_index(menu_layer);
    if (index.row == 0)
    {
        menu_layer_set_selected_index(
            menu_layer,
            MenuIndex(0, window_notification_data.num_actions - 1),
            MenuRowAlignCenter,
            true
        );
        return;
    }

    menu_layer_set_selected_next(menu_layer, true, MenuRowAlignCenter, true);
}

void window_notification_action_list_move_down()
{
    const MenuIndex index = menu_layer_get_selected_index(menu_layer);
    if (index.row == window_notification_data.num_actions - 1)
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

uint16_t window_notification_action_list_get_selected_index()
{
    return menu_layer_get_selected_index(menu_layer).row;
}
