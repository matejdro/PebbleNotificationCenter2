#include "window_image.h"

#include "commons/bytes.h"
#include "connection/packets.h"

// Zoom levels as multiples of the fit size: 0 = fit (1.0x), 4 = 2.0x
#define IMAGE_ZOOM_LEVEL_FIT 0
#define IMAGE_ZOOM_LEVEL_MAX 4

// Margin added on top of (PNG buffer + decoded pixels) to cover decoder
// structures and transient allocations
#define IMAGE_MEM_MARGIN 2048

// Decoded pixel footprint: 1 byte per pixel on color watches, 1 byte per 8
// pixels on monochrome watches (diorite). The monochrome case must be a
// division of the surface: the integer expression 1/8 evaluates to 0 in C.
#if defined(PBL_COLOR)
#define IMAGE_PIXEL_BYTES(width, height) ((size_t)(width) * (height))
#else
#define IMAGE_PIXEL_BYTES(width, height) (((size_t)(width) * (height) + 7) / 8)
#endif

// Pan step of the zoomed image: 1/4 of the corresponding screen dimension
#define IMAGE_PAN_STEP(dimension) ((dimension) / 4)

// Delay between zoom step-down retransmissions: the send must run outside the
// app_message inbox callback context AND while the phone is not mid-stream of
// the previous level (app_message_outbox_begin() fails while the phone is
// still pushing), so a failed send is retried on this interval until it
// succeeds or the retry budget is exhausted
#define IMAGE_STEP_DOWN_RETRY_MS 500

// Maximum number of retransmissions for a single step-down before giving up
// (500 ms * 20 = 10 s, well beyond any image stream the phone can produce)
#define IMAGE_STEP_DOWN_MAX_RETRIES 20

// Window during which a second press on the same button counts as a double
// click; must be >= the 150 ms multi-click recognizer window
#define IMAGE_DOUBLE_CLICK_WINDOW_MS 250

#define IMAGE_COUNTER_WIDTH 40
#define IMAGE_COUNTER_HEIGHT 24

static uint8_t* bitmap_data = NULL;
static size_t bitmap_data_position = 0;
static GBitmap* bitmap = NULL;
static Layer* drawing_layer = NULL;
static ScrollLayer* scroll_layer = NULL;
static Layer* bitmap_layer = NULL;
static TextLayer* image_counter_layer = NULL;
static char image_counter_text[8] = {0};
static uint8_t notification_id;
static uint8_t current_image_index = 0;
static uint8_t total_image_count = 1;
// Last zoom level the watch requested: also the level the in-flight sequence
// is expected to match; decremented and re-sent on a failed check
static uint8_t requested_zoom_level = IMAGE_ZOOM_LEVEL_FIT;
// The current image cannot be loaded (unavailable, or error at fit level):
// "Impossible to load" is shown instead of the image
static bool image_unavailable = false;
// The watch sent a packet 15 (zoom, photo change, level retry) and has not
// yet consumed the response
static bool expecting_response = false;
// The first packet of the current sequence was accepted: subsequent packets
// of the sequence are processed, otherwise the whole sequence is discarded
static bool sequence_accepted = false;
// A zoom step-down retransmission is scheduled but not yet sent (AppTimer
// armed): the non-NULL handle is the pending state itself, a separate flag
// would be redundant
static AppTimer* step_down_timer = NULL;
// Number of retransmissions already attempted for the pending step-down; reset
// whenever a new step-down is scheduled
static uint8_t step_down_retries = 0;
// First press of a potential UP/DOWN double click: the vertical step of the
// first press is deferred (the offset is recorded here) and only applied by
// the double-click timer if no second press arrives; a second press turns it
// into a horizontal pan, so a double never shows any vertical movement. The
// commit-armed flag tells the timer whether the step still has to be applied
// (false = the pending state only swallows a late raw second-press event)
static bool up_double_pending = false;
static bool up_double_commit_armed = false;
static GPoint up_baseline;
static AppTimer* up_double_timer = NULL;
static bool down_double_pending = false;
static bool down_double_commit_armed = false;
static GPoint down_baseline;
static AppTimer* down_double_timer = NULL;

