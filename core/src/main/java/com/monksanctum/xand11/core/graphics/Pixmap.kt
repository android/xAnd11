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


import org.monksanctum.xand11.core.*
import org.monksanctum.xand11.errors.ValueError

import org.monksanctum.xand11.graphics.GraphicsManager.Companion.DEBUG

class Pixmap @Throws(ValueError::class) constructor(private val mDepth: Byte, width: Int, height: Int, override val id: Int, override val parent: XDrawable) : XDrawable, XPaintable {
    val bitmap: Bitmap
    private val mCanvas: Canvas

    override val x: Int
        get() = 0

    override val y: Int
        get() = 0

    override val borderWidth: Int
        get() = 0

    override val width: Int
        get() = bitmap.getWidth()

    override val height: Int
        get() = bitmap.getHeight()

    override val depth: Int
        get() = mDepth.toInt()

    init {
        if (!isValidConfigType(mDepth.toInt())) {
            throw ValueError(mDepth.toInt())
        }
        if (width == 0 || height == 0) {
            throw ValueError(0)
        }
        bitmap = createBitmap(width, height, mDepth.toInt())
        mCanvas = Canvas(bitmap)
    }

    override fun draw(drawable: XDrawable, bounds: Rect, context: GraphicsContext?) {
        drawable.withCanvas(context) {
            val src = Rect(0, 0, bitmap.getWidth(), bitmap.getHeight())
            if (DEBUG) Platform.logd(TAG, "Drawing pixmap $bounds")
            it.drawBitmap(context, bitmap, src, bounds)
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
        c.drawBitmap(null, this.bitmap, Rect(x, y, width, height), Rect(0, 0, width, height))
    }

    companion object {

        private val TAG = "Pixmap"
    }
}
