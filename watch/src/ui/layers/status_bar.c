#include "status_bar.h"
#include "pebble.h"
#include "commons/connection/bluetooth.h"
#include "commons/connection/bucket_sync.h"

static const int STATUS_BAR_HEIGHT = 16;

// Emery (and presumably future Core devices with larger displays) have a pretty big corner radius. And since there's
// more display real estate, we can afford to make some padding on the right
#if PBL_DISPLAY_WIDTH > 190
#define CORNER_PADDING 5
#else
#define CORNER_PADDING 0
#endif

#define CLOCK_WIDTH 48
static const int STATUS_BAR_RIGHT_WIDTH = CLOCK_WIDTH + 1 + 14 + 1 + CORNER_PADDING;

static CustomStatusBarLayer* active_layer;
static bool listeners_active = false;
static char clock_text[9];
static GBitmap* indicator_busy = NULL;
static GBitmap* indicator_disconnected = NULL;
static GBitmap* indicator_error = NULL;

static void custom_status_bar_update_clock();
static void custom_status_bar_paint(Layer * layer, GContext * ctx);
static void minute_tick(void);
static void update_data();

CustomStatusBarLayer* custom_status_bar_layer_create(const GRect window_frame)
{
    Layer* layer = layer_create(GRect(0, 0, window_frame.size.w, STATUS_BAR_HEIGHT));
    TextLayer* clock_layer = text_layer_create(
        GRect(window_frame.size.w - CLOCK_WIDTH - CORNER_PADDING - 1, 0, CLOCK_WIDTH - 1, STATUS_BAR_HEIGHT));
    text_layer_set_background_color(clock_layer, GColorClear);
    text_layer_set_text_color(clock_layer, GColorWhite);
    text_layer_set_font(clock_layer, fonts_get_system_font(FONT_KEY_GOTHIC_14));
    text_layer_set_text_alignment(clock_layer, GTextAlignmentRight);

    layer_add_child(layer, text_layer_get_layer(clock_layer));
    layer_set_update_proc(layer, custom_status_bar_paint);

    CustomStatusBarLayer* status_bar_layer = malloc(sizeof(CustomStatusBarLayer));
    status_bar_layer->layer = layer;
    status_bar_layer->clock_layer = clock_layer;

    if (indicator_busy == NULL)
    {
        indicator_busy = gbitmap_create_with_resource(RESOURCE_ID_INDICATOR_BUSY);
        indicator_disconnected = gbitmap_create_with_resource(RESOURCE_ID_INDICATOR_DISCONNECTED);
        indicator_error = gbitmap_create_with_resource(RESOURCE_ID_INDICATOR_ERROR);
    }

    return status_bar_layer;
}

// ReSharper disable once CppParameterMayBeConstPtrOrRef
static void custom_status_bar_paint(Layer* layer, GContext* ctx)
{
    const GColor background_color = GColorBlack;

    graphics_context_set_fill_color(ctx, background_color);
    graphics_fill_rect(ctx, layer_get_frame(layer), 0, GCornerNone);

    const GRect whole_status_size = layer_get_bounds(layer);

    const uint16_t icon_x = whole_status_size.size.w - CLOCK_WIDTH - 1 - 14 - CORNER_PADDING;
    if (got_sending_error)
    {
        graphics_draw_bitmap_in_rect(ctx, indicator_error, GRect(icon_x + 3, 3, 9, 10));
    }
    else if (!is_phone_connected)
    {
        graphics_draw_bitmap_in_rect(ctx, indicator_disconnected, GRect(icon_x, 1, 14, 13));
    }
    else if (is_currently_sending_data || bucket_sync_is_currently_syncing)
    {
        graphics_draw_bitmap_in_rect(ctx, indicator_busy, GRect(icon_x + 3, 3, 9, 10));
    }
}

void custom_status_bar_set_active(CustomStatusBarLayer* layer, bool active)
{
    if (active)
    {
        if (active_layer != layer)
        {
            active_layer = layer;
            if (!listeners_active)
            {
                // ReSharper disable once CppRedundantCastExpression
                tick_timer_service_subscribe(MINUTE_UNIT, (TickHandler)minute_tick);
                bluetooth_register_phone_connected_change_callback(update_data);
                bluetooth_register_sending_error_status_callback(update_data);
                bluetooth_register_sending_now_change_callback(update_data);
                bucket_sync_register_syncing_status_changed_callback(update_data);
                listeners_active = true;
            }

            clock_text[0] = 0; // Clear clock cache to force clock to update
            custom_status_bar_update_clock();
        }
    }
    else if (active_layer == layer)
    {
        active_layer = NULL;
        if (listeners_active)
        {
            // ReSharper disable once CppRedundantCastExpression
            tick_timer_service_unsubscribe();
            listeners_active = false;
        }
    }
}

static void custom_status_bar_update_clock()
{
    const CustomStatusBarLayer* local_active_layer = active_layer;
    if (active_layer == NULL)
    {
        return;
    }

    const time_t now = time(NULL);
    const struct tm* lTime = (const struct tm*)localtime(&now);

    char* format_string;
    if (clock_is_24h_style())
        format_string = "%H:%M";
    else
        format_string = "%I:%M %p";

    char tmp_clock_text[9];
    // ReSharper disable once CppIncompatiblePointerConversion
    strftime(tmp_clock_text, 9, format_string, lTime);

    //Only update screen when actual clock changes
    if (strcmp(tmp_clock_text, clock_text) != 0)
    {
        strcpy(clock_text, tmp_clock_text);
        text_layer_set_text(local_active_layer->clock_layer, clock_text);
    }
}

void custom_status_bar_layer_destroy(CustomStatusBarLayer* layer)
{
    layer_destroy(layer->layer);
    text_layer_destroy(layer->clock_layer);
    free(layer);
}

static void minute_tick(void)
{
    custom_status_bar_update_clock();
}

static void update_data()
{
    const CustomStatusBarLayer* local_active_layer = active_layer;
    if (active_layer == NULL)
    {
        return;
    }

    layer_mark_dirty(local_active_layer->layer);
}

GRect custom_status_bar_get_left_space(CustomStatusBarLayer* layer)
{
    const GRect whole_status_size = layer_get_bounds(layer->layer);

    return (GRect)
    {
        .
        origin = {
            .x = CORNER_PADDING,
            .y = 0
        },
        .
        size = {
            .w = whole_status_size.size.w - STATUS_BAR_RIGHT_WIDTH - CORNER_PADDING,
            .h = whole_status_size.size.h
        }
    };
}