#include "window_preferences.h"

#include "commons/connection/bucket_sync.h"
#include "layers/status_bar.h"

static CustomStatusBarLayer* status_bar = NULL;

static SimpleMenuItem section_muting[2] = {};
static SimpleMenuSection sections[1] = {};
static SimpleMenuLayer* menu_layer = NULL;


static void toggle_phone_mute()
{
}

static void toggle_watch_mute()
{
}

static void update_data()
{
    uint8_t preferences[1];
    const bool bucket_exists = bucket_sync_load_bucket(1, preferences);
    if (!bucket_exists)
    {
        return;
    }
    if ((preferences[0] & 0x01) != 0)
    {
        section_muting[0].subtitle = "currently ON";
    }
    else
    {
        section_muting[0].subtitle = "currently OFF";
    }

    if ((preferences[0] & 0x02) != 0)
    {
        section_muting[1].subtitle = "currently ON";
    }
    else
    {
        section_muting[1].subtitle = "currently OFF";
    }

    if (menu_layer != NULL)
    {
        menu_layer_reload_data(simple_menu_layer_get_menu_layer(menu_layer));
    }
}

static void on_bucket_data_update(const BucketMetadata bucket_metadata, void* context)
{
    if (bucket_metadata.id == 1)
    {
        update_data();
    }
}

// ReSharper disable once CppParameterMayBeConstPtrOrRef
static void window_load(Window* window)
{
    Layer* window_layer = window_get_root_layer(window);
    const GRect screen_bounds = layer_get_bounds(window_layer);
    status_bar = custom_status_bar_layer_create(screen_bounds);
    const GRect status_bar_bounds = layer_get_bounds(status_bar->layer);

    GRect main_content_bounds = GRect(
        0,
        status_bar_bounds.size.h,
        screen_bounds.size.w,
        screen_bounds.size.h - status_bar_bounds.size.h
    );
    layer_add_child(window_layer, status_bar->layer);

    sections[0].title = "Muting";
    sections[0].items = section_muting;
    sections[0].num_items = 2;

    section_muting[0].title = "Mute watch";
    section_muting[0].callback = toggle_watch_mute;
    section_muting[1].title = "Mute phone";
    section_muting[1].callback = toggle_phone_mute;
    update_data();

    menu_layer = simple_menu_layer_create(main_content_bounds, window, sections, 1, NULL);
    layer_add_child(window_layer, simple_menu_layer_get_layer(menu_layer));
}

static void window_show(Window* window)
{
    custom_status_bar_set_active(status_bar, true);
    bucket_sync_set_bucket_data_change_callback(on_bucket_data_update, NULL);
}

static void window_hide(Window* window)
{
    custom_status_bar_set_active(status_bar, false);
    bucket_sync_clear_bucket_data_change_callback(on_bucket_data_update, NULL);
}

static void window_unload(Window* window)
{
    simple_menu_layer_destroy(menu_layer);
    custom_status_bar_layer_destroy(status_bar);
    window_destroy(window);
    menu_layer = NULL;
}

void window_preferences_show()
{
    Window* window = window_create();
    window_set_window_handlers(window, (WindowHandlers)
    {
        .
        load = window_load,
        .
        unload = window_unload,
        .
        appear = window_show,
        .
        disappear = window_hide
    }
    )
    ;
    window_stack_push(window, true);
}