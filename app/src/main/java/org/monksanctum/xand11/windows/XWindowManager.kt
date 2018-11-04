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

package org.monksanctum.xand11.windows

import android.util.Log
import android.util.SparseArray

import org.monksanctum.xand11.XServerInfo
import org.monksanctum.xand11.XService
import org.monksanctum.xand11.activity.XActivityManager
import org.monksanctum.xand11.errors.WindowError
import org.monksanctum.xand11.graphics.ColorPaintable
import org.monksanctum.xand11.graphics.GraphicsManager
import org.monksanctum.xand11.input.XInputManager

class XWindowManager(private val mInfo: XServerInfo, internal val graphicsManager: GraphicsManager,
                     activityManager: XActivityManager, internal val inputManager: XInputManager) {

    private val mWindows = SparseArray<XWindow>()
    private val mRootWindow: XWindow

    private val size: Int
        get() = mInfo.screens[0].size

    init {
        mRootWindow = createWindow(3, size, size, XWindow.INPUT_OUTPUT)
        mRootWindow.border = ColorPaintable(-0x1000000)
        mRootWindow.setWindowCallback(RootWindowCallback(activityManager))
    }

    private fun createWindow(id: Int, width: Int, height: Int, windowClass: Byte): XWindow {
        val w = XWindow(id, width, height, windowClass, this)
        synchronized(mWindows) {
            mWindows.put(id, w)
        }
        graphicsManager.addDrawable(id, w)
        return w
    }

    @Throws(WindowError::class)
    fun createWindow(windowId: Int, width: Int, height: Int, windowClass: Byte,
                     parentId: Int): XWindow {
        var windowClass = windowClass
        val parent = getWindow(parentId)
        if (windowClass == XWindow.COPY_FROM_PARENT) {
            windowClass = parent.windowClass
        }
        val w = createWindow(windowId, width, height, windowClass)
        synchronized(parent) {
            parent.addChildLocked(w)
            parent.notifyChildCreated(w)
        }
        return w
    }

    @Throws(WindowError::class)
    fun getWindow(windowId: Int): XWindow {
        //if (DEBUG) Log.d(TAG, "getWindow " + Integer.toHexString(windowId));
        val window = mWindows.get(windowId)
        if (window == null) {
            Log.w(TAG, "Attempt to get unknown window $windowId")
            throw WindowError(windowId)
        }
        return window
    }

    companion object {

        internal val DEBUG = XService.WINDOW_DEBUG
        private val TAG = "XWindowManager"
        val ROOT_WINDOW = 3
    }
}
