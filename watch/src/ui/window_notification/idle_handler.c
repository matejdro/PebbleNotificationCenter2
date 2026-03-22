#include "idle_handler.h"

#include "commons/bytes.h"
#include "commons/connection/bucket_sync.h"
#include "connection/packets.h"

bool idle_handler_has_user_interacted_since_app_start = false;
bool idle_handler_has_user_interacted_since_last_vibration = false;

static AppTimer* auto_close_timer = NULL;

static void cancel_timers(void)
{
    if (auto_close_timer != NULL)
    {
        app_timer_cancel(auto_close_timer);
        auto_close_timer = NULL;
    }
}

void idle_handler_register_timers()
{
    cancel_timers();

    if (launch_reason() == APP_LAUNCH_PHONE)
    {
        uint8_t config[3];
        if (bucket_sync_load_bucket(1, config))
        {
            const uint32_t duration = read_uint16_from_byte_array(config, 1) * 1000;

            if (duration != 0)
            {
                auto_close_timer = app_timer_register(duration, send_close_me, NULL);
            }
        }
    }
}


void idle_handler_notify_user_interacted()
{
    idle_handler_has_user_interacted_since_app_start = true;
    idle_handler_has_user_interacted_since_last_vibration = true;

    cancel_timers();
}

void idle_handler_notify_received_new_vibration()
{
    idle_handler_has_user_interacted_since_last_vibration = false;

    idle_handler_register_timers();
}
