#include <pebble.h>
#include "commons/connection/bluetooth.h"
#include "commons/connection/bucket_sync.h"
#include "connection/packets.h"
#include "ui/window_status.h"

int main(void)
{
    packets_init();
    bluetooth_init();
    bucket_sync_init();

    send_watch_welcome();

    uint8_t tmp[PERSIST_DATA_MAX_LENGTH];
    const bool loaded = bucket_sync_load_bucket(1, tmp);

    if (!loaded || tmp[0] == 0)
    {
        window_status_show_empty();
    }
    else
    {
        // TODO
    }

    app_event_loop();
}