// Packet 11 header (11 bytes, then PNG):
// 0 = notification id, 1-2 = total size, 3 = flags, 4 = image index,
// 5 = image count, 6-7 = width, 8-9 = height, 10 = zoom level, 11+ = PNG
static bool image_sequence_is_acceptable(const uint8_t flags, const uint8_t packet_id, const uint8_t image_index, const uint8_t zoom_level)
{
    if ((flags & 0x10) != 0)
    {
        // "Show image" push: always accepted (the only legitimate case in
        // which the window can open without a preceding packet 15)
        return true;
    }

    if (!expecting_response)
    {
        // Late response (e.g. the phone is still processing zoom levels
        // after a BACK): discard it so the window never re-opens
        return false;
    }

    // Response to a packet 15: the (notificationId, imageIndex, zoomLevel)
    // tuple must match the pending request; a residual level mismatch means
    // the sequence is stale and is discarded
    if (packet_id != notification_id || image_index != current_image_index)
    {
        return false;
    }

    return zoom_level == requested_zoom_level;
}

static bool image_fits_in_free_ram(const uint16_t width, const uint16_t height, const uint16_t total_size)
{
    const size_t pixel_bytes = IMAGE_PIXEL_BYTES(width, height);
    const size_t footprint = total_size + pixel_bytes + IMAGE_MEM_MARGIN;

    // Memory check against the RAM currently free on the watch, using
    // heap_bytes_free() (present in this SDK for basalt/diorite/emery;
    // app_mem_get_size/app_mem_get_used do not exist in this SDK)
    return footprint <= heap_bytes_free();
}

// Forward declaration: the retry-exhaustion path below falls back to the
// error state, whose helper is defined further down in the file
static void show_impossible_to_load(void);

// Sends the step-down packet 15, retrying on a backoff when the send fails.
// A send fails while the phone is still streaming the previous zoom level
// (the AppMessage outbox is not usable mid-push) or right after the
// app_message inbox callback, so a single attempt can never be trusted: retry
// until the outbox is usable, then give up and show the error state
static void step_down_timer_fired(void* data)
{
    step_down_timer = NULL;

    if (drawing_layer == NULL)
    {
        // The window was unloaded while the retransmission was pending (e.g.
        // a BACK): do not send a stale packet 15
        expecting_response = false;
        return;
    }

    const bool success = send_request_image(notification_id, requested_zoom_level, current_image_index);
    if (success)
    {
        expecting_response = true;
        step_down_retries = 0;
        return;
    }

    if (step_down_retries < IMAGE_STEP_DOWN_MAX_RETRIES)
    {
        // The phone is (almost certainly) still streaming the previous level:
        // wait a bit and try again
        step_down_retries++;
        expecting_response = true;
        step_down_timer = app_timer_register(IMAGE_STEP_DOWN_RETRY_MS, step_down_timer_fired, NULL);
    }
    else
    {
        // Out of retries: the outbox never became usable (the watch is most
        // likely disconnected) -> fall back to the error state instead of
        // staying on "Loading..." forever
        expecting_response = false;
        if (bitmap_data != NULL)
        {
            free(bitmap_data);
            bitmap_data = NULL;
        }
        show_impossible_to_load();
    }
}

static void schedule_step_down(void)
{
    expecting_response = true;
    step_down_retries = 0;

    if (step_down_timer == NULL)
    {
        step_down_timer = app_timer_register(IMAGE_STEP_DOWN_RETRY_MS, step_down_timer_fired, NULL);
    }
}

// Ask the phone for one zoom level lower; the watch steps down the scale
// (2x -> 1.75x -> 1.5x -> 1.25x -> fit) on its own. The level decrement is
// synchronous, the retransmission is scheduled on the app event loop (this
// function runs in the app_message inbox callback context, where a direct
// outbox send always fails)
static bool step_down_zoom_level(void)
{
    if (requested_zoom_level == IMAGE_ZOOM_LEVEL_FIT)
    {
        return false;
    }

    requested_zoom_level--;
    schedule_step_down();
    return true;
}

// Error state: the image cannot be loaded even at fit level; "Impossible to
// load" is shown in the center of the screen (the image still counts in N/M)
static void show_impossible_to_load(void)
{
    image_unavailable = true;
    bitmap = NULL;
    expecting_response = false;
    sequence_accepted = false;

    if (drawing_layer != NULL)
    {
        layer_mark_dirty(drawing_layer);
    }
}

static void update_image_counter(const uint8_t image_count)
{
    snprintf(image_counter_text, sizeof(image_counter_text), "%u/%u", current_image_index + 1, image_count);
    text_layer_set_text(image_counter_layer, image_counter_text);
    layer_set_hidden(text_layer_get_layer(image_counter_layer), image_count <= 1);
}

