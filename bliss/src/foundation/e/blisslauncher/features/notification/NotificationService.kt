/*
 * Copyright © MURENA SAS 2023.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */
package foundation.e.blisslauncher.features.notification

import com.android.launcher3.notification.NotificationListener

class NotificationService : NotificationListener() {

    init {
        sIsConnected = true
        sNotificationServiceInstance = this
    }

    companion object {
        private var sIsConnected = false

        private var sNotificationServiceInstance: NotificationService? = null

        @JvmStatic
        fun getInstanceIfConnected(): NotificationService? {
            return if (sIsConnected) sNotificationServiceInstance else null
        }
    }
}
