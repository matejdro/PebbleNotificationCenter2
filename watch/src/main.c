#include <pebble.h>
#include "commons/connection/bluetooth.h"
#include "commons/connection/bucket_sync.h"
#include "connection/packets.h"
#include "ui/window_status.h"
#include "ui/window_notification/window_notification.h"
#include "utils/bucket_utils.h"

int main(void)
{
    packets_init();
    bluetooth_init();
    bucket_sync_init();

    send_watch_welcome();

    const BucketList* buckets = bucket_sync_get_bucket_list();

    if (is_any_notification_bucket_active(buckets))
    {
        window_notification_show();
    }
    else
    {
        window_status_show_empty();
    }

    app_event_loop();
}