// ReSharper disable once CppParameterMayBeConstPtrOrRef
static void bitmap_layer_paint(Layer* layer, GContext* ctx)
{
    const GRect layer_bounds = layer_get_bounds(layer);

    graphics_context_set_fill_color(ctx, GColorBlack);
    graphics_fill_rect(ctx, layer_bounds, 0, GCornerNone);

    if (bitmap != NULL)
    {
        // The layer is sized to the bitmap: it reuses the decoded bitmap,
        // it does not duplicate it
        graphics_draw_bitmap_in_rect(ctx, bitmap, layer_bounds);
    }
}

// Centered content offset: centered on the axes where the bitmap overflows
// the screen, 0 on the other axes
static GPoint image_centered_scroll_offset(const GSize content_size, const GSize view_size)
{
    int16_t x = 0;
    int16_t y = 0;

    if (content_size.w > view_size.w)
    {
        x = (view_size.w - content_size.w) / 2;
    }
    if (content_size.h > view_size.h)
    {
        y = (view_size.h - content_size.h) / 2;
    }

    return GPoint(x, y);
}

// (Re)configures the scroll layer for the current bitmap: in zoom mode the
// content is sized to the bitmap and centered; otherwise the scroll layer is
// zeroed (fit mode draws the bitmap centered on the root layer, a NULL bitmap
// shows "Loading..."/"Impossible to load" on it)
static void update_scroll_layer(void)
{
    if (scroll_layer == NULL)
    {
        return;
    }

    if (bitmap == NULL)
    {
        layer_set_frame(bitmap_layer, GRect(0, 0, 0, 0));
        scroll_layer_set_content_size(scroll_layer, GSize(0, 0));
        scroll_layer_set_content_offset(scroll_layer, GPoint(0, 0), false);
        return;
    }

    if (drawing_layer == NULL)
    {
        return;
    }

    const GSize bitmap_size = gbitmap_get_bounds(bitmap).size;
    const GSize view_size = layer_get_bounds(drawing_layer).size;

    if (bitmap_size.w <= view_size.w && bitmap_size.h <= view_size.h)
    {
        // Fit mode: the bitmap is drawn centered by the root layer
        layer_set_frame(bitmap_layer, GRect(0, 0, 0, 0));
        scroll_layer_set_content_size(scroll_layer, GSize(0, 0));
        scroll_layer_set_content_offset(scroll_layer, GPoint(0, 0), false);
        return;
    }

    // Zoom mode: the content is sized to the bitmap and starts centered
    layer_set_frame(bitmap_layer, GRect(0, 0, bitmap_size.w, bitmap_size.h));
    scroll_layer_set_content_size(scroll_layer, bitmap_size);
    scroll_layer_set_content_offset(scroll_layer, image_centered_scroll_offset(bitmap_size, view_size), false);
    layer_mark_dirty(bitmap_layer);
}

// True while the bitmap overflows the screen in at least one dimension
static bool image_is_zoomed(void)
{
    if (bitmap == NULL || drawing_layer == NULL)
    {
        return false;
    }

    const GSize bitmap_size = gbitmap_get_bounds(bitmap).size;
    const GSize view_size = layer_get_bounds(drawing_layer).size;

    return bitmap_size.w > view_size.w || bitmap_size.h > view_size.h;
}

// Pan the zoomed image one step vertically (single click UP/DOWN): UP moves
// the image down (showing its top), DOWN moves it up (showing its bottom)
static void pan_image_vertical(const bool down)
{
    if (bitmap == NULL || scroll_layer == NULL || drawing_layer == NULL)
    {
        return;
    }

    const GSize bitmap_size = gbitmap_get_bounds(bitmap).size;
    const GSize view_size = layer_get_bounds(drawing_layer).size;

    // No overflow on this axis: the handler does nothing (no feedback)
    if (bitmap_size.h <= view_size.h)
    {
        return;
    }

    GPoint offset = scroll_layer_get_content_offset(scroll_layer);
    const int16_t step = IMAGE_PAN_STEP(view_size.h);
    offset.y += down ? -step : step;

    const int16_t min_y = (int16_t)(view_size.h - bitmap_size.h);
    if (offset.y < min_y)
    {
        offset.y = min_y;
    }
    else if (offset.y > 0)
    {
        offset.y = 0;
    }

    scroll_layer_set_content_offset(scroll_layer, offset, false);
}

