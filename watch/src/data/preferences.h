#pragma once
#include <pebble.h>

typedef struct
{
    bool phone_muted;
    bool watch_muted;
    bool no_scroll_wrap;
    uint16_t auto_close_timeout;
} Preferences;

extern Preferences preferences;

void reload_preferences();
