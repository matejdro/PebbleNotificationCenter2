# AppMessage packets

Dictionary entry `0` will always contain the packet ID (uint8)

## Phone -> Watch

### Phone Welcome (packet 1)

Sent to the watch as the response to the packet 1.

* `1`. - phone protocol version (uint16)    
* `2` - Bucketsync sync data (byte array)
  * Sync status (uint8) - `2` - watch is up to date, `1` - this is the last sync packet, `0` - more packet 3 packets will follow this packet.
  * If latest bucketsync version does not match the watch's version:
    * Latest bucketsync version on the phone (uint16)
    * Number of currently active buckets (uint8)
    * For every active bucket:
      * Bucket id (uint8)
      * Bucket flags (uint8)
    * For every bucketsync updated bucket that can fit into this packet:
      * Bucket id (uint8)
      * Bucket size in bytes (uint8)
      * Bucket data (bytes)
* `3` - If this key exists, watch should auto-close after sync is completed (uint8)

(Note: if phone/watch protocol versions do not match, only dictionary entry `1` is sent).

### Re-start bucketsync sync (packet 2)

Sent to the watch when the watchapp is open and buckets on the phone change

* `1` - Bucketsync sync data (byte array)
  * Sync complete flag (uint8) - `1` if this is the last sync packet, `0` if more packet 3 packets will follow.
  * Latest bucketsync version on the phone (uint16)
  * Number of currently active buckets (uint8)
  * For every active bucket:
    * Bucket id (uint8)
    * Bucket flags (uint8)
  * For every bucketsync updated bucket that can fit into this packet:
    * Bucket id (uint8)
    * Bucket size in bytes (uint8)
    * Bucket data (bytes)

### Follow up bucket data (packet 3)

Optionally sent to the watch after packets 1 or 2. Can be repeated until data for all changed buckets has been sent

* `1` - Bucketsync bucket data (byte array)
  * Sync complete flag (uint8) - `1` if this is the last sync packet, `0` if more packet 3 packets will follow.
  * For every bucketsync updated bucket that can fit into this packet:
    * Bucket id (uint8)
    * Bucket size in bytes (uint8)
    * Bucket data (bytes)

### Larger notification text (packet 5)

Sent from the phone after the packet 4

* `1` - Data (byte array)
  * Notification (Bucket) ID to apply that to (uint8)
  * Text (cstring, up to the max size of the packet)

## Watch -> Phone

### Watch Welcome (packet 0)

Sent from the watch when the app is opened.

* `1` - watch protocol version (uint16)
* `2` - current bucketsync watch version (uint16)
* `3` - Appmessage incoming buffer size in bytes (uint16)

### Notification opened notification (packet 4)

Sent from the watch when user opens/views a notification

* `1` - id of the seen bucket (uint8)

# Buckets

Watch can store up to 15 of them, up to 255 bytes each.    
Every bucket is stored in the `2001` - `2015` storage keys.

## Bucket 1

User's preferences
TBD

## Buckets 2-15

* Notification receive timestamp, in unix time (uint32)
* Notification title (string, up to 20 bytes + null terminator)
* Notification subtitle (string, up to 20 bytes + null terminator)
* Notification text (string, up to 249 bytes, depending on how much space was already taken by the title and the subtitle). No null terminator (end of bucket functions as the end of string)

# Non-bucket storage on the watch

160 bytes left over from buckets

`1000` - List of all buckets on the watch (up to 60 bytes)    
* array of tuples (up to 15 items)
* array size determined through `persist_get_size()`
* Tuple: (2 bytes each)
  * Bucket id (uint8)
  * Flags (uint8) - For future use

`1001` - Current version of the data on the watch (uint16)
`1002` - Protocol version of the last data writing on the watch (uint16)
  If this changes, the watch is wiped and re-synced to the phone