// Pan the zoomed image one step horizontally (double click UP/DOWN): UP looks
// at the left part (the image moves right), DOWN at the right part (the image
// moves left)
static void pan_image_horizontal(const bool right)
{
    if (bitmap == NULL || scroll_layer == NULL || drawing_layer == NULL)
    {
        return;
    }

    const GSize bitmap_size = gbitmap_get_bounds(bitmap).size;
    const GSize view_size = layer_get_bounds(drawing_layer).size;

    // No overflow on this axis: the handler does nothing (no feedback)
    if (bitmap_size.w <= view_size.w)
    {
        return;
    }

    GPoint offset = scroll_layer_get_content_offset(scroll_layer);
    const int16_t step = IMAGE_PAN_STEP(view_size.w);
    offset.x += right ? step : -step;

    const int16_t min_x = (int16_t)(view_size.w - bitmap_size.w);
    if (offset.x < min_x)
    {
        offset.x = min_x;
    }
    else if (offset.x > 0)
    {
        offset.x = 0;
    }

    scroll_layer_set_content_offset(scroll_layer, offset, false);
}

// Sets the vertical content offset to the given value, clamped with the same
// expression the vertical pan handler uses: the restore stays safe even if the
// content size changed since the value was recorded (new image or zoom level)
static void restore_vertical_scroll_offset(const int16_t y)
{
    if (bitmap == NULL || scroll_layer == NULL || drawing_layer == NULL)
    {
        return;
    }

    const GSize bitmap_size = gbitmap_get_bounds(bitmap).size;
    const GSize view_size = layer_get_bounds(drawing_layer).size;

    // No vertical overflow: the offset stays where update_scroll_layer left
    // it (the vertical pan handler does nothing in that case)
    if (bitmap_size.h <= view_size.h)
    {
        return;
    }

    GPoint offset = scroll_layer_get_content_offset(scroll_layer);
    offset.y = y;

    const int16_t min_y = (int16_t)(view_size.h - bitmap_size.h);
    if (offset.y < min_y)
    {
        offset.y = min_y;
    }
    else if (offset.y > 0)
    {
        offset.y = 0;
    }

    scroll_layer_set_content_offset(scroll_layer, offset, false);
}

// ReSharper disable once CppParameterMayBeConstPtrOrRef
static void image_layer_paint(Layer* layer, GContext* ctx)
{
    const GRect layer_bounds = layer_get_bounds(layer);
    graphics_context_set_fill_color(ctx, GColorBlack);
    graphics_fill_rect(ctx, layer_bounds, 0, GCornerNone);

    if (bitmap == NULL)
    {
        graphics_context_set_text_color(ctx, GColorWhite);
        const char* message = image_unavailable ? "Impossible to load" : "Loading...";
        // Centered horizontally and vertically: GTextAlignmentCenter only
        // centers horizontally, so the text is drawn into a one-line box
        // positioned at the vertical middle of the screen
        const GFont font = fonts_get_system_font(FONT_KEY_GOTHIC_18_BOLD);
        const int16_t line_height = 21; // GOTHIC_18 line height (no fonts_get_line_height in this SDK)
        const int16_t y = (layer_bounds.size.h - line_height) / 2;
        const GRect text_bounds = GRect(0, y, layer_bounds.size.w, line_height);
        graphics_draw_text(ctx, message, font, text_bounds, GTextOverflowModeWordWrap, GTextAlignmentCenter, NULL);
    }
    else
    {
        const GRect image_bounds = gbitmap_get_bounds(bitmap);

        // Fit mode: the bitmap is drawn centered (black bands); in zoom mode
        // it is shown by the scroll layer, nothing to draw here
        if (image_bounds.size.w <= layer_bounds.size.w && image_bounds.size.h <= layer_bounds.size.h)
        {
            const int16_t x = (layer_bounds.size.w - image_bounds.size.w) / 2;
            const int16_t y = (layer_bounds.size.h - image_bounds.size.h) / 2;

            graphics_draw_bitmap_in_rect(ctx, bitmap, GRect(x, y, image_bounds.size.w, image_bounds.size.h));
        }
    }
}

