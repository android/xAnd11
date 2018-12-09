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

import org.monksanctum.xand11.comm.*
import org.monksanctum.xand11.core.*
import org.monksanctum.xand11.errors.DrawableError
import org.monksanctum.xand11.errors.MatchError
import org.monksanctum.xand11.errors.ValueError
import org.monksanctum.xand11.errors.XError
import org.monksanctum.xand11.fonts.FontManager
import org.monksanctum.xand11.graphics.GraphicsManager.Companion.DEBUG


class GraphicsProtocol(private val mManager: GraphicsManager, private val mFontManager: FontManager) : Dispatcher.PacketHandler {

    override val opCodes: ByteArray
        get() = HANDLED_OPS

    @Throws(XError::class)
    override fun handleRequest(client: Client, reader: PacketReader, writer: PacketWriter) {
        when (reader.majorOpCode) {
            Request.CREATE_GC.code -> handleCreateGc(reader)
            Request.CHANGE_GC.code -> handleChangeGc(reader)
            Request.FREE_GC.code -> handleFreeGc(reader)
            Request.CREATE_PIXMAP.code -> handleCreatePixmap(reader)
            Request.FREE_PIXMAP.code -> handleFreePixmap(reader)
            Request.PUT_IMAGE.code -> handlePutImage(reader)
            Request.GET_IMAGE.code -> handleGetImage(reader, writer)
            Request.GET_GEOMETRY.code -> handleGetGeometry(reader, writer)
            Request.QUERY_BEST_SIZE.code -> handleRequestBestSize(reader, writer)
            Request.COPY_PLANE.code -> handleCopyPlane(reader, writer)
        }
    }

    private fun handleRequestBestSize(reader: PacketReader, writer: PacketWriter) {
        val type = reader.minorOpCode
        val drawable = reader.readCard32()
        val width = reader.readCard16()
        val height = reader.readCard16()

        writer.writeCard16(width)
        writer.writeCard16(height)
        writer.writePadding(20)
    }

    @Throws(DrawableError::class)
    private fun handleGetGeometry(reader: PacketReader, writer: PacketWriter) {
        val drawable = mManager.getDrawable(reader.readCard32())
        synchronized(drawable) {
            writer.writeCard32(drawable.parent!!.id)
            writer.writeCard16(drawable.x)
            writer.writeCard16(drawable.y)
            writer.writeCard16(drawable.width)
            writer.writeCard16(drawable.height)
            writer.writeCard16(drawable.borderWidth)
            writer.writePadding(10)
        }
    }

    @Throws(XError::class)
    private fun handlePutImage(reader: PacketReader) {
        val drawableId = reader.readCard32()
        val gcontextId = reader.readCard32()
        val width = reader.readCard16()
        val height = reader.readCard16()
        val x = reader.readCard16() // Should be Int16
        val y = reader.readCard16() // Should be Int16
        val leftPad = reader.readByte()
        val depth = reader.readByte()
        val format = reader.minorOpCode
        reader.readPadding(2)
        if (format.toInt() == ZPIXMAP) {
            if (leftPad.toInt() != 0) {
                Platform.logd(TAG, "Invalid $depth $leftPad")
                throw MatchError(depth.toInt())
            }
        }
        if (depth.toInt() != 1 && depth.toInt() != 32) {
            throw MatchError(depth.toInt())
        }
        val context = mManager.getGc(gcontextId)
        val drawable = mManager.getDrawable(drawableId)
        val bitmap = if (depth.toInt() != 1)
            GraphicsUtils.readZBitmap(reader, width, height)
        else
            GraphicsUtils.readBitmap(reader, leftPad, width, height, depth, context)
        drawable.withCanvas(context) {
            if (DEBUG) Platform.logd(TAG, "Drawing bitmap " + x + " " + y + " "
                    + bitmap.getWidth() + " " + bitmap.getHeight())
            context.drawBitmap(it, bitmap, x.toFloat(), y.toFloat())
        }
    }

    @Throws(XError::class)
    private fun handleGetImage(reader: PacketReader, writer: PacketWriter) {
        val drawableId = reader.readCard32()
        val x = reader.readCard16() // Should be Int16
        val y = reader.readCard16() // Should be Int16
        val width = reader.readCard16()
        val height = reader.readCard16()
        val planeMask = reader.readCard32()
        val format = reader.minorOpCode

        val drawable = mManager.getDrawable(drawableId)
        val bitmap = createBitmap(width, height)
        synchronized(drawable) {
            drawable.read(bitmap, x, y, width, height)
        }

        writer.minorOpCode = drawable.depth.toByte()
        writer.writeCard32(drawableId)
        writer.writePadding(20)
        if (format.toInt() == ZPIXMAP) {
            GraphicsUtils.writeZBitmap(writer, width, height, bitmap)
        } else {
            GraphicsUtils.writeBitmap(writer, 0.toByte(), width, height, 32.toByte(), null, bitmap)
        }
    }

