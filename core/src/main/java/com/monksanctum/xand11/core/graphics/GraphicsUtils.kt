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

import org.monksanctum.xand11.comm.PacketReader
import org.monksanctum.xand11.comm.PacketWriter
import org.monksanctum.xand11.core.Bitmap
import org.monksanctum.xand11.core.GraphicsContext
import org.monksanctum.xand11.core.Platform.Companion.blue
import org.monksanctum.xand11.core.Platform.Companion.green
import org.monksanctum.xand11.core.Platform.Companion.red
import org.monksanctum.xand11.core.createBitmap
import kotlin.experimental.or


object GraphicsUtils {
    fun readBitmap(reader: PacketReader, leftPad: Byte, width: Int, height: Int,
                   depth: Byte, context: GraphicsContext): Bitmap {
        val bitmap = createBitmap(width, height)
        if (depth.toInt() == 1) {
            readDepth1Bitmap(reader, leftPad, bitmap, context)
        } else {
            throw RuntimeException("Healp!")
        }
        return bitmap
    }

    fun readZBitmap(reader: PacketReader, width: Int, height: Int): Bitmap {
        val bitmap = createBitmap(width, height)
        for (i in 0 until height) {
            for (j in 0 until width) {
                val b = reader.readByte().toInt()
                val g = reader.readByte().toInt()
                val r = reader.readByte().toInt()
                bitmap.setPixel(j, i, -0x1000000 or (r shl 16) or (g shl 8) or b)
            }
        }
        return bitmap
    }

    private fun readDepth1Bitmap(reader: PacketReader, leftPad: Byte, bitmap: Bitmap,
                                 context: GraphicsContext) {
        val width = bitmap.getWidth()
        val height = bitmap.getHeight()
        val rightPad = -(width + leftPad) and 7
        val bitReader = BitReader(reader)
        for (i in 0 until height) {
            for (k in 0 until leftPad) {
                bitReader.readBit()
            }
            for (j in 0 until width) {
                bitmap.setPixel(j, i,
                        if (bitReader.readBit()) context.foreground else context.background)
            }
            for (k in 0 until rightPad) {
                bitReader.readBit()
            }
        }
    }

    fun writeBitmap(writer: PacketWriter, leftPad: Byte, width: Int, height: Int,
                    depth: Byte, context: GraphicsContext?, bitmap: Bitmap) {
        if (depth.toInt() == 1) {
            writeDepth1Bitmap(writer, leftPad, bitmap, context!!)
        } else {
            throw RuntimeException("Healp!")
        }
    }

    fun writeZBitmap(writer: PacketWriter, width: Int, height: Int, bitmap: Bitmap) {
        var count = 0
        for (i in 0 until height) {
            for (j in 0 until width) {
                val pixel = bitmap.getPixel(j, i)
                writer.writeByte(blue(pixel).toByte())
                writer.writeByte(green(pixel).toByte())
                writer.writeByte(red(pixel).toByte())
                count += 3
            }
        }
        while (count % 4 != 0) {
            writer.writeByte(0.toByte())
            count++
        }
    }

    private fun writeDepth1Bitmap(writer: PacketWriter, leftPad: Byte, bitmap: Bitmap,
                                  context: GraphicsContext) {
        val width = bitmap.getWidth()
        val height = bitmap.getHeight()
        val rightPad = -(width + leftPad) and 7
        val bitReader = BitWriter(writer)
        for (i in 0 until height) {
            for (k in 0 until leftPad) {
                bitReader.writeBit(false)
            }
            for (j in 0 until width) {
                bitReader.writeBit(bitmap.getPixel(j, i) == context.foreground)
            }
            for (k in 0 until rightPad) {
                bitReader.writeBit(false)
            }
        }
    }

    private class BitReader(private val mReader: PacketReader) {
        private var mask: Int = 0
        private var value: Byte = 0

        fun readBit(): Boolean {
            if (mask == 0) {
                mask = 0x80
                value = mReader.readByte()
            }
            val bit = value.toInt() and mask
            mask = mask shr 1
            return bit != 0
        }
    }

    private class BitWriter(private val mWriter: PacketWriter) {
        private var mask = 0x80
        private var value: Byte = 0

        fun writeBit(bit: Boolean) {
            if (bit) value = value or mask.toByte()
            mask = mask shr 1
            if (mask == 0) {
                mask = 0x80
                mWriter.writeByte(value)
                value = 0
            }
        }
    }
}
