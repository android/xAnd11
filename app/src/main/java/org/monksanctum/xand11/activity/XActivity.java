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

package org.monksanctum.xand11.activity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;

import org.monksanctum.xand11.Utils;
import org.monksanctum.xand11.XService;
import org.monksanctum.xand11.atoms.AtomManager;
import org.monksanctum.xand11.errors.WindowError;
import org.monksanctum.xand11.windows.PropertyCallback;
import org.monksanctum.xand11.windows.XWindow;
import org.monksanctum.xand11.windows.XWindow.Property;

public class XActivity extends Activity {

    private static final String TAG = "XActivity";

    public static final String EXTRA_WINDOW_ID = "windowId";

    private static final boolean DEBUG_WINDOW_HIERARCHY = true;

    private XService mService;
    private int mWindowId;
    private XWindow mRootWindow;
    private boolean mResumed;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWindowId = getIntent().getIntExtra(EXTRA_WINDOW_ID, -1);
        bindService(new Intent(this, XService.class), mServiceConnection, 0);
    }

    private void onCreateAndConnected() {
        mService.getActivityManager().setTask(mWindowId, getTaskId());
        try {
            mRootWindow = mService.getWindowManager().getWindow(mWindowId);
            mRootWindow.addCallback(mPropertyCallback);
            updateTitle();
            XRootWindowView rootWindowView = new XRootWindowView(this, mRootWindow);
            rootWindowView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT));
            setContentView(rootWindowView);
            if (DEBUG_WINDOW_HIERARCHY) {
                rootWindowView.postDelayed(() -> {
                    debugWindow(mRootWindow, "");
                }, 3000);
            }
            if (mResumed) {
                Utils.sBgHandler.post(() -> {
                    synchronized (mRootWindow) {
                        mRootWindow.notifyEnter();
                    }
                });
            }
        } catch (WindowError windowError) {
            // TODO: UI to handle error?
            throw new RuntimeException(windowError);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mResumed = true;
        if (mRootWindow != null) {
            Utils.sBgHandler.post(() -> {
                synchronized (mRootWindow) {
                    mRootWindow.notifyEnter();
                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mResumed = false;
        if (mRootWindow != null) {
            Utils.sBgHandler.post(() -> {
                synchronized (mRootWindow) {
                    mRootWindow.notifyLeave();
                }
            });
        }
    }

    private void updateTitle() {
        synchronized (mRootWindow) {
            Property prop = mRootWindow.getPropertyLocked(AtomManager.WM_NAME, false);
            if (prop != null) {
                synchronized (prop) {
                    String v = new String(prop.value);
                    setTitle(v);
                }
            }
        }
    }

    @Override
    public boolean onKeyDown(final int keyCode, KeyEvent event) {
        if (mRootWindow == null) return false;
        Utils.sBgHandler.post(() -> {
            synchronized (mRootWindow) {
                mRootWindow.notifyKeyDown(keyCode);
            }
        });
        return true;
    }

    @Override
    public boolean onKeyUp(final int keyCode, KeyEvent event) {
        if (mRootWindow == null) return false;
        Utils.sBgHandler.post(() -> {
            synchronized (mRootWindow) {
                mRootWindow.notifyKeyUp(keyCode);
            }
        });
        return true;
    }

    private void debugWindow(XWindow window, String prefix) {
        Log.d(TAG, prefix + " " + window.getBounds() + " "+ window.getBorderWidth() + " "
                + window.getBorder() + " " + window.getBackground() + " " + window.getInnerBounds()
                + " " + Integer.toHexString(window.getVisibility()));
        synchronized (window) {
            for (int i = 0; i < window.getChildCountLocked(); i++) {
                debugWindow(window.getChildAtLocked(i), prefix + " -- ");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        if (mRootWindow != null) {
            mRootWindow.removeCallback(mPropertyCallback);
        }
    }

    private final PropertyCallback mPropertyCallback = new PropertyCallback() {
        @Override
        protected void onPropertyChanged(int prop) {
            if (prop == AtomManager.WM_NAME) {
                getWindow().getDecorView().post(() -> updateTitle());
            }
        }
    };

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = XService.getServiceFromBinder(service);
            onCreateAndConnected();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // Shouldn't happen, if it does the process is going down, so we don't care.
        }
    };
}
