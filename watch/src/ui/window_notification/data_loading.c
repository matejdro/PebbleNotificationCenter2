#include "data_loading.h"

#include "action_list.h"
#include "window_notification.h"
#include "commons/bytes.h"
#include "commons/math.h"
#include "commons/connection/bucket_sync.h"
#include "connection/notification_details_fetcher.h"
#include "ui/window_status.h"

static BucketList* buckets;

static void apply_date_to_body()
{
    const time_t current_unix_time = time(NULL);
    // gmtime only has one static variable. We must make a copy in order to process two different date objects
    const tm current_time = *gmtime(&current_unix_time);
    const tm* receive_time = gmtime(&window_notification_data.receive_time);

    char* format_string;
    if (current_time.tm_yday == receive_time->tm_yday && current_time.tm_year == receive_time->tm_year)
    {
        if (clock_is_24h_style())
            format_string = "Received at %H:%M";
        else
            format_string = "Received at %I:%M %p";
    }
    else if (current_time.tm_year == receive_time->tm_year && receive_time->tm_yday == current_time.tm_yday - 1)
    {
        if (clock_is_24h_style())
            format_string = "Received yesterday, at %H:%M";
        else
            format_string = "Received yesterday, at %I:%M %p";
    }
    else
    {
        if (clock_is_24h_style())
            format_string = "Received on %b %d, %H:%M";
        else
            format_string = "Received on %b %d, %I:%M %p";
    }

    size_t position = strlen(window_notification_data.body_text);

    // Insert two newlines
    window_notification_data.body_text[position++] = '\n';
    window_notification_data.body_text[position++] = '\n';

    strftime(&window_notification_data.body_text[position], 39, format_string, receive_time);
}

static void reload_data_for_current_bucket()
{
    uint8_t bucket_data[256];

    if (!bucket_sync_load_bucket(window_notification_data.currently_selected_bucket, bucket_data))
    {
        // Bucket is not on the device yet. Show blank for now and wait for the buckets to load.
        strcpy(window_notification_data.title_text, "");
        strcpy(window_notification_data.subtitle_text, "");
        strcpy(window_notification_data.body_text, "");
    }
    else
    {
        const uint8_t size = bucket_sync_get_bucket_size(window_notification_data.currently_selected_bucket);

        window_notification_data.receive_time = read_uint32_from_byte_array(bucket_data, 0);

        uint8_t position = 4;
        strcpy(window_notification_data.title_text, (char*)&bucket_data[position]);
        position += strlen(window_notification_data.title_text) + 1;
        strcpy(window_notification_data.subtitle_text, (char*)&bucket_data[position]);
        position += strlen(window_notification_data.subtitle_text) + 1;
        const uint8_t body_bytes = size - position;
        strncpy(window_notification_data.body_text, (char*)&bucket_data[position], body_bytes);
        window_notification_data.body_text[body_bytes] = '\0';

        notification_details_fetcher_fetch(window_notification_data.currently_selected_bucket);
    }

    apply_date_to_body();
    window_notification_ui_redraw_scroller_content();
}

void window_notification_data_select_bucket_on_index(const uint8_t target_index)
{
    uint8_t index_without_settings = 0;

    for (int i = 0; i < buckets->count; i++)
    {
        const uint8_t id = buckets->data[i].id;

        if (id != 1)
        {
            if (index_without_settings == target_index)
            {
                window_notification_data.currently_selected_bucket = id;
                window_notification_data.currently_selected_bucket_index = target_index;
                window_notification_data.num_actions = 0;

                reload_data_for_current_bucket();
                window_notification_ui_on_bucket_selected();
                window_notification_action_list_hide();
                return;
            }

            index_without_settings++;
        }
    }

    // If target_index was out of bounds, just select current bucket
    window_notification_data_select_bucket_on_index(window_notification_data.bucket_count - 1);
}

void notification_window_ingest_bucket_metadata()
{
    uint8_t count_without_settings = 0;
    int16_t current_bucket_index = -1;
    for (int i = 0; i < buckets->count; i++)
    {
        const uint8_t id = buckets->data[i].id;

        if (id != 1)
        {
            count_without_settings++;
        }

        if (id == window_notification_data.currently_selected_bucket)
        {
            current_bucket_index = i;
        }
    }

    if (count_without_settings == 0)
    {
        window_stack_pop(true);
        window_status_show_empty();
        return;
    }

    window_notification_data.bucket_count = count_without_settings;

    if (current_bucket_index != -1)
    {
        window_notification_data_select_bucket_on_index(current_bucket_index);
    }
    else
    {
        window_notification_data_select_bucket_on_index(window_notification_data.currently_selected_bucket_index);
    }
}

static void on_buckets_changed()
{
    buckets = bucket_sync_get_bucket_list();
    notification_window_ingest_bucket_metadata();
}

static void on_bucket_updated(const BucketMetadata bucket_metadata, void* context)
{
    if (bucket_metadata.id == window_notification_data.currently_selected_bucket)
    {
        reload_data_for_current_bucket();
    }
}

void window_notification_data_receive_more_text(const uint8_t bucket_id, const uint8_t* data, const size_t data_size)
{
    if (window_notification_data.active == false || bucket_id != window_notification_data.currently_selected_bucket)
    {
        return;
    }

    size_t position = 1;
    const uint8_t num_actions = data[0];
    window_notification_data.num_actions = num_actions;
    for (int i = 0; i < num_actions; i++)
    {
        const char* action_title = strcpy(window_notification_data.actions[i].text, (char*)&data[position]);
        position += strlen(action_title) + 1;
    }

    const size_t max_text_size = MIN(MAX_BODY_TEXT_SIZE, data_size - position);
    strncpy(window_notification_data.body_text, (char*)&data[position], max_text_size);
    window_notification_data.body_text[max_text_size] = '\0';

    apply_date_to_body();
    window_notification_ui_redraw_scroller_content();
}


void window_notification_data_init()
{
    on_buckets_changed();
    bucket_sync_set_bucket_list_change_callback(on_buckets_changed);
    bucket_sync_set_bucket_data_change_callback(on_bucket_updated, NULL);
}

void window_notification_data_deinit()
{
    bucket_sync_set_bucket_list_change_callback(NULL);
    bucket_sync_clear_bucket_data_change_callback(on_bucket_updated, NULL);
}