    @Throws(ValueError::class, DrawableError::class)
    private fun handleCreatePixmap(reader: PacketReader) {
        val pixmap = reader.readCard32()
        // TODO: Validate drawable id.
        val drawable = reader.readCard32()
        val depth = reader.minorOpCode
        val width = reader.readCard16()
        val height = reader.readCard16()
        mManager.createPixmap(pixmap, depth, width, height, mManager.getDrawable(drawable))
    }

    private fun handleFreePixmap(reader: PacketReader) {
        val pixmap = reader.readCard32()
        mManager.freePixmap(pixmap)
    }

    @Throws(XError::class)
    private fun handleCopyPlane(reader: PacketReader, writer: PacketWriter) {
        val srcDrawableId = reader.readCard32()
        val destDrawableId = reader.readCard32()
        val gcontextId = reader.readCard32()
        val srcX = reader.readInt16()
        val srcY = reader.readInt16()
        val destX = reader.readInt16()
        val destY = reader.readInt16()
        val width = reader.readCard16()
        val height = reader.readCard16()
        val bitPlane = reader.readCard32()

        val src = mManager.getDrawable(srcDrawableId)
        val dest = mManager.getDrawable(destDrawableId)
        val gc = mManager.getGc(gcontextId)
        val bitmap = createBitmap(width, height)
        synchronized(src) {
            src.read(bitmap, srcX, srcY, width, height)
        }
        for (i in 0 until height) {
            for (j in 0 until width) {
                bitmap.setPixel(j, i, if (bitmap.getPixel(j, i) and bitPlane != 0)
                    gc.foreground
                else
                    gc.background)
            }
        }
        dest.withCanvas(gc) {
            gc.drawBitmap(it, bitmap, destX.toFloat(), destY.toFloat())
        }
    }

    @Throws(XError::class)
    fun handleChangeGc(reader: PacketReader) {
        val gc = mManager.getGc(reader.readCard32())
        synchronized(gc) {
            parseGc(reader, gc)
            gc.createPaint(mFontManager)
            // Eat up the extra bytes so we don't get a warning.
            // This is expected to have some arbitrary padding.
            reader.readPadding(reader.remaining)
        }
    }

    @Throws(XError::class)
    fun handleCreateGc(reader: PacketReader) {
        // TODO (SPEC)
        val id = reader.readCard32()
        val gc = mManager.createGc(id)
        synchronized(gc) {
            val drawable = reader.readCard32()
            gc.drawable = drawable
            parseGc(reader, gc)
            gc.createPaint(mFontManager)
            // Eat up the extra bytes so we don't get a warning.
            // This is expected to have some arbitrary padding.
            reader.readPadding(reader.remaining)
        }
    }

    @Throws(XError::class)
    private fun parseGc(reader: PacketReader, gc: GraphicsContext) {
        object : BitmaskParser(reader.readCard32(), 0x400000) {
            @Throws(ValueError::class)
            override fun readValue(mask: Int) {
                when (mask) {
                    0x01 -> gc.function = reader.readByte()
                    0x02 -> gc.planeMask = reader.readCard32()
                    0x04 -> gc.foreground = reader.readCard32()
                    0x08 -> gc.background = reader.readCard32()
                    0x10 -> gc.lineWidth = reader.readCard16()
                    0x20 -> gc.lineStyle = reader.readByte()
                    0x40 -> gc.capStyle = reader.readByte()
                    0x80 -> gc.joinStyle = reader.readByte()
                    0x100 -> gc.fillStyle = reader.readByte()
                    0x200 -> gc.fillRule = reader.readByte()
                    0x400 -> gc.tile = reader.readCard32()
                    0x800 -> gc.stipple = reader.readCard32()
                    0x1000 -> gc.tileStippleX = reader.readCard16()
                    0x2000 -> gc.tileStippleY = reader.readCard16()
                    0x4000 -> gc.font = reader.readCard32()
                    0x8000 -> gc.subwindowMode = reader.readByte()
                    0x10000 -> gc.graphicsExposures = reader.readByte().toInt() != 0
                    0x20000 -> gc.clipX = reader.readCard16()
                    0x40000 -> gc.clipY = reader.readCard16()
                    0x80000 -> {
                        gc.clipMask = reader.readCard32()
                        if (gc.clipMask != 0) {
                            throw ValueError(gc.clipMask)
                        }
                    }
                    0x100000 -> gc.dashOffset = reader.readCard16()
                    0x200000 -> gc.dashes = reader.readByte()
                    0x400000 -> gc.arcMode = reader.readByte()
                }
            }
        }
    }

    private fun handleFreeGc(reader: PacketReader) {
        val id = reader.readCard32()
        mManager.freeGc(id)
    }

    companion object {
        private val TAG = "GraphicsProtocol"

        private val HANDLED_OPS = byteArrayOf(Request.CREATE_GC.code,
                Request.CHANGE_GC.code,
                Request.FREE_GC.code,
                Request.CREATE_PIXMAP.code,
                Request.FREE_PIXMAP.code,
                Request.PUT_IMAGE.code,
                Request.GET_IMAGE.code,
                Request.GET_GEOMETRY.code,
                Request.QUERY_BEST_SIZE.code,
                Request.COPY_PLANE.code)

        val BITMAP = 0
        val XYPIXMAP = 1
        val ZPIXMAP = 2
    }
}
