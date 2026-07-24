# We build the debug variant for distribution, so minification is off.
# Rules kept here for a future signed release build.
-keepattributes *Annotation*
-keep class com.buildwclaude.alarm.data.** { *; }