static void button_select_single(ClickRecognizerRef recognizer, void* context)
{
    if (bitmap == NULL || drawing_layer == NULL)
    {
        // Still loading (or unavailable): don't send requests, that would
        // cross the 0x10 push or an in-flight sequence
        vibes_double_pulse();
        return;
    }

    const GSize bitmap_size = gbitmap_get_bounds(bitmap).size;
    const GSize window_size = layer_get_bounds(drawing_layer).size;

    if (bitmap_size.w > window_size.w || bitmap_size.h > window_size.h)
    {
        // Zoom active: go back to fit (index unchanged)
        requested_zoom_level = IMAGE_ZOOM_LEVEL_FIT;
    }
    else
    {
        // Start the zoom scale at the highest level, stepping down on failure
        requested_zoom_level = IMAGE_ZOOM_LEVEL_MAX;
    }

    const bool success = send_request_image(notification_id, requested_zoom_level, current_image_index);
    if (!success)
    {
        vibes_double_pulse();
        return;
    }

    expecting_response = true;

    gbitmap_destroy(bitmap);
    bitmap = NULL;
    update_scroll_layer();
    layer_mark_dirty(drawing_layer);
}

static void switch_to_next_image(ClickRecognizerRef recognizer, void* context)
{
    requested_zoom_level = IMAGE_ZOOM_LEVEL_FIT;
    current_image_index = (uint8_t)((current_image_index + 1 + total_image_count) % total_image_count);

    if (bitmap != NULL)
    {
        gbitmap_destroy(bitmap);
        bitmap = NULL;
    }
    if (bitmap_data != NULL)
    {
        free(bitmap_data);
        bitmap_data = NULL;
    }
    bitmap_data_position = 0;
    update_scroll_layer();

    const bool success = send_request_image(notification_id, requested_zoom_level, current_image_index);
    if (!success)
    {
        vibes_double_pulse();
        return;
    }

    expecting_response = true;

    if (drawing_layer != NULL)
    {
        layer_mark_dirty(drawing_layer);
    }
}

static void switch_to_previous_image(ClickRecognizerRef recognizer, void* context)
{
    requested_zoom_level = IMAGE_ZOOM_LEVEL_FIT;
    current_image_index = (uint8_t)((current_image_index + total_image_count - 1 + total_image_count) % total_image_count);

    if (bitmap != NULL)
    {
        gbitmap_destroy(bitmap);
        bitmap = NULL;
    }
    if (bitmap_data != NULL)
    {
        free(bitmap_data);
        bitmap_data = NULL;
    }
    bitmap_data_position = 0;
    update_scroll_layer();

    const bool success = send_request_image(notification_id, requested_zoom_level, current_image_index);
    if (!success)
    {
        vibes_double_pulse();
        return;
    }

    expecting_response = true;

    if (drawing_layer != NULL)
    {
        layer_mark_dirty(drawing_layer);
    }
}

static void up_double_timer_fired(void* data)
{
    // No second press arrived: commit the deferred vertical step (the press
    // was a single, or a hold is no longer in its first window)
    if (up_double_commit_armed)
    {
        pan_image_vertical(false);
    }
    up_double_commit_armed = false;
    up_double_pending = false;
    up_double_timer = NULL;
}

static void down_double_timer_fired(void* data)
{
    // No second press arrived: commit the deferred vertical step (the press
    // was a single, or a hold is no longer in its first window)
    if (down_double_commit_armed)
    {
        pan_image_vertical(true);
    }
    down_double_commit_armed = false;
    down_double_pending = false;
    down_double_timer = NULL;
}

static void button_up_single(ClickRecognizerRef recognizer, void* context)
{
    if (click_recognizer_is_repeating(recognizer))
    {
        // Hold-repetition: pan immediately and kill the first-press commit,
        // so the hold flows smoothly without an extra step at 250 ms
        up_double_commit_armed = false;
        if (up_double_timer != NULL)
        {
            app_timer_cancel(up_double_timer);
            up_double_timer = NULL;
        }
        up_double_pending = false;
        pan_image_vertical(false);
        return;
    }

    if (up_double_pending)
    {
        // Second press of a double (or a late raw event after the multi
        // handler already handled it): swallow it, the vertical step is
        // cancelled (or was never armed)
        up_double_commit_armed = false;
        up_double_pending = false;
        if (up_double_timer != NULL)
        {
            app_timer_cancel(up_double_timer);
            up_double_timer = NULL;
        }
        return;
    }

    if (image_is_zoomed())
    {
        // First press while zoomed: remember the offset and arm the commit;
        // the step is applied by the timer only if no second press follows
        up_baseline = scroll_layer_get_content_offset(scroll_layer);
        up_double_pending = true;
        up_double_commit_armed = true;
        if (up_double_timer == NULL)
        {
            up_double_timer = app_timer_register(IMAGE_DOUBLE_CLICK_WINDOW_MS, up_double_timer_fired, NULL);
        }
        return;
    }

    // Fit mode (or not zoomed): a double means photo browse, no vertical
    // overflow is possible -> immediate pan is a harmless no-op
    pan_image_vertical(false);
}

