#include "status_bar.h"
#include "pebble.h"
#include "commons/connection/bluetooth.h"
#include "commons/connection/bucket_sync.h"
#include "connection/notification_details_fetcher.h"

// The status bar grows to fit the clock font. The "large font" mode (toggled from the phone's Tools menu and
// delivered via bucket 1 flag 0x10) uses GOTHIC_24_BOLD instead of GOTHIC_14 so the clock is easier to read.
#define STATUS_BAR_HEIGHT_SMALL 16
#define STATUS_BAR_HEIGHT_LARGE 30
#define CLOCK_WIDTH_SMALL 48
#define CLOCK_WIDTH_LARGE 86
#define CLOCK_FONT_SMALL FONT_KEY_GOTHIC_14
#define CLOCK_FONT_LARGE FONT_KEY_GOTHIC_24_BOLD
#define ICON_AREA_WIDTH 14

// Emery (and presumably future Core devices with larger displays) have a pretty big corner radius. And since there's
// more display real estate, we can afford to make some padding on the right
#if PBL_DISPLAY_WIDTH > 190
#define CORNER_PADDING 5
#else
#define CORNER_PADDING 0
#endif

// Width consumed on the right side of the bar (clock + 1px gap + status icon + 1px gap + corner padding) for a given
// clock width. The clock width is stored in the layer's data so both the paint proc and get_left_space agree on it.
static int status_bar_right_width(const int clock_width)
{
    return clock_width + 1 + ICON_AREA_WIDTH + 1 + CORNER_PADDING;
}

// Reads the "large font" preference from bucket 1 (byte 0, flag 0x10). Falls back to the small font when the bucket
// has not been synced from the phone yet.
static bool status_bar_large_font_enabled(void)
{
    uint8_t config[3];
    if (bucket_sync_load_bucket(1, config))
    {
        return (config[0] & 0x10) != 0;
    }
    return false;
}

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
    const bool large_font = status_bar_large_font_enabled();
    const int status_bar_height = large_font ? STATUS_BAR_HEIGHT_LARGE : STATUS_BAR_HEIGHT_SMALL;
    const int clock_width = large_font ? CLOCK_WIDTH_LARGE : CLOCK_WIDTH_SMALL;
    const char* clock_font = large_font ? CLOCK_FONT_LARGE : CLOCK_FONT_SMALL;

    // Store the clock width in the layer's data so the paint proc and get_left_space can reconstruct the geometry.
    Layer* layer = layer_create_with_data(GRect(0, 0, window_frame.size.w, status_bar_height), sizeof(uint8_t));
    *(uint8_t*)layer_get_data(layer) = (uint8_t)clock_width;

    TextLayer* clock_layer = text_layer_create(
        GRect(window_frame.size.w - clock_width - CORNER_PADDING - 1, 0, clock_width - 1, status_bar_height));
    text_layer_set_background_color(clock_layer, GColorClear);
    text_layer_set_text_color(clock_layer, GColorWhite);
    text_layer_set_font(clock_layer, fonts_get_system_font(clock_font));
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
    const int bar_height = whole_status_size.size.h;
    const uint8_t clock_width = *(uint8_t*)layer_get_data(layer);

    const uint16_t icon_x = whole_status_size.size.w - clock_width - 1 - ICON_AREA_WIDTH - CORNER_PADDING;
    // Vertically center the indicators within the bar (matches the original 16px-tall layout where the 13px and 10px
    // icons sat at y=1 and y=3 respectively).
    const int large_icon_y = (bar_height - 13) / 2;
    const int small_icon_y = (bar_height - 10) / 2;
    if (sending_error != APP_MSG_OK)
    {
        graphics_draw_bitmap_in_rect(ctx, indicator_error, GRect(icon_x + 3, small_icon_y, 9, 10));
    }
    else if (!is_phone_connected)
    {
        graphics_draw_bitmap_in_rect(ctx, indicator_disconnected, GRect(icon_x, large_icon_y, 14, 13));
    }
    else if (is_currently_sending_data || bucket_sync_is_currently_syncing || notification_details_fetcher_is_fetching())
    {
        graphics_draw_bitmap_in_rect(ctx, indicator_busy, GRect(icon_x + 3, small_icon_y, 9, 10));
    }
}

void custom_status_bar_set_active(CustomStatusBarLayer* layer, const bool active)
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
                notification_details_fetcher_register_fetching_status_callback(update_data);
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
    const uint8_t clock_width = *(uint8_t*)layer_get_data(layer->layer);

    return (GRect)
    {
        .
        origin = {
            .x = CORNER_PADDING,
            .y = 0
        },
        .
        size = {
            .w = whole_status_size.size.w - status_bar_right_width(clock_width) - CORNER_PADDING,
            .h = whole_status_size.size.h
        }
    };
}