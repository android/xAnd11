// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.monksanctum.xand11

import android.app.Notification
import android.app.Notification.Builder
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.IBinder
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.util.Log

import org.monksanctum.xand11.activity.XActivityManager
import org.monksanctum.xand11.atoms.AtomProtocol
import org.monksanctum.xand11.comm.AuthManager
import org.monksanctum.xand11.comm.Event
import org.monksanctum.xand11.comm.Request
import org.monksanctum.xand11.comm.ServerListener
import org.monksanctum.xand11.display.XScreenSaverProtocol
import org.monksanctum.xand11.extension.ExtensionManager
import org.monksanctum.xand11.extension.ExtensionProtocol
import org.monksanctum.xand11.fonts.FontManager
import org.monksanctum.xand11.fonts.FontProtocol
import org.monksanctum.xand11.fonts.FontSpec
import org.monksanctum.xand11.graphics.ColorMaps.ColorMap
import org.monksanctum.xand11.graphics.ColorProtocol
import org.monksanctum.xand11.graphics.DrawingProtocol
import org.monksanctum.xand11.graphics.GraphicsManager
import org.monksanctum.xand11.graphics.GraphicsProtocol
import org.monksanctum.xand11.hosts.HostsManager
import org.monksanctum.xand11.hosts.HostsProtocol
import org.monksanctum.xand11.input.XInputManager
import org.monksanctum.xand11.input.XInputProtocol
import org.monksanctum.xand11.windows.XWindowManager
import org.monksanctum.xand11.windows.XWindowProtocol

class XService : Service() {

    private lateinit var mClientManager: ClientManager
    private lateinit var mListener: ServerListener
    private lateinit var mAuthManager: AuthManager

    private lateinit var mDispatcher: Dispatcher
    private lateinit var mGraphicsManager: GraphicsManager
    lateinit var windowManager: XWindowManager
        private set
    private lateinit var mExtensionManager: ExtensionManager
    private lateinit var mHostsManager: HostsManager
    private lateinit var mInputManager: XInputManager
    private lateinit var mFontManager: FontManager
    private lateinit var mInfo: XServerInfo
    lateinit var activityManager: XActivityManager
        private set

    private val mLocalService = LocalService()

    override fun onCreate() {
        super.onCreate()
        if (DEBUG) Log.d(TAG, "onCreate")
        val builder = Builder(this)
                .setSmallIcon(R.drawable.ic_android_24dp)
                .setContentTitle(getString(R.string.service_name))
                .setContentText(getString(R.string.service_description))
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                    .createNotificationChannel(NotificationChannel("CHANNEL", "X11 " + "Foreground service", NotificationManager.IMPORTANCE_DEFAULT))
            builder.setChannelId("CHANNEL")
        }
        val n = builder.build()
        startForeground(NOTIF_ID, n)
        isRunning = true
        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(STATE_CHANGED))

        if (DEBUG) Log.d(TAG, "Starting up server...")
        Request.populateNames()
        Event.populateNames()
        mInfo = XServerInfo(this)
        initXServices()

        mDispatcher!!.addPacketHandler(XInputProtocol(mInputManager, windowManager))
        mDispatcher!!.addPacketHandler(HostsProtocol(mHostsManager))
        mDispatcher!!.addPacketHandler(GraphicsProtocol(mGraphicsManager, mFontManager))
        mDispatcher!!.addPacketHandler(DrawingProtocol(mGraphicsManager, mFontManager))
        mDispatcher!!.addPacketHandler(ColorProtocol(mGraphicsManager))
        mDispatcher!!.addPacketHandler(XWindowProtocol(windowManager))
        mDispatcher!!.addPacketHandler(ExtensionProtocol(mExtensionManager))
        mDispatcher!!.addPacketHandler(AtomProtocol())
        mDispatcher!!.addPacketHandler(FontProtocol(mFontManager, mGraphicsManager))
        mDispatcher!!.addPacketHandler(XScreenSaverProtocol())

        mAuthManager = AuthManager(mInfo, mHostsManager)
        mClientManager = ClientManager(mAuthManager, mDispatcher)
        val port = Integer.parseInt(getSharedPreferences(
                packageName + "_preferences", 0).getString("display", "6000")!!)
        mListener = ServerListener(port, mClientManager)
        mListener!!.open()
    }

    override fun onDestroy() {
        if (DEBUG) Log.d(TAG, "onDestroy")
        super.onDestroy()
        isRunning = false
        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(STATE_CHANGED))
        mListener!!.close()
    }

    override fun onBind(intent: Intent): IBinder? {
        if (DEBUG) Log.d(TAG, "onBind")
        return mLocalService
    }

    private fun initXServices() {
        // In general trying to init these in order of importance, so they can reference each
        // other if they need to.
        ColorMap.initNames(this)
        FontSpec.initDpi(this)
        activityManager = XActivityManager(this)
        mHostsManager = HostsManager()
        mGraphicsManager = GraphicsManager()
        mInputManager = XInputManager()
        windowManager = XWindowManager(mInfo, mGraphicsManager, activityManager,
                mInputManager)
        mDispatcher = Dispatcher()
        mExtensionManager = ExtensionManager(mDispatcher)
        mFontManager = FontManager()
    }

    private inner class LocalService : Binder() {
        internal val service: XService
            get() = this@XService
    }

    companion object {

        private val TAG = "XService"

        val STATE_CHANGED = "org.monksanctum.xand11.action.STATE_CHANGED"
        private val DEBUG = true

        val COMM_DEBUG = false
        val WINDOW_DEBUG = false
        val GRAPHICS_DEBUG = false
        val FONT_DEBUG = false
        val PROFILE_DEBUG = false
        val DRAWING_DEBUG = false

        val MAJOR_VERSION = 11
        val MINOR_VERSION = 0

        private val NOTIF_ID = 42

        var isRunning: Boolean = false
            private set

        fun getServiceFromBinder(binder: IBinder): XService {
            return (binder as LocalService).service
        }

        fun checkAutoStart(context: Context) {
            Log.d("TestTest", "Check start " + !isRunning)
            if (!isRunning && context.getSharedPreferences(
                            context.packageName + "_preferences", 0).getBoolean("auto_start", false)) {
                context.startService(Intent(context, XService::class.java))
            }
        }
    }
}
