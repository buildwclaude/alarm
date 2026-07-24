package com.buildwclaude.alarm.alarm

import android.net.Uri
import com.buildwclaude.alarm.R

/**
 * The catalogue of bundled alarm tones.
 *
 * To add another tone: drop `my_tone.ogg` into `res/raw/` and add ONE line to [SOUNDS],
 * e.g. `"chimes" to R.raw.my_tone`. Everything else (editor, service) picks it up.
 */
object AlarmSound {

    // key -> raw resource id
    val SOUNDS: Map<String, Int> = mapOf(
        "default" to R.raw.alarm_default,
    )

    val DEFAULT_KEY = "default"

    fun resIdFor(key: String): Int = SOUNDS[key] ?: R.raw.alarm_default

    fun uriFor(packageName: String, key: String): Uri =
        Uri.parse("android.resource://$packageName/${resIdFor(key)}")
}