static void button_down_single(ClickRecognizerRef recognizer, void* context)
{
    if (click_recognizer_is_repeating(recognizer))
    {
        // Hold-repetition: pan immediately and kill the first-press commit,
        // so the hold flows smoothly without an extra step at 250 ms
        down_double_commit_armed = false;
        if (down_double_timer != NULL)
        {
            app_timer_cancel(down_double_timer);
            down_double_timer = NULL;
        }
        down_double_pending = false;
        pan_image_vertical(true);
        return;
    }

    if (down_double_pending)
    {
        // Second press of a double (or a late raw event after the multi
        // handler already handled it): swallow it, the vertical step is
        // cancelled (or was never armed)
        down_double_commit_armed = false;
        down_double_pending = false;
        if (down_double_timer != NULL)
        {
            app_timer_cancel(down_double_timer);
            down_double_timer = NULL;
        }
        return;
    }

    if (image_is_zoomed())
    {
        // First press while zoomed: remember the offset and arm the commit;
        // the step is applied by the timer only if no second press follows
        down_baseline = scroll_layer_get_content_offset(scroll_layer);
        down_double_pending = true;
        down_double_commit_armed = true;
        if (down_double_timer == NULL)
        {
            down_double_timer = app_timer_register(IMAGE_DOUBLE_CLICK_WINDOW_MS, down_double_timer_fired, NULL);
        }
        return;
    }

    // Fit mode (or not zoomed): a double means photo browse, no vertical
    // overflow is possible -> immediate pan is a harmless no-op
    pan_image_vertical(true);
}

static void button_up_repeating(ClickRecognizerRef recognizer, void* context)
{
    if (click_recognizer_is_repeating(recognizer))
    {
        button_up_single(recognizer, context);
    }
}

static void button_down_repeating(ClickRecognizerRef recognizer, void* context)
{
    if (click_recognizer_is_repeating(recognizer))
    {
        button_down_single(recognizer, context);
    }
}

static void button_up_multi(ClickRecognizerRef recognizer, void* context)
{
    if (image_is_zoomed())
    {
        // Confirmed double: the vertical step was never applied (deferred),
        // restore the baseline just in case, then pan horizontally (UP looks
        // at the left part, the image moves right)
        up_double_pending = false;
        if (up_double_timer != NULL)
        {
            app_timer_cancel(up_double_timer);
            up_double_timer = NULL;
        }
        restore_vertical_scroll_offset(up_baseline.y);
        pan_image_horizontal(true);

        // If this handler ran before the raw handler of the same second
        // press, re-arm the swallow so that late raw event cannot commit a
        // vertical step
        up_double_pending = true;
        up_double_commit_armed = false;
        if (up_double_timer == NULL)
        {
            up_double_timer = app_timer_register(IMAGE_DOUBLE_CLICK_WINDOW_MS, up_double_timer_fired, NULL);
        }
    }
    else
    {
        switch_to_previous_image(recognizer, context);
    }
}

static void button_down_multi(ClickRecognizerRef recognizer, void* context)
{
    if (image_is_zoomed())
    {
        // Confirmed double: the vertical step was never applied (deferred),
        // restore the baseline just in case, then pan horizontally (DOWN
        // looks at the right part, the image moves left)
        down_double_pending = false;
        if (down_double_timer != NULL)
        {
            app_timer_cancel(down_double_timer);
            down_double_timer = NULL;
        }
        restore_vertical_scroll_offset(down_baseline.y);
        pan_image_horizontal(false);

        // If this handler ran before the raw handler of the same second
        // press, re-arm the swallow so that late raw event cannot commit a
        // vertical step
        down_double_pending = true;
        down_double_commit_armed = false;
        if (down_double_timer == NULL)
        {
            down_double_timer = app_timer_register(IMAGE_DOUBLE_CLICK_WINDOW_MS, down_double_timer_fired, NULL);
        }
    }
    else
    {
        switch_to_next_image(recognizer, context);
    }
}

static void button_back_single(ClickRecognizerRef recognizer, void* context)
{
    // Inert if the window was already popped by the double handler
    window_stack_pop(true);
}

