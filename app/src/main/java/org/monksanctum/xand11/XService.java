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

package org.monksanctum.xand11;

import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.monksanctum.xand11.activity.XActivityManager;
import org.monksanctum.xand11.atoms.AtomProtocol;
import org.monksanctum.xand11.comm.AuthManager;
import org.monksanctum.xand11.comm.Event;
import org.monksanctum.xand11.comm.Request;
import org.monksanctum.xand11.comm.ServerListener;
import org.monksanctum.xand11.display.XScreenSaverProtocol;
import org.monksanctum.xand11.extension.ExtensionManager;
import org.monksanctum.xand11.extension.ExtensionProtocol;
import org.monksanctum.xand11.fonts.FontManager;
import org.monksanctum.xand11.fonts.FontProtocol;
import org.monksanctum.xand11.fonts.FontSpec;
import org.monksanctum.xand11.graphics.ColorMaps.ColorMap;
import org.monksanctum.xand11.graphics.ColorProtocol;
import org.monksanctum.xand11.graphics.DrawingProtocol;
import org.monksanctum.xand11.graphics.GraphicsManager;
import org.monksanctum.xand11.graphics.GraphicsProtocol;
import org.monksanctum.xand11.hosts.HostsManager;
import org.monksanctum.xand11.hosts.HostsProtocol;
import org.monksanctum.xand11.input.XInputManager;
import org.monksanctum.xand11.input.XInputProtocol;
import org.monksanctum.xand11.windows.XWindowManager;
import org.monksanctum.xand11.windows.XWindowProtocol;

public class XService extends Service {

    private static final String TAG = "XService";

    public static final String STATE_CHANGED = "org.monksanctum.xand11.action.STATE_CHANGED";
    private static final boolean DEBUG = true;

    public static final boolean COMM_DEBUG = false;
    public static final boolean WINDOW_DEBUG = false;
    public static final boolean GRAPHICS_DEBUG = false;
    public static final boolean FONT_DEBUG = false;
    public static final boolean PROFILE_DEBUG = false;
    public static final boolean DRAWING_DEBUG = false;

    public static final int MAJOR_VERSION = 11;
    public static final int MINOR_VERSION = 0;

    private static final int NOTIF_ID = 42;

    private static boolean sRunning;

    private ClientManager mClientManager;
    private ServerListener mListener;
    private AuthManager mAuthManager;

    private Dispatcher mDispatcher;
    private GraphicsManager mGraphicsManager;
    private XWindowManager mWindowManager;
    private ExtensionManager mExtensionManager;
    private HostsManager mHostsManager;
    private XInputManager mInputManager;
    private FontManager mFontManager;
    private XServerInfo mInfo;
    private XActivityManager mActivityManager;

    @Override
    public void onCreate() {
        super.onCreate();
        if (DEBUG) Log.d(TAG, "onCreate");
        Builder builder = new Builder(this)
                .setSmallIcon(R.drawable.ic_android_24dp)
                .setContentTitle(getString(R.string.service_name))
                .setContentText(getString(R.string.service_description));
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                    .createNotificationChannel(new NotificationChannel("CHANNEL", "X11 " +
                            "Foreground service", NotificationManager.IMPORTANCE_DEFAULT));
            builder.setChannelId("CHANNEL");
        }
        Notification n = builder.build();
        startForeground(NOTIF_ID, n);
        sRunning = true;
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(STATE_CHANGED));

        if (DEBUG) Log.d(TAG, "Starting up server...");
        Request.populateNames();
        Event.populateNames();
        mInfo = new XServerInfo(this);
        initXServices();

        mDispatcher.addPacketHandler(new XInputProtocol(mInputManager, mWindowManager));
        mDispatcher.addPacketHandler(new HostsProtocol(mHostsManager));
        mDispatcher.addPacketHandler(new GraphicsProtocol(mGraphicsManager, mFontManager));
        mDispatcher.addPacketHandler(new DrawingProtocol(mGraphicsManager, mFontManager));
        mDispatcher.addPacketHandler(new ColorProtocol(mGraphicsManager));
        mDispatcher.addPacketHandler(new XWindowProtocol(mWindowManager));
        mDispatcher.addPacketHandler(new ExtensionProtocol(mExtensionManager));
        mDispatcher.addPacketHandler(new AtomProtocol());
        mDispatcher.addPacketHandler(new FontProtocol(mFontManager, mGraphicsManager));
        mDispatcher.addPacketHandler(new XScreenSaverProtocol());

        mAuthManager = new AuthManager(mInfo, mHostsManager);
        mClientManager = new ClientManager(mAuthManager, mDispatcher);
        int port = Integer.parseInt(getSharedPreferences(
                getPackageName() + "_preferences", 0).getString("display", "6000"));
        mListener = new ServerListener(port, mClientManager);
        mListener.open();
    }

    @Override
    public void onDestroy() {
        if (DEBUG) Log.d(TAG, "onDestroy");
        super.onDestroy();
        sRunning = false;
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(STATE_CHANGED));
        mListener.close();
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (DEBUG) Log.d(TAG, "onBind");
        return mLocalService;
    }

    private void initXServices() {
        // In general trying to init these in order of importance, so they can reference each
        // other if they need to.
        ColorMap.initNames(this);
        FontSpec.initDpi(this);
        mActivityManager = new XActivityManager(this);
        mHostsManager = new HostsManager();
        mGraphicsManager = new GraphicsManager();
        mInputManager = new XInputManager();
        mWindowManager = new XWindowManager(mInfo, mGraphicsManager, mActivityManager,
                mInputManager);
        mDispatcher = new Dispatcher();
        mExtensionManager = new ExtensionManager(mDispatcher);
        mFontManager = new FontManager();
    }

    public XActivityManager getActivityManager() {
        return mActivityManager;
    }

    public XWindowManager getWindowManager() {
        return mWindowManager;
    }

    public static XService getServiceFromBinder(IBinder binder) {
        return ((LocalService) binder).getService();
    }

    public static boolean isRunning() {
        return sRunning;
    }

    private final LocalService mLocalService = new LocalService();

    public static void checkAutoStart(Context context) {
        Log.d("TestTest", "Check start " + !isRunning());
        if (!isRunning() && context.getSharedPreferences(
                context.getPackageName() + "_preferences", 0).getBoolean("auto_start", false)) {
            context.startService(new Intent(context, XService.class));
        }
    }

    private class LocalService extends Binder {
        private XService getService() {
            return XService.this;
        }
    }
}
