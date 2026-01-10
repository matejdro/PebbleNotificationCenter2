#pragma once
#include <stdint.h>

#include "ui/layers/dots.h"
#include "ui/layers/status_bar.h"

typedef struct
{
    char* text;
    GFont font;
    GRect bounds;
} TextParameters;

typedef struct
{
    uint8_t currently_selected_bucket;
    uint8_t currently_selected_bucket_index;
    uint8_t bucket_count;

    char title_text[21];
    char subtitle_text[21];
    char body_text[256];
} NotificationWindowData;

extern NotificationWindowData window_notification_data;

void window_notification_show();
void window_notification_ui_redraw_scroller_content();
void window_notification_ui_on_bucket_selected();
void window_notification_ui_scroll_by(int16_t amount);