static void button_back_double(ClickRecognizerRef recognizer, void* context)
{
    // Returns to the notification list (and doesn't "leak" a BACK press to
    // it, where it would close the app)
    window_stack_pop(true);
}

static void buttons_config()
{
    window_single_click_subscribe(BUTTON_ID_SELECT, button_select_single);

    // Double click: browse the photos in fit mode, pan horizontally in zoom mode
    window_multi_click_subscribe(BUTTON_ID_UP, 2, 2, 150, false, button_up_multi);
    window_multi_click_subscribe(BUTTON_ID_DOWN, 2, 2, 150, false, button_down_multi);

    // Single click: inert in fit mode, vertical pan (with hold repetition) in
    // zoom mode; a single press pans vertically after the double-click window
    // unless a second press turns it into a horizontal pan. The raw "button
    // down" event gives the immediate response, the repeating recognizer
    // drives the hold
    window_single_repeating_click_subscribe(BUTTON_ID_UP, 100, button_up_repeating);
    window_single_repeating_click_subscribe(BUTTON_ID_DOWN, 100, button_down_repeating);
    window_raw_click_subscribe(BUTTON_ID_UP, button_up_single, NULL, NULL);
    window_raw_click_subscribe(BUTTON_ID_DOWN, button_down_single, NULL, NULL);

    window_single_click_subscribe(BUTTON_ID_BACK, button_back_single);
    window_multi_click_subscribe(BUTTON_ID_BACK, 2, 2, 150, true, button_back_double);
}

// ReSharper disable once CppParameterMayBeConstPtrOrRef
static void window_load(Window* window)
{
    Layer* window_layer = window_get_root_layer(window);
    drawing_layer = window_layer;
    layer_set_update_proc(window_layer, image_layer_paint);

    const GRect screen_bounds = layer_get_bounds(window_layer);

    // Scroll layer over the whole screen: its content hosts the bitmap layer
    // (sized to the bitmap); the bitmap is reused, not duplicated
    scroll_layer = scroll_layer_create(screen_bounds);
    bitmap_layer = layer_create(GRect(0, 0, 0, 0));
    layer_set_update_proc(bitmap_layer, bitmap_layer_paint);
    layer_add_child(window_layer, scroll_layer_get_layer(scroll_layer));
    scroll_layer_add_child(scroll_layer, bitmap_layer);
    scroll_layer_set_content_size(scroll_layer, GSize(0, 0));

    const GRect counter_bounds = GRect(
        screen_bounds.size.w - IMAGE_COUNTER_WIDTH,
        0,
        IMAGE_COUNTER_WIDTH,
        IMAGE_COUNTER_HEIGHT
    );
    image_counter_layer = text_layer_create(counter_bounds);
    text_layer_set_text(image_counter_layer, image_counter_text);
    text_layer_set_text_alignment(image_counter_layer, GTextAlignmentRight);
    text_layer_set_text_color(image_counter_layer, GColorWhite);
    text_layer_set_background_color(image_counter_layer, GColorClear);
    text_layer_set_font(image_counter_layer, fonts_get_system_font(FONT_KEY_GOTHIC_14_BOLD));
    layer_add_child(window_layer, text_layer_get_layer(image_counter_layer));
    layer_set_hidden(text_layer_get_layer(image_counter_layer), true);
}

// ReSharper disable once CppParameterMayBeConstPtrOrRef
static void window_unload(Window* window)
{
    if (bitmap != NULL)
    {
        gbitmap_destroy(bitmap);
        bitmap = NULL;
    }
    if (bitmap_data != NULL)
    {
        free(bitmap_data);
        bitmap_data = NULL;
    }
    if (scroll_layer != NULL)
    {
        scroll_layer_destroy(scroll_layer);
        scroll_layer = NULL;
    }
    if (bitmap_layer != NULL)
    {
        layer_destroy(bitmap_layer);
        bitmap_layer = NULL;
    }
    if (image_counter_layer != NULL)
    {
        text_layer_destroy(image_counter_layer);
        image_counter_layer = NULL;
    }
    // Cancels a zoom step-down retransmission that is still pending: a timer
    // firing after a BACK would send a stale packet 15
    if (step_down_timer != NULL)
    {
        app_timer_cancel(step_down_timer);
        step_down_timer = NULL;
    }
    // Cancels a pending UP/DOWN double-click timer: a timer firing after a
    // BACK would apply a deferred vertical step to a window that is gone
    if (up_double_timer != NULL)
    {
        app_timer_cancel(up_double_timer);
        up_double_timer = NULL;
    }
    if (down_double_timer != NULL)
    {
        app_timer_cancel(down_double_timer);
        down_double_timer = NULL;
    }
    up_double_pending = false;
    up_double_commit_armed = false;
    down_double_pending = false;
    down_double_commit_armed = false;
    // Cancels the in-flight sequence: packets the phone still sends (e.g.
    // zoom levels being processed after BACK) are discarded
    expecting_response = false;
    sequence_accepted = false;
    bitmap_data_position = 0;

    window_destroy(window);
    drawing_layer = NULL;
}


