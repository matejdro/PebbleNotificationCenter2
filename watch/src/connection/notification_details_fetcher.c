#include "notification_details_fetcher.h"

#include "packets.h"
#include "commons/connection/bluetooth.h"
#include "ui/window_notification/data_loading.h"

static void (*change_callback)() = NULL;
static bool is_fetching = false;

static int16_t next_notification_to_fetch = -1;

void notification_details_fetcher_fetch(const uint8_t bucket_id)
{
    bool success = send_notification_opened(bucket_id);

    if (!success)
    {
        next_notification_to_fetch = bucket_id;
        return;
    }

    is_fetching = true;
    if (change_callback != NULL)
    {
        change_callback();
    }
}

void notification_details_fetcher_on_text_received(const uint8_t* data, const size_t data_size)
{
    is_fetching = false;
    if (change_callback != NULL)
    {
        change_callback();
    }

    const uint8_t bucket_id = data[0];

    window_notification_data_receive_more_text(bucket_id, &data[1], data_size - 1);
}

static void on_sending_finished(const bool success)
{
    if (success && next_notification_to_fetch >= 0)
    {
        uint8_t local_next_notification_to_fetch = next_notification_to_fetch;
        next_notification_to_fetch = -1;
        notification_details_fetcher_fetch(local_next_notification_to_fetch);
    }
}

void notification_details_fetcher_init()
{
    bluetooth_register_sending_finish(on_sending_finished);
}

bool notification_details_fetcher_is_fetching()
{
    return is_fetching;
}

void notification_details_fetcher_register_fetching_status_callback(void (*callback)())
{
    change_callback = callback;
}

void notification_details_fetcher_unregister_fetching_status_callback(void (*callback)())
{
    if (change_callback == callback)
    {
        change_callback = NULL;
    }
}
