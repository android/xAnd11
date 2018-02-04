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

package org.monksanctum.xand11.windows;

import android.util.Log;
import android.util.SparseArray;

import org.monksanctum.xand11.XServerInfo;
import org.monksanctum.xand11.XService;
import org.monksanctum.xand11.activity.XActivityManager;
import org.monksanctum.xand11.errors.WindowError;
import org.monksanctum.xand11.graphics.ColorPaintable;
import org.monksanctum.xand11.graphics.GraphicsManager;
import org.monksanctum.xand11.input.XInputManager;

public class XWindowManager {

    static final boolean DEBUG = XService.WINDOW_DEBUG;
    private static final String TAG = "XWindowManager";
    public static final int ROOT_WINDOW = 3;

    private final SparseArray<XWindow> mWindows = new SparseArray<>();
    private final XWindow mRootWindow;
    private final XServerInfo mInfo;
    private final GraphicsManager mGraphicsManager;
    private final XInputManager mInputManager;

    public XWindowManager(XServerInfo info, GraphicsManager graphicsManager,
            XActivityManager activityManager, XInputManager inputManager) {
        mInfo = info;
        mGraphicsManager = graphicsManager;
        mInputManager = inputManager;
        mRootWindow = createWindow(3, getSize(), getSize(), XWindow.INPUT_OUTPUT);
        mRootWindow.setBorder(new ColorPaintable(0xff000000));
        mRootWindow.setWindowCallback(new RootWindowCallback(activityManager));
    }

    XInputManager getInputManager() {
        return mInputManager;
    }

    GraphicsManager getGraphicsManager() {
        return mGraphicsManager;
    }

    private XWindow createWindow(int id, int width, int height, byte windowClass) {
        XWindow w = new XWindow(id, width, height, windowClass, this);
        synchronized (mWindows) {
            mWindows.put(id, w);
        }
        mGraphicsManager.addDrawable(id, w);
        return w;
    }

    public XWindow createWindow(int windowId, int width, int height, byte windowClass,
            int parentId) throws WindowError {
        XWindow parent = getWindow(parentId);
        if (windowClass == XWindow.COPY_FROM_PARENT) {
            windowClass = parent.getWindowClass();
        }
        XWindow w = createWindow(windowId, width, height, windowClass);
        synchronized (parent) {
            parent.addChildLocked(w);
            parent.notifyChildCreated(w);
        }
        return w;
    }

    private int getSize() {
        return mInfo.getScreens().get(0).getSize();
    }

    public XWindow getWindow(int windowId) throws WindowError {
        //if (DEBUG) Log.d(TAG, "getWindow " + Integer.toHexString(windowId));
        XWindow window = mWindows.get(windowId);
        if (window == null) {
            Log.w(TAG, "Attempt to get unknown window " + windowId);
            throw new WindowError(windowId);
        }
        return window;
    }
}
