package com.buildwclaude.alarm.alarm

import android.content.Context

/**
 * The bare minimum about the currently-ringing alarm, stored in synchronous
 * SharedPreferences so a sticky service restart (after the process is killed) can resume
 * ringing the right alarm rather than falling silent.
 */
object RingState {
    private const val PREFS = "ring_state"
    private const val KEY_ID = "id"
    private const val KEY_AUTO = "auto_snooze"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun set(context: Context, id: Int, autoSnoozeCount: Int) {
        prefs(context).edit().putInt(KEY_ID, id).putInt(KEY_AUTO, autoSnoozeCount).apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }

    fun id(context: Context): Int = prefs(context).getInt(KEY_ID, -1)
    fun autoSnoozeCount(context: Context): Int = prefs(context).getInt(KEY_AUTO, 0)
}
