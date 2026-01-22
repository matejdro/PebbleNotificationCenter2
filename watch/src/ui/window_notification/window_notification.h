#pragma once
#include <stdint.h>

#include "ui/layers/dots.h"
#include "ui/layers/status_bar.h"

#define MAX_BODY_TEXT_SIZE 4000

typedef struct
{
    char* text;
    GFont font;
    GRect bounds;
} TextParameters;

typedef struct
{
    char text[21];
} Action;

typedef struct
{
    bool active;

    uint8_t currently_selected_bucket;
    uint8_t currently_selected_bucket_index;
    uint8_t bucket_count;

    char title_text[21];
    char subtitle_text[21];
    // Include + 40 for the date and the null character
    char body_text[MAX_BODY_TEXT_SIZE + 40];

    time_t receive_time;

    uint8_t num_actions;
    Action actions[20];
    bool menu_displayed;
    bool user_interacted;
} NotificationWindowData;

extern NotificationWindowData window_notification_data;

void window_notification_show();
void window_notification_ui_redraw_scroller_content();
void window_notification_ui_on_bucket_selected();
void window_notification_ui_scroll_by(int16_t amount, bool repeating);