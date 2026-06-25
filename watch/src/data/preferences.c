#include "preferences.h"

#include "commons/bytes.h"
#include "commons/connection/bucket_sync.h"
#include "ui/window_notification/window_notification.h"

Preferences preferences;

void reload_preferences()
{
    uint8_t bucket_data[PERSIST_DATA_MAX_LENGTH];
    const bool bucket_exists = bucket_sync_load_bucket(1, bucket_data);
    if (!bucket_exists)
    {
        return;
    }

    preferences.watch_muted = (bucket_data[0] & 0x01) != 0;
    preferences.phone_muted = (bucket_data[0] & 0x02) != 0;
    preferences.no_scroll_wrap = (bucket_data[0] & 0x04) != 0;
    preferences.enable_backlight_on_vibration = (bucket_data[0] & 0x08) != 0;
    preferences.auto_close_timeout = read_uint16_from_byte_array(bucket_data, 1);
}
