#include "packets.h"
#include "commons/connection/bluetooth.h"
#include "commons/connection/bucket_sync.h"
#include <pebble.h>

#include "notification_details_fetcher.h"
#include "../ui/window_status.h"
#include "commons/bytes.h"

static void receive_phone_welcome(const DictionaryIterator* iterator);
static void receive_sync_restart(const DictionaryIterator* iterator);
static void receive_sync_next_packet(const DictionaryIterator* iterator);
static void receive_notification_details_text_packet(const DictionaryIterator* iterator);
static void receive_watch_packet(const DictionaryIterator* received);
static void receive_vibrate_packet(const DictionaryIterator* iterator);

void packets_init()
{
    bluetooth_register_reconnect_callback(send_watch_welcome);
    bluetooth_register_receive_watch_packet(receive_watch_packet);
}

void send_watch_welcome()
{
    DictionaryIterator* iterator;
    app_message_outbox_begin(&iterator);
    dict_write_uint8(iterator, 0, 0);
    dict_write_uint16(iterator, 1, PROTOCOL_VERSION);
    dict_write_uint16(iterator, 2, bucket_sync_current_version);
    dict_write_uint16(iterator, 3, appmessage_max_size);
    bluetooth_app_message_outbox_send();
}

bool send_notification_opened(const uint8_t id)
{
    DictionaryIterator* iterator;
    const AppMessageResult res = app_message_outbox_begin(&iterator);

    if (res != APP_MSG_OK)
    {
        return false;
    }

    dict_write_uint8(iterator, 0, 4);
    dict_write_uint8(iterator, 1, id);
    bluetooth_app_message_outbox_send();
    return true;
}

bool send_action_trigger(const uint8_t notification_id, const uint8_t action_index)
{
    DictionaryIterator* iterator;
    const AppMessageResult res = app_message_outbox_begin(&iterator);

    if (res != APP_MSG_OK)
    {
        return false;
    }

    dict_write_uint8(iterator, 0, 6);
    dict_write_uint8(iterator, 1, notification_id);
    dict_write_uint8(iterator, 2, action_index);
    bluetooth_app_message_outbox_send();
    return true;
}

static void receive_watch_packet(const DictionaryIterator* received)
{
    const uint8_t packet_id = dict_find(received, 0)->value->uint8;

    switch (packet_id)
    {
    case 1:
        receive_phone_welcome(received);
        break;
    case 2:
        receive_sync_restart(received);
        break;
    case 3:
        receive_sync_next_packet(received);
        break;
    case 5:
        receive_notification_details_text_packet(received);
        break;
    case 7:
        receive_vibrate_packet(received);
        break;
    default:
        break;
    }
}

static void receive_phone_welcome(const DictionaryIterator* iterator)
{
    if (launch_reason() == APP_LAUNCH_PHONE && dict_find(iterator, 3) != NULL)
    {
        bucket_sync_set_auto_close_after_sync();
    }

    const uint16_t phone_protocol_version = dict_find(iterator, 1)->value->uint16;
    if (phone_protocol_version != PROTOCOL_VERSION)
    {
        if (phone_protocol_version > PROTOCOL_VERSION)
        {
            window_status_show_error("Version mismatch\n\nPlease update watch app");
        }
        else
        {
            window_status_show_error("Version mismatch\n\nPlease update phone app");
        }
        return;
    }

    // ReSharper disable once CppLocalVariableMayBeConst
    Tuple* dict_entry = dict_find(iterator, 2);

    bucket_sync_on_start_received(dict_entry->value->data, dict_entry->length);
}

static void receive_sync_restart(const DictionaryIterator* iterator)
{
    // ReSharper disable once CppLocalVariableMayBeConst
    Tuple* dict_entry = dict_find(iterator, 1);

    bucket_sync_on_start_received(dict_entry->value->data, dict_entry->length);
}

static void receive_sync_next_packet(const DictionaryIterator* iterator)
{
    // ReSharper disable once CppLocalVariableMayBeConst
    Tuple* dict_entry = dict_find(iterator, 1);

    bucket_sync_on_next_packet_received(dict_entry->value->data, dict_entry->length);
}

static void receive_notification_details_text_packet(const DictionaryIterator* iterator)
{
    // ReSharper disable once CppLocalVariableMayBeConst
    Tuple* dict_entry = dict_find(iterator, 1);

    notification_details_fetcher_on_text_received(dict_entry->value->data, dict_entry->length);
}

static void receive_vibrate_packet(const DictionaryIterator* iterator)
{
    const Tuple* dict_entry = dict_find(iterator, 1);

    const size_t size = dict_entry->length;
    const uint8_t* data = dict_entry->value->data;

    static uint32_t segments[100];
    const uint16_t num_segments = size / 2;

    for (int i = 0; i < num_segments; i++)
    {
        segments[i] = read_uint16_from_byte_array(data, i * 2);
    }

    const VibePattern vibe_pattern = {
        .durations = segments,
        .num_segments = num_segments,
    };
    vibes_cancel();
    vibes_enqueue_custom_pattern(vibe_pattern);
}
