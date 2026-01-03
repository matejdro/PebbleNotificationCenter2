#pragma once

/**
 * Show a status window for when there is no data in the app yet. Window will be replaced with the action list window,
 * when data becomes available.
 */
void window_status_show_empty();

/**
 *
 * Pop all other windows and show an error. This error will be shown indefinitely (until user closes the app).
 */
void window_status_show_error(const char* text);