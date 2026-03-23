package com.messaging.messagingplatform

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Wires Hilt and dependency injections through each component.
 */
@HiltAndroidApp
class MessagingApp : Application() {
}