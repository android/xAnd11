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

import org.monksanctum.xand11.core.GraphicsContext
import org.monksanctum.xand11.core.Platform.Companion.drawColorPaintable
import org.monksanctum.xand11.core.Platform.Companion.intToHexString
import org.monksanctum.xand11.core.Rect

class ColorPaintable(internal val mColor: Int) : XPaintable {

    override fun draw(drawable: XDrawable, bounds: Rect, context: GraphicsContext?) {
        synchronized(drawable) {
            drawColorPaintable(drawable, bounds, context, this)
        }
    }

    override fun toString(): String {
        return "ColorPaintable(#${intToHexString(mColor)})"
    }

    companion object {

        internal val TAG = "ColorPaintable"
    }
}
