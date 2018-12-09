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

import org.monksanctum.xand11.core.Bitmap
import org.monksanctum.xand11.core.Canvas
import org.monksanctum.xand11.core.GraphicsContext

interface XDrawable {

    val parent: XDrawable?
    val id: Int

    val depth: Int
    val x: Int
    val y: Int
    val borderWidth: Int
    val width: Int
    val height: Int
    fun lockCanvas(gc: GraphicsContext?): Canvas
    fun unlockCanvas()

    fun read(bitmap: Bitmap, x: Int, y: Int, width: Int, height: Int)

}

inline fun XDrawable.withCanvas(context: GraphicsContext? = null, function: (canvas: Canvas) -> Unit) {
    synchronized(this) {
        val canvas = lockCanvas(context)
        function.invoke(canvas)
        unlockCanvas()
    }
}
