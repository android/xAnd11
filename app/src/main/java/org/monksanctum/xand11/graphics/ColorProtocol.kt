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

import org.monksanctum.xand11.Client
import org.monksanctum.xand11.Dispatcher.PacketHandler
import org.monksanctum.xand11.comm.PacketReader
import org.monksanctum.xand11.comm.PacketWriter
import org.monksanctum.xand11.comm.Request
import org.monksanctum.xand11.comm.XProtoWriter.WriteException
import org.monksanctum.xand11.errors.ColorMapError
import org.monksanctum.xand11.errors.XError
import org.monksanctum.xand11.graphics.ColorMaps.Color
import org.monksanctum.xand11.graphics.ColorMaps.ColorMap

import android.graphics.Color.blue
import android.graphics.Color.green
import android.graphics.Color.red

class ColorProtocol(private val mGraphics: GraphicsManager) : PacketHandler {
    private val mColorMaps: ColorMaps

    override val opCodes: ByteArray
        get() = HANDLED_OPS

    init {
        mColorMaps = mGraphics.colorMap
    }

    @Throws(XError::class)
    override fun handleRequest(client: Client, reader: PacketReader, writer: PacketWriter) {
        when (reader.majorOpCode) {
            Request.QUERY_COLORS -> handleQueryColors(reader, writer)
            Request.ALLOC_COLOR -> handleAllocColor(reader, writer)
            Request.LOOKUP_COLOR -> handleLookupColor(reader, writer)
            Request.ALLOC_NAMED_COLOR -> handleAllocNamedColor(reader, writer)
        }
    }

    @Throws(XError::class)
    private fun handleAllocNamedColor(reader: PacketReader, writer: PacketWriter) {
        val map = mColorMaps.get(reader.readCard32())
        val len = reader.readCard16()
        reader.readPadding(2)
        val str = reader.readPaddedString(len)
        val color = ColorMap.sColorLookup[str.toLowerCase()]
                ?: throw object : XError(XError.NAME, "Color not found: $str") {
                    override val extraData: Int
                        get() = 0
                }
        val r = red(color)
        val g = green(color)
        val b = blue(color)
        synchronized(map) {
            val c = map.getColor(r, g, b)

            try {
                //Color color = map.colors.get(color);
                c.write(writer)
                val cInt = c.color()
                writer.writeCard32(cInt)
            } catch (e: WriteException) {
            }

        }
        writer.writeCard16(r shl 8 or r)
        writer.writeCard16(g shl 8 or g)
        writer.writeCard16(b shl 8 or b)
        writer.writeCard16(r shl 8 or r)
        writer.writeCard16(g shl 8 or g)
        writer.writeCard16(b shl 8 or b)
        writer.writePadding(8)
    }

    @Throws(XError::class)
    private fun handleLookupColor(reader: PacketReader, writer: PacketWriter) {
        val map = mColorMaps.get(reader.readCard32())
        val len = reader.readCard16()
        reader.readPadding(2)
        val str = reader.readPaddedString(len)
        val color = ColorMap.sColorLookup[str.toLowerCase()]
                ?: throw object : XError(XError.NAME, "Color not found: $str") {
                    override val extraData: Int
                        get() = 0
                }

        val r = red(color)
        val g = green(color)
        val b = blue(color)
        writer.writeCard16(r shl 8 or r)
        writer.writeCard16(g shl 8 or g)
        writer.writeCard16(b shl 8 or b)
        writer.writeCard16(r shl 8 or r)
        writer.writeCard16(g shl 8 or g)
        writer.writeCard16(b shl 8 or b)
        writer.writePadding(12)
    }

    @Throws(XError::class)
    private fun handleQueryColors(reader: PacketReader, writer: PacketWriter) {
        val map = mColorMaps.get(reader.readCard32())
        val n = reader.length / 4 - 1
        writer.writeCard16(n)
        writer.writePadding(22)
        synchronized(map) {
            for (i in 0 until n) {
                val index = reader.readCard32()
                val color = map.getColor(index)
                try {
                    color.write(writer)
                } catch (e: WriteException) {
                }

            }
        }
    }

    @Throws(ColorMapError::class)
    private fun handleAllocColor(reader: PacketReader, writer: PacketWriter) {
        val map = mColorMaps.get(reader.readCard32())
        val r = reader.readCard16()
        val g = reader.readCard16()
        val b = reader.readCard16()
        reader.readPadding(2)

        synchronized(map) {
            val color = map.getColor(r, g, b)

            try {
                //Color color = map.colors.get(color);
                color.write(writer)
                val c = color.color()
                writer.writeCard32(c)
            } catch (e: WriteException) {
            }

        }
        //writer.writeCard32(index);
        writer.writePadding(12)
    }

    companion object {

        private val HANDLED_OPS = byteArrayOf(Request.QUERY_COLORS, Request.ALLOC_COLOR, Request.LOOKUP_COLOR, Request.ALLOC_NAMED_COLOR)
    }
}
