#include "buttons.h"

#include "data_loading.h"

const int32_t SINGLE_SCROLL_HEIGHT = 32;

static void button_up_single(ClickRecognizerRef recognizer, void* context)
{
    window_notification_ui_scroll_by(SINGLE_SCROLL_HEIGHT);
}

static void button_down_single(ClickRecognizerRef recognizer, void* context)
{
    window_notification_ui_scroll_by(-SINGLE_SCROLL_HEIGHT);
}

static void button_up_repeating(const ClickRecognizerRef recognizer, void* context)
{
    if (click_recognizer_is_repeating(recognizer))
    {
        button_up_single(recognizer, context);
    }
}

static void button_down_repeating(const ClickRecognizerRef recognizer, void* context)
{
    if (click_recognizer_is_repeating(recognizer))
    {
        button_down_single(recognizer, context);
    }
}

static void button_up_multi(const ClickRecognizerRef recognizer, void* context)
{
    if (click_number_of_clicks_counted(recognizer) != 2)
    {
        button_up_single(recognizer, context);
        return;
    }

    if (window_notification_data.currently_selected_bucket_index > 0)
    {
        window_notification_data_select_bucket_on_index(window_notification_data.currently_selected_bucket_index - 1);
    }
    else
    {
        window_notification_data_select_bucket_on_index(window_notification_data.bucket_count - 1);
    }
}

static void button_down_multi(const ClickRecognizerRef recognizer, void* context)
{
    if (click_number_of_clicks_counted(recognizer) != 2)
    {
        button_down_single(recognizer, context);
        return;
    }

    if (window_notification_data.currently_selected_bucket_index < window_notification_data.bucket_count - 1)
    {
        window_notification_data_select_bucket_on_index(window_notification_data.currently_selected_bucket_index + 1);
    }
    else
    {
        window_notification_data_select_bucket_on_index(0);
    }
}

void window_notification_buttons_config()
{
    window_multi_click_subscribe(BUTTON_ID_UP, 1, 2, 150, false, button_up_multi);
    window_multi_click_subscribe(BUTTON_ID_DOWN, 1, 2, 150, false, button_down_multi);

    window_single_repeating_click_subscribe(BUTTON_ID_UP, 100, button_up_repeating);
    window_single_repeating_click_subscribe(BUTTON_ID_DOWN, 100, button_down_repeating);
}