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


import org.monksanctum.xand11.core.GRAPHICS_DEBUG
import org.monksanctum.xand11.core.GraphicsContext
import org.monksanctum.xand11.core.Throws
import org.monksanctum.xand11.errors.DrawableError
import org.monksanctum.xand11.errors.GContextError
import org.monksanctum.xand11.errors.PixmapError
import org.monksanctum.xand11.errors.ValueError

class GraphicsManager {

    private val mGcs = mutableMapOf<Int, GraphicsContext>()
    private val mPixmaps = mutableMapOf<Int, Pixmap>()
    private val mDrawableLookup = mutableMapOf<Int, XDrawable>()

    val colorMap = ColorMaps()

    fun addDrawable(id: Int, drawable: XDrawable) {
        synchronized(mDrawableLookup) {
            mDrawableLookup.put(id, drawable)
        }
    }

    fun removeDrawable(id: Int) {
        synchronized(mDrawableLookup) {
            mDrawableLookup.remove(id)
        }
    }

    @Throws(DrawableError::class)
    fun getDrawable(id: Int): XDrawable {
        synchronized(mDrawableLookup) {
            return mDrawableLookup.get(id) ?: throw DrawableError(id)
        }
    }

    fun createGc(id: Int): GraphicsContext {
        val graphicsContext = GraphicsContext(id)
        synchronized(mGcs) {
            mGcs.put(id, graphicsContext)
        }
        return graphicsContext
    }

    @Throws(GContextError::class)
    fun getGc(id: Int): GraphicsContext {
        synchronized(mGcs) {
            return mGcs.get(id) ?: throw GContextError(id)
        }
    }

    fun freeGc(id: Int) {
        synchronized(mGcs) {
            mGcs.remove(id)
        }
    }

    @Throws(ValueError::class)
    fun createPixmap(id: Int, depth: Byte, width: Int, height: Int, drawable: XDrawable): Pixmap {
        val pixmap = Pixmap(depth, width, height, id, drawable)
        synchronized(mPixmaps) {
            mPixmaps.put(id, pixmap)
        }
        addDrawable(id, pixmap)
        return pixmap
    }

    fun freePixmap(id: Int) {
        synchronized(mPixmaps) {
            mPixmaps.remove(id)
        }
        removeDrawable(id)
    }

    @Throws(PixmapError::class)
    fun getPixmap(pixmap: Int): Pixmap {
        synchronized(mPixmaps) {
            return mPixmaps.get(pixmap) ?: throw PixmapError(pixmap)
        }
    }

    companion object {

        val DEBUG = GRAPHICS_DEBUG
    }
}
