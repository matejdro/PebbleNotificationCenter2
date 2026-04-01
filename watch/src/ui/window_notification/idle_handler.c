#include "idle_handler.h"

#include "data_loading.h"
#include "commons/bytes.h"
#include "commons/connection/bucket_sync.h"
#include "connection/packets.h"

const uint32_t PERIODIC_VIBRATION_PERIOD_MS = 10000;
static const uint32_t PERIODIC_VIBRATION_SEGMENTS[] = {50};
const VibePattern PERIODIC_VIBRATION_PATTERN = {
    .durations = PERIODIC_VIBRATION_SEGMENTS,
    .num_segments = ARRAY_LENGTH(PERIODIC_VIBRATION_SEGMENTS),
};

bool idle_handler_has_user_interacted_since_app_start = false;
bool idle_handler_has_user_interacted_since_last_vibration = false;
static bool any_notification_vibrated = false;

static AppTimer* auto_close_timer = NULL;
static AppTimer* periodic_vibration_timer = NULL;

static void cancel_timers(void)
{
    if (auto_close_timer != NULL)
    {
        app_timer_cancel(auto_close_timer);
        auto_close_timer = NULL;
    }

    if (periodic_vibration_timer != NULL)
    {
        app_timer_cancel(periodic_vibration_timer);
        periodic_vibration_timer = NULL;
    }
}

static bool any_notification_wants_periodic_vibration(void)
{
    const BucketList* bucket_list = bucket_sync_get_bucket_list();
    for (int i = 0; i < bucket_list->count; i++)
    {
        const BucketMetadata bucket_metadata = bucket_list->data[i];
        const bool notification_has_periodic_vibration = (bucket_metadata.flags) & 0x04;

        if (notification_has_periodic_vibration && is_notification_unread(bucket_metadata.flags, bucket_metadata.id))
        {
            return true;
        }
    }

    return false;
}

static void maybe_start_periodic_vibration_timer();

static void handle_periodic_vibration()
{
    vibes_enqueue_custom_pattern(PERIODIC_VIBRATION_PATTERN);
    maybe_start_periodic_vibration_timer();
}

void idle_handler_register_timers()
{
    cancel_timers();

    if (!idle_handler_has_user_interacted_since_last_vibration)
    {
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

        maybe_start_periodic_vibration_timer();
    }
}

static void maybe_start_periodic_vibration_timer()
{
    if (any_notification_vibrated &&
        !idle_handler_has_user_interacted_since_last_vibration &&
        any_notification_wants_periodic_vibration()
    )
    {
        periodic_vibration_timer = app_timer_register(PERIODIC_VIBRATION_PERIOD_MS, handle_periodic_vibration, NULL);
    }
    else
    {
        periodic_vibration_timer = NULL;
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
    any_notification_vibrated = true;

    idle_handler_register_timers();
}

void idle_handler_notify_notifications_updated()
{
    if (!any_notification_wants_periodic_vibration())
    {
        if (periodic_vibration_timer != NULL)
        {
            app_timer_cancel(periodic_vibration_timer);
            periodic_vibration_timer = NULL;
        }
    }
}