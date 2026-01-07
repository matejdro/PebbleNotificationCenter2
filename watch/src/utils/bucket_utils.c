#include "bucket_utils.h"

bool is_any_notification_bucket_active(const BucketList* bucket_list)
{
    // Bucket 1 is for settings. So if any non-1 bucket is loaded, we have notifications

    for (int i = 0; i < bucket_list->count; ++i)
    {
        if (bucket_list[i].data->id != 1)
        {
            return true;
        }
    }

    return false;
}
