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

import org.monksanctum.xand11.comm.XProtoReader
import org.monksanctum.xand11.comm.XProtoReader.ReadException
import org.monksanctum.xand11.comm.XProtoWriter
import org.monksanctum.xand11.comm.XProtoWriter.WriteException
import org.monksanctum.xand11.comm.XSerializable
import org.monksanctum.xand11.core.IOException
import org.monksanctum.xand11.core.Platform
import org.monksanctum.xand11.core.Throws
import org.monksanctum.xand11.errors.ColorMapError

class ColorMaps {

    private val mMaps = mutableMapOf<Int, ColorMap>()

    init {
        createColormap(4)
    }

    @Throws(ColorMapError::class)
    operator fun get(cmap: Int): ColorMap {
        synchronized(mMaps) {
            return mMaps.get(cmap) ?: throw ColorMapError(cmap)
        }
    }

    fun createColormap(cmap: Int) {
        synchronized(mMaps) {
            mMaps.put(cmap, ColorMap())
        }
    }

    fun freeColormap(cmap: Int) {
        synchronized(mMaps) {
            mMaps.remove(cmap)
        }
    }

    class ColorMap {
        private val temp = Color()

        fun getColor(color: Int): Color {
            return getColor(dupe(color shr 16 and 0xff), dupe(color shr 8 and 0xff),
                    dupe(color shr 0 and 0xff))
        }

        private fun dupe(i: Int): Int {
            return i or (i shl 8)
        }

        fun getColor(r: Int, g: Int, b: Int): Color {
            temp.r = r
            temp.g = g
            temp.b = b
            return temp
        }

        companion object {
            val sColorLookup = HashMap<String, Int>()
            private val TAG = "ColorMap"
        }
    }

    class Color : XSerializable {
        internal var r: Int = 0 // Card16
        internal var g: Int = 0 // Card16
        internal var b: Int = 0 // Card16

        constructor() {}

        constructor(r: Int, g: Int, b: Int) {
            this.r = r
            this.g = g
            this.b = b
        }

        fun color(): Int {
            return Platform.rgb(r shr 8, g shr 8, b shr 8)
        }

        fun copy(): Color {
            return Color(r, g, b)
        }

        @Throws(WriteException::class)
        override fun write(writer: XProtoWriter) {
            writer.writeCard16(r)
            writer.writeCard16(g)
            writer.writeCard16(b)
            writer.writePadding(2)
        }

        @Throws(ReadException::class)
        override fun read(reader: XProtoReader) {
            r = reader.readCard16()
            g = reader.readCard16()
            b = reader.readCard16()
            reader.readPadding(2)
        }

        override fun equals(o: Any?): Boolean {
            try {
                val other = o as Color?
                return other!!.r == r && other.g == g && other.b == b
            } catch (e: ClassCastException) {
                return false
            }

        }
    }
}