void window_image_show(const uint8_t* image_data, const size_t length)
{
    const uint8_t flags = image_data[3];
    const bool first_packet = (flags & 0x01) != 0;
    const bool last_packet = (flags & 0x02) != 0;

    const uint16_t total_size = read_uint16_from_byte_array(image_data, 1);
    const bool unavailable = (flags & 0x04) != 0;
    const bool too_large = (flags & 0x08) != 0;
    const uint8_t index = image_data[4];
    const uint8_t count = image_data[5] == 0 ? 1 : image_data[5];
    const uint16_t width = read_uint16_from_byte_array(image_data, 6);
    const uint16_t height = read_uint16_from_byte_array(image_data, 8);
    const uint8_t level = image_data[10];

    if (first_packet)
    {
        sequence_accepted = image_sequence_is_acceptable(flags, image_data[0], index, level);
    }

    if (!sequence_accepted)
    {
        // Stale sequence (first packet or a leftover packet of it): discard
        // everything, no window creation, no buffer
        return;
    }

    notification_id = image_data[0];


    if (first_packet)
    {
        current_image_index = index;
        total_image_count = count;
        image_unavailable = false;

        // The incoming sequence replaces the displayed image: release the
        // previous buffer before measuring the free RAM, so the check sees
        // the RAM the new image will actually have to fit into
        if (bitmap != NULL)
        {
            gbitmap_destroy(bitmap);
            bitmap = NULL;
        }
        if (bitmap_data != NULL)
        {
            free(bitmap_data);
            bitmap_data = NULL;
        }
        bitmap_data_position = 0;
        update_scroll_layer();

        if (drawing_layer == NULL)
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
            }
            )
            ;
            window_set_click_config_provider(window, buttons_config);

            window_stack_push(window, true);
        }

        update_image_counter(count);

        if (unavailable)
        {
            // The phone couldn't load the image (permissions, URI): it still
            // counts in N/M, "Impossible to load" is shown instead
            show_impossible_to_load();
            return;
        }

        if (too_large)
        {
            // PNG too large to be sent at the requested zoom level: same
            // treatment as a failed memory check (step down or error)
            if (step_down_zoom_level())
            {
                sequence_accepted = false;
                return;
            }

            show_impossible_to_load();
            return;
        }

        if (!image_fits_in_free_ram(width, height, total_size))
        {
            // The decoded image (PNG buffer + decoded pixels + margin) doesn't
            // fit in the free RAM: ask the phone for a lower zoom level
            if (step_down_zoom_level())
            {
                sequence_accepted = false;
                return;
            }

            show_impossible_to_load();
            return;
        }

        bitmap_data = malloc(total_size);
        if (bitmap_data == NULL)
        {
            // Backstop: the allocation failed (the check above should have
            // prevented it)
            if (step_down_zoom_level())
            {
                sequence_accepted = false;
                return;
            }

            show_impossible_to_load();
            return;
        }
    }

    const size_t png_data_length = length - 11;
    memcpy(&bitmap_data[bitmap_data_position], &image_data[11], png_data_length);
    bitmap_data_position += png_data_length;

    if (last_packet)
    {
        bitmap = gbitmap_create_from_png_data(bitmap_data, bitmap_data_position);
        free(bitmap_data);
        bitmap_data = NULL;

        if (bitmap == NULL)
        {
            // Backstop: the PNG data failed to decode
            if (step_down_zoom_level())
            {
                // "Loading..." stays visible while the phone processes
                // the lower zoom level
                sequence_accepted = false;
            }
            else
            {
                show_impossible_to_load();
            }
        }
        else
        {
            expecting_response = false;

            // Size the scroll content and center the offset right after the
            // decode, before the first redraw
            update_scroll_layer();
        }

        if (drawing_layer != NULL)
        {
            layer_mark_dirty(drawing_layer);
        }
    }
}
