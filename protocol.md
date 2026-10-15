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
* `4` - If this key exists, watch will disable close to last app

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

### Notification details (packet 5)

Sent from the phone after the packet 4

* `1` - Data (byte array)
  * Notification (Bucket) ID to apply that to (uint8)
  * Number of actions (uint8)
  * For every action:
    * Action ID (uint8) 
    * Action text (cstring, up to 20 bytes + null terminator)
  * Number of bytes of the notification icon (uint16) - 0 means no icon
  * Icon data (bytes, encoded indexed png for color watches or grayscale png for black-and-white watches)
  * Text (cstring, up to the max size of the packet)
  
### Vibrate (packet 7)

Sent from the phone after new notificaton, when all data is synced. On reception, watch will vibrate with the provided pattern.

* `1` - Data (byte array)
    * Vibration pattern
      * Number of milliseconds to vibrate (uint16)
      * Number of milliseconds to stay quiet (uint16)
      * Number of milliseconds to vibrate (uint16)
      * Number of milliseconds to stay quiet (uint16)
      * ...


### Show a submenu (packet 9)

Sent from the phone after the packet 4

* `1` - Data (byte array)
  * Notification (Bucket) ID to apply the menu to (uint8)
  * Menu ID (uint8)
  * Number of actions (uint8)
  * For every action:
    * Action text (cstring, up to 20 bytes + null terminator)
    * Should send result with voice - 1 or 0 (uint8)
      * When `1`, upon selecting the item, watchapp will prompt the user for voice text entry and reply with it included in key `4` on packet 6.

### Show an image (packet 11)

When the user requests an image from the phone (the "Show image" action, or a packet 15 to change the zoom level or to view another image of the notification), one or more packets 11 will be sent, containing image data. Each packet carries an 11-byte header, followed by the image data.

* `1` - Data (byte array)
  * Notification ID (uint8)
  * Total size of the image bytes (uint16, size of the PNG data; 0 if the image is unavailable or too large)
  * Flags (uint8)
    * 0x01 - 1 when this is the first packet in the image sequence, 0 otherwise
    * 0x02 - 1 when this is the last packet in the image sequence, 0 otherwise
    * 0x04 - the image source could not be loaded on the phone (e.g. missing permission): no image data is sent, width and height are 0; the watch shows "Impossible to load" and the image still counts in the total
    * 0x08 - the image could be loaded, but the PNG is too large to be sent at the requested zoom level: no image data is sent, width and height are 0; the watch steps down to the next lower zoom level and re-requests the image
    * 0x10 - initial push, the sequence is sent in response to the "Show image" action: the watch always accepts it (the only case in which the image window can open without a preceding packet 15); never set on responses to a packet 15
  * Image index (uint8) - position of this image in the notification's image list (0-based)
  * Image count (uint8) - total number of images in the notification
  * Width (uint16) - width in pixels of the image as rendered at the requested zoom level (0 if unavailable or too large)
  * Height (uint16) - height in pixels of the image as rendered at the requested zoom level (0 if unavailable or too large)
  * Zoom level (uint8) - the zoom level the image is rendered at, as a multiplier of the image's fit dimensions (fit = the image scaled to fully fit the screen, preserving the aspect ratio, with black bands): 0 = fit (1.0x), 1 = 1.25x, 2 = 1.5x, 3 = 1.75x, 4 = 2.0x
  * Image data (bytes, encoded indexed png for color watches or grayscale png for black-and-white watches; absent if the image is unavailable or too large)

When zooming, the watch starts at 2x and checks locally, before allocating memory, that the image fits in the free RAM: the declared dimensions from the header (decoded pixels) plus the total PNG size from the header plus a fixed margin, compared against `heap_bytes_free()`. If the check fails, or if the sphone answers with the too-large flag (0x08), the watch re-sends the same packet 15 with the next lower zoom level (1.75x, 1.5x, 1.25x), down to fit; the amount of free RAM is never transmitted to the sphone. Images the sphone cannot load (e.g. missing permissions) are sent with the unavailable flag (0x04) and still count in the total shown to the user.

A first packet 11 is accepted by the watch only if it has the initial-push flag (0x10, a "Show image" push) or if it is the response to a packet 15 that is still pending on the watch. The sphone is request-driven: when the user closes the image window (BACK), the watch sends no cancel packet - it simply stops expecting a response, and any packet 15 responses the sphone completes late are discarded, so the image window never re-opens "out of nothing" on top of the notification list.

### Request re-init (packet 12)

When sent, watch should re-send the welcome packet (packet 0) to the phone

## Watch -> Phone

### Watch Welcome (packet 0)

Sent from the watch when the app is opened.

* `1` - watch protocol version (uint16)
* `2` - current bucketsync watch version (uint16)
* `3` - Appmessage incoming buffer size in bytes (uint16)
* `4` - Watch info flags
  * 0x01 - 1 when the watch has a color screen, 0 otherwise
* `5` - Width of the watch screen (uint16)
* `6` - Height of the watch screen (uint16)
* `7` - List of bucket ids currently active on the watch (byte array)

### Notification opened notification (packet 4)

Sent from the watch when user opens/views a notification

* `1` - id of the seen bucket (uint8)

### Activate action (packet 6)

Sent from the watch when selects an action from the action menu

* `1` - id of the notification bucket (uint8)
* `2` - ID (main menu) / index (submenu) of the action (uint8)
* `3` - ID of the menu that the action is in (0 = regular actions menu, other menus are sent via packet 9) (uint8)
* `4` - Custom text from voice (optional) (cstring)

### Close me (packet 8)

Sent from the watch when it wants to close. Phone app will open the last app, closing the NC in the process.

### Change a setting (packet 10)

* `1` - ID of the setting (uint8)
  * 0 - Mute watch
  * 1 - Mute phone
* `2` - Value of the setting (uint8)
  * 0 = OFF
  * 1 = ON

### Reload all notifications (packet 14)

Sent from the watch to re-show all hidden notifications

### Re-send image (packet 15)

Sent from the watch to change the zoom level of the image, or to request another image of the notification.

* `1` - id of the seen bucket (uint8)
* `2` - Zoom level to request: 0 = fit (1.0x), 1 = 1.25x, 2 = 1.5x, 3 = 1.75x, 4 = 2.0x (uint8, as a multiplier of the image's fit dimensions; replaces the old "cropped 0/1")
* `3` - Image index (uint8) - position of the image to send (0-based)

# Buckets

Watch can store up to 15 of them, up to 255 bytes each.    
Every bucket is stored in the `2001` - `2015` storage keys.

## Bucket 1

Bucket data:

* Flags (uint8)
  * 0x01 - Watch mute on or off
  * 0x02 - Phone mute on or off
  * 0x04 - When set, scrolling wrap-around is disabled
  * 0x08 - When set, watch will turn on backlight on vibration
  * 0x10 - Large status bar font on or off
* Auto close seconds (uint16)
  * 0 means disabled

## Buckets 2-15

Bucket flags:

* 0x01 - When set, notification was not seen by the user yet
* 0x02 - When set, notification has a pause enabled
* 0x04 - When set and this notification is present, app should do periodic vibration

Bucket data:

* Notification receive timestamp, in unix time (uint32)
* Title font (uint8)
* Subtitle font (uint8)
* Body font (uint8)
* Notification accent color (uint8, Pebble GColor8 ARGB). Alpha 0 means no accent color
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

`3002` - `3015` - On-watch per-notification flags
  * When flag is set to 1, it means user has already seen the notification
