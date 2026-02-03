#include "buttons.h"

#include "action_list.h"
#include "data_loading.h"
#include "connection/packets.h"

const int32_t SINGLE_SCROLL_HEIGHT = 32;

// ReSharper disable once CppParameterMayBeConst
static void button_up_single(ClickRecognizerRef recognizer, void* context)
{
    window_notification_data.user_interacted = true;

    if (window_notification_data.menu_displayed)
    {
        window_notification_action_list_move_up();
    }
    else
    {
        window_notification_ui_scroll_by(SINGLE_SCROLL_HEIGHT, click_recognizer_is_repeating(recognizer));
    }
}

// ReSharper disable once CppParameterMayBeConst
static void button_down_single(ClickRecognizerRef recognizer, void* context)
{
    window_notification_data.user_interacted = true;

    if (window_notification_data.menu_displayed)
    {
        window_notification_action_list_move_down();
    }
    else
    {
        window_notification_ui_scroll_by(-SINGLE_SCROLL_HEIGHT, click_recognizer_is_repeating(recognizer));
    }
}

static void button_select_single(ClickRecognizerRef recognizer, void* context)
{
    window_notification_data.user_interacted = true;

    if (window_notification_data.num_actions == 0)
    {
        vibes_double_pulse();
        return;
    }

    if (window_notification_data.menu_displayed)
    {
        window_notification_action_select();
    }
    else
    {
        window_notification_action_list_show();
    }
}

static void button_back_single(ClickRecognizerRef recognizer, void* context)
{
    window_notification_data.user_interacted = true;

    if (window_notification_data.menu_displayed)
    {
        window_notification_action_list_hide();
    }
    else
    {
        send_close_me();
    }
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
    if (window_notification_data.menu_displayed)
    {
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
    if (window_notification_data.menu_displayed)
    {
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
    window_multi_click_subscribe(BUTTON_ID_UP, 2, 2, 150, false, button_up_multi);
    window_multi_click_subscribe(BUTTON_ID_DOWN, 2, 2, 150, false, button_down_multi);

    window_single_repeating_click_subscribe(BUTTON_ID_UP, 100, button_up_repeating);
    window_single_repeating_click_subscribe(BUTTON_ID_DOWN, 100, button_down_repeating);

    // regular button single click action is delayed since the watch is waiting to determine whether we hold the button
    // or not. This is not necessary in our case, so we can just use "button down" event instead
    window_raw_click_subscribe(BUTTON_ID_UP, button_up_single, NULL, NULL);
    window_raw_click_subscribe(BUTTON_ID_DOWN, button_down_single, NULL, NULL);

    window_single_click_subscribe(BUTTON_ID_SELECT, button_select_single);
    window_single_click_subscribe(BUTTON_ID_BACK, button_back_single);
}