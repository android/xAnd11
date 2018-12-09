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
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import android.view.View
import android.view.ViewGroup
import org.monksanctum.xand11.core.t

import org.monksanctum.xand11.windows.XWindow
import org.monksanctum.xand11.windows.XWindow.WindowCallback

import org.monksanctum.xand11.windows.XWindow.Companion.FLAG_MAPPED

open class XWindowView(context: Context, protected val mWindow: XWindow) : ViewGroup(context), WindowCallback {

    init {
        synchronized(mWindow) {
            for (i in 0 until mWindow.childCountLocked) {
                val child = mWindow.getChildAtLocked(i)
                if (child.visibility and FLAG_MAPPED.toInt() != 0) {
                    addView(XWindowView(context, child))
                }
            }
        }
        mWindow.setWindowCallback(this)
        setWillNotDraw(false)
    }

    override fun addView(child: View) {
        if (child !is XWindowView) {
            throw RuntimeException("Invalid view $child")
        }
        super.addView(child)
    }

    // ----- View stuff ------

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mWindow.setVisibily(true)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mWindow.setVisibily(false)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var width: Int
        var height: Int
        synchronized(mWindow) {
            val bounds = mWindow.bounds
            width = bounds.width()
            height = bounds.height()
            setMeasuredDimension(width, height)
        }

        for (i in 0 until childCount) {
            val child = getChildAt(i) as XWindowView
            synchronized(child.mWindow) {
                child.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.AT_MOST),
                        View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.AT_MOST))
            }
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        for (i in 0 until childCount) {
            val child = getChildAt(i) as XWindowView
            synchronized(child.mWindow) {
                val bounds = child.mWindow.bounds
                child.layout(bounds.left, bounds.top, bounds.right, bounds.bottom)
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        t(String.format("Draw(%x)", mWindow.id)) {
            // TODO: Probably really really don't want to be synchronizing here, figure out if needed
            // or if way around it.
            synchronized(mWindow) {
                val bitmap = mWindow.bitmap
                canvas.drawBitmap(bitmap!!, 0f, 0f, Paint())
            }
        }
    }

    // ----- XServer stuff ------

    override fun onContentChanged() {
        postInvalidate()
    }

    override fun windowOrderChanged() {
        post {
            synchronized(mWindow) {
                var i = 0
                while (i < childCount && i < mWindow.childCountLocked) {
                    while (mWindow.getChildAtLocked(i) !== (getChildAt(i) as XWindowView).mWindow) {
                        bringChildToFront(getChildAt(i))
                    }
                    i++
                }
            }
        }
    }

    override fun onChildAdded(w: XWindow) {}

    override fun onChildRemoved(w: XWindow) {}

    override fun onVisibilityChanged() {
        // Don't care?
    }

    override fun onChildMappingChanged(child: XWindow) {
        post {
            if (child.visibility and FLAG_MAPPED.toInt() != 0) {
                addView(XWindowView(context, child))
            } else {
                for (i in 0..childCount) {
                    if ((getChildAt(i) as XWindowView).mWindow === child) {
                        removeViewAt(i)
                        return@post
                    }
                }
            }
        }
    }

    override fun onLocationChanged() {
        postRequestLayout()
    }

    override fun onSizeChanged() {
        postRequestLayout()
    }

    protected fun postRequestLayout() {
        post { requestLayout() }
    }

    companion object {

        private val TAG = "XWindowView"
    }
}
