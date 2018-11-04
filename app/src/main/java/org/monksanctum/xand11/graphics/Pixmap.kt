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

package org.monksanctum.xand11.graphics

import android.graphics.Bitmap
import android.graphics.Bitmap.Config
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import android.util.SparseArray

import org.monksanctum.xand11.errors.ValueError

import org.monksanctum.xand11.graphics.GraphicsManager.Companion.DEBUG

class Pixmap @Throws(ValueError::class)
constructor(private val mDepth: Byte, width: Int, height: Int, override val id: Int, override val parent: XDrawable) : XDrawable, XPaintable {
    val bitmap: Bitmap
    private val mCanvas: Canvas

    override val x: Int
        get() = 0

    override val y: Int
        get() = 0

    override val borderWidth: Int
        get() = 0

    override val width: Int
        get() = bitmap.width

    override val height: Int
        get() = bitmap.height

    override val depth: Int
        get() = mDepth.toInt()

    init {
        if (mConfigs.indexOfKey(mDepth.toInt()) < 0) {
            throw ValueError(mDepth.toInt())
        }
        if (width == 0 || height == 0) {
            throw ValueError(0)
        }
        bitmap = Bitmap.createBitmap(width, height, mConfigs.get(mDepth.toInt()))
        mCanvas = Canvas(bitmap)
    }

    override fun draw(drawable: XDrawable, bounds: Rect, context: GraphicsContext?) {
        synchronized(drawable) {
            val c = drawable.lockCanvas(context)
            val paint = if (context != null) context.paint else Paint()
            val src = Rect(0, 0, bitmap.width, bitmap.height)
            if (DEBUG) Log.d(TAG, "Drawing pixmap $bounds")
            c.drawBitmap(bitmap, src, bounds, paint)
            drawable.unlockCanvas()
        }
    }

    override fun lockCanvas(gc: GraphicsContext?): Canvas {
        return mCanvas
    }

    override fun unlockCanvas() {
        // Don't care?
    }

    override fun read(bitmap: Bitmap, x: Int, y: Int, width: Int, height: Int) {
        val c = Canvas(bitmap)
        c.drawBitmap(this.bitmap, Rect(x, y, width, height), Rect(0, 0, width, height),
                Paint())
    }

    companion object {

        private val TAG = "Pixmap"
        val mConfigs = SparseArray<Config>()

        init {
            // These all make sense.
            mConfigs.put(12, Config.ARGB_4444)
            mConfigs.put(24, Config.ARGB_8888)
            mConfigs.put(32, Config.ARGB_8888)
            mConfigs.put(16, Config.RGB_565)
            // These need better handling somehow.
            mConfigs.put(1, Config.ALPHA_8)
            mConfigs.put(4, Config.ALPHA_8)
            mConfigs.put(8, Config.ALPHA_8)
        }
    }
}
