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

package org.monksanctum.xand11.activity

import android.content.Context
import android.graphics.Rect
import android.view.KeyEvent
import android.view.View
import org.monksanctum.xand11.core.Utils

import org.monksanctum.xand11.windows.XWindow

class XRootWindowView(context: Context, window: XWindow) : XWindowView(context, window) {

    init {
        setBackgroundColor(-0x1)
    }

    override fun onLocationChanged() {
        super.onLocationChanged()
    }

    override fun onSizeChanged() {
        super.onSizeChanged()
    }

    override fun onVisibilityChanged() {
        super.onVisibilityChanged()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        Utils.sBgHandler.post {
            synchronized(mWindow) {
                mWindow.notifyKeyDown(keyCode)
            }
        }
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        Utils.sBgHandler.post {
            synchronized(mWindow) {
                mWindow.notifyKeyUp(keyCode)
            }
        }
        return true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var width = View.MeasureSpec.getSize(widthMeasureSpec)
        var height = View.MeasureSpec.getSize(heightMeasureSpec)
        if (RESIZE_TO_VIEW) {
            val bounds = mWindow.bounds
            val border = mWindow.borderWidth
            if (bounds.right != width || bounds.bottom != height || bounds.left != 0 || bounds
                            .right != 0) {
                if (mWindow.setBounds(Rect(0, 0, width, height))) {
                    Utils.sBgHandler.post { mWindow.notifyConfigureWindow() }
                }
            }
        } else {
            width = mWindow.bounds.width()
            height = mWindow.bounds.height()
        }
        setMeasuredDimension(width, height)
    }

    companion object {

        private val RESIZE_TO_VIEW = false
    }
}
