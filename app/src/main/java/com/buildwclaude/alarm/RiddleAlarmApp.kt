package com.buildwclaude.alarm

import android.app.Application

/**
 * Application subclass. Kept intentionally light for now; later steps will use it to
 * create notification channels and initialise the database/scheduler on process start.
 */
class RiddleAlarmApp : Application()
