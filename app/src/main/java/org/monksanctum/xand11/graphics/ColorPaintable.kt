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

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import org.monksanctum.xand11.graphics.GraphicsManager.Companion.DEBUG

class ColorPaintable(private val mColor: Int) : XPaintable {

    override fun draw(drawable: XDrawable, bounds: Rect, context: GraphicsContext?) {
        synchronized(drawable) {
            val canvas = drawable.lockCanvas(context!!)
            val p = if (context != null) Paint(context.paint) else Paint()
            p.color = mColor
            p.style = Paint.Style.FILL
            if (DEBUG) Log.d(TAG, "Drawing 0x" + Integer.toHexString(mColor) + " on " + bounds)
            canvas.drawRect(bounds, p)
            drawable.unlockCanvas()
        }
    }

    override fun toString(): String {
        return String.format("ColorPaintable(#%x)", mColor)
    }

    companion object {

        private val TAG = "ColorPaintable"
    }
}
