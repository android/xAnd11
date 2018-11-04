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
import android.graphics.Paint.Style
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import android.util.Pair

import org.monksanctum.xand11.Client
import org.monksanctum.xand11.Dispatcher.PacketHandler
import org.monksanctum.xand11.XService
import org.monksanctum.xand11.comm.PacketReader
import org.monksanctum.xand11.comm.PacketWriter
import org.monksanctum.xand11.comm.Request
import org.monksanctum.xand11.comm.XProtoReader
import org.monksanctum.xand11.comm.XProtoWriter
import org.monksanctum.xand11.comm.XSerializable
import org.monksanctum.xand11.errors.DrawableError
import org.monksanctum.xand11.errors.GContextError
import org.monksanctum.xand11.errors.XError
import org.monksanctum.xand11.fonts.Font
import org.monksanctum.xand11.fonts.FontManager

import java.util.ArrayList

import org.monksanctum.xand11.graphics.GraphicsManager.Companion.DEBUG

class DrawingProtocol(private val mGraphics: GraphicsManager, private val mFontManager: FontManager) : PacketHandler {
    private val mBounds = Rect()

    override val opCodes: ByteArray
        get() = HANDLED_OPS

    @Throws(XError::class)
    override fun handleRequest(client: Client, reader: PacketReader, writer: PacketWriter) {
        when (reader.majorOpCode) {
            Request.POLY_LINE -> handlePolyLine(reader)
            Request.FILL_POLY -> handleFillPoly(reader)
            Request.POLY_SEGMENT -> handlePolySegment(reader)
            Request.POLY_TEXT_8 -> handlePolyText8(reader)
            Request.POLY_FILL_RECTANGLE -> handlePolyFillRectangle(reader)
            Request.SET_CLIP_RECTANGLES -> handleSetClipRectangles(reader)
            Request.POLY_RECTANGLE -> handlePolyRectangle(reader)
            Request.IMAGE_TEXT_16 -> handleImageText16(reader, writer)
        }
    }

    @Throws(XError::class)
    private fun handleImageText16(reader: PacketReader, writer: PacketWriter) {
        val strLen = reader.minorOpCode.toInt()
        val drawableId = reader.readCard32()
        val gcontextId = reader.readCard32()
        val x = reader.readCard16()
        val y = reader.readCard16()
        val str = reader.readPaddedString16(strLen)
        val context = mGraphics.getGc(gcontextId)
        val drawable = mGraphics.getDrawable(drawableId)
        synchronized(drawable) {
            val font = mFontManager.getFont(context.font)
            val paint = font.paint
            val rect = Rect()
            font.getTextBounds(str, x, y, rect)
            paint.style = Style.FILL
            paint.color = context.background
            val canvas = drawable.lockCanvas(context)
            canvas.drawRect(rect, paint)

            paint.color = context.foreground
            canvas.drawText(str, x.toFloat(), y.toFloat(), paint)
            if (DEBUG)
                Log.d(TAG, "Drawing text " + x + " " + y + " \"" + str + "\" " +
                        Integer.toHexString(context.foreground))
            drawable.unlockCanvas()
        }
    }

    @Throws(XError::class)
    private fun handlePolyFillRectangle(reader: PacketReader) {
        val drawable = mGraphics.getDrawable(reader.readCard32())
        val gContext = mGraphics.getGc(reader.readCard32())
        val r = Rectangle()
        val p = gContext.paint
        p!!.style = Style.FILL
        synchronized(drawable) {
            val canvas = drawable.lockCanvas(gContext)
            while (reader.remaining != 0) {
                try {
                    r.read(reader)
                    if (DEBUG) Log.d(TAG, "Poly fill rectangle " + r.r)
                    canvas.drawRect(r.r, p)
                } catch (e: XProtoReader.ReadException) {
                    // Not possible here.
                    throw RuntimeException(e)
                }

            }
            drawable.unlockCanvas()
        }
    }

    @Throws(XError::class)
    private fun handlePolyText8(reader: PacketReader) {
        val drawable = mGraphics.getDrawable(reader.readCard32())
        val gContext = mGraphics.getGc(reader.readCard32())
        var x = reader.readCard16()
        val y = reader.readCard16()
        val c = drawable.lockCanvas(gContext)
        var paint = gContext.paint
        val item = TextItem8()
        while (reader.remaining > 1) {
            try {
                item.read(reader)
            } catch (e: XProtoReader.ReadException) {
                // Not possible here.
                throw RuntimeException(e)
            }

            if (item.mFontShift) {
                val font = item.mFont
                val f = mFontManager.getFont(font)
                paint = gContext.applyToPaint(f.paint)
            } else {
                x += item.mDelta.toInt()
                Font.getTextBounds(item.mValue!!, paint!!, x, y, mBounds)

                paint.color = gContext.foreground
                if (DEBUG) Log.d(TAG, "Poly text " + item.mValue + " " + x + " " + y)
                c.drawText(item.mValue!!, x.toFloat(), y.toFloat(), paint)
                x += mBounds.width()
            }
        }
        if (reader.remaining != 0) {
            // Just to avoid the warnings, read the last byte.
            reader.readPadding(1)
        }
        drawable.unlockCanvas()
    }

    @Throws(DrawableError::class, GContextError::class)
    private fun handlePolyLine(reader: PacketReader) {
        val drawable = mGraphics.getDrawable(reader.readCard32())
        val gContext = mGraphics.getGc(reader.readCard32())
        val mode = reader.minorOpCode
        val p = readPath(mode, reader)

        synchronized(drawable) {
            val c = drawable.lockCanvas(gContext)
            if (DEBUG) Log.d(TAG, "Poly line $p")
            val paint = gContext.paint
            paint!!.style = Style.STROKE
            c.drawPath(p, paint)
            drawable.unlockCanvas()
        }
    }

    @Throws(DrawableError::class, GContextError::class)
    private fun handleFillPoly(reader: PacketReader) {
        val drawable = mGraphics.getDrawable(reader.readCard32())
        val gContext = mGraphics.getGc(reader.readCard32())
        // TODO: Really need to use these.
        val shape = reader.readByte()
        val mode = reader.readByte()
        reader.readPadding(2)
        val p = readPath(mode, reader)

        synchronized(drawable) {
            val c = drawable.lockCanvas(gContext)
            val paint = Paint(gContext.paint)
            paint.style = Style.FILL
            if (DEBUG) Log.d(TAG, "Filling poly $p")
            c.drawPath(p, paint)
            drawable.unlockCanvas()
        }
    }

    @Throws(DrawableError::class, GContextError::class)
    private fun handlePolySegment(reader: PacketReader) {
        val drawable = mGraphics.getDrawable(reader.readCard32())
        val gContext = mGraphics.getGc(reader.readCard32())
        val segments = ArrayList<Pair<Pair<Int, Int>, Pair<Int, Int>>>()
        while (reader.remaining != 0) {
            val start = Pair(reader.readCard16(), reader.readCard16())
            val end = Pair(reader.readCard16(), reader.readCard16())
            segments.add(Pair(start, end))
        }
        val N = segments.size
        synchronized(drawable) {
            val c = drawable.lockCanvas(gContext)
            for (i in 0 until N) {
                val segment = segments[i]
                val start = segment.first
                val end = segment.second
                c.drawLine(start.first.toFloat(), start.second.toFloat(), end.first.toFloat(), end.second.toFloat(), gContext.paint!!)
            }
            if (DEBUG) Log.d(TAG, "Poly segment")
            drawable.unlockCanvas()
        }
    }

    @Throws(XError::class)
    private fun handleSetClipRectangles(reader: PacketReader) {
        val gContext = mGraphics.getGc(reader.readCard32())
        val x = reader.readInt16()
        val y = reader.readInt16()
        val r = Rectangle()
        val p = Path()
        while (reader.remaining != 0) {
            try {
                r.read(reader)
            } catch (e: XProtoReader.ReadException) {
                // Not possible here.
                throw RuntimeException(e)
            }

            if (DEBUG) Log.d(TAG, "Set clip rectangles " + r.r)
            p.addRect(RectF(r.r), Path.Direction.CW)
        }
        gContext.setClipPath(p)
    }

    @Throws(XError::class)
    private fun handlePolyRectangle(reader: PacketReader) {
        val drawable = mGraphics.getDrawable(reader.readCard32())
        val gContext = mGraphics.getGc(reader.readCard32())
        val r = Rectangle()
        val p = gContext.paint
        p!!.style = Style.STROKE
        synchronized(drawable) {
            val canvas = drawable.lockCanvas(gContext)
            while (reader.remaining != 0) {
                try {
                    r.read(reader)
                    if (DEBUG) Log.d(TAG, "Poly rectangle " + r.r)
                    canvas.drawRect(r.r, p)
                } catch (e: XProtoReader.ReadException) {
                    // Not possible here.
                    throw RuntimeException(e)
                }

            }
            drawable.unlockCanvas()
        }
    }

    private fun readPath(mode: Byte, reader: PacketReader): Path {
        val p = Path()
        val startX = reader.readCard16()
        val startY = reader.readCard16()
        p.moveTo(startX.toFloat(), startY.toFloat())
        while (reader.remaining != 0) {
            val x = reader.readCard16().toShort().toInt()
            val y = reader.readCard16().toShort().toInt()
            if (mode == COORDINATES_RELATIVE) {
                p.rLineTo(x.toFloat(), y.toFloat())
            } else {
                p.lineTo(x.toFloat(), y.toFloat())
            }
        }
        //p.lineTo(startX, startY);
        return p
    }

    internal class Rectangle : XSerializable {
        internal val r = Rect()

        @Throws(XProtoWriter.WriteException::class)
        override fun write(writer: XProtoWriter) {
            writer.writeCard16(r.left)
            writer.writeCard16(r.top)
            writer.writeCard16(r.width())
            writer.writeCard16(r.height())
        }

        @Throws(XProtoReader.ReadException::class)
        override fun read(reader: XProtoReader) {
            r.left = reader.readCard16()
            r.top = reader.readCard16()
            r.right = r.left + reader.readCard16()
            r.bottom = r.top + reader.readCard16()
        }
    }

    internal class TextItem8 : XSerializable {

        internal var mDelta: Byte = 0
        internal var mValue: String? = null
        internal var mFontShift: Boolean = false
        internal var mFont: Int = 0

        @Throws(XProtoReader.ReadException::class)
        override fun read(reader: XProtoReader) {
            val len = reader.readByte().toInt()
            if (len == 255) {
                mFontShift = true
                val f3 = reader.readByte().toInt()
                val f2 = reader.readByte().toInt()
                val f1 = reader.readByte().toInt()
                mFont = f3 shl 24 or (f2 shl 16) or (f1 shl 8) or reader.readByte().toInt()
            } else {
                mFontShift = false
                mDelta = reader.readByte()
                mValue = reader.readString(len)
            }
        }

        @Throws(XProtoWriter.WriteException::class)
        override fun write(writer: XProtoWriter) {
            if (mFontShift) {
                writer.writeByte(255.toByte())
                writer.writeByte((mFont shr 24).toByte())
                writer.writeByte((mFont shr 16).toByte())
                writer.writeByte((mFont shr 8).toByte())
                writer.writeByte((mFont shr 0).toByte())
            } else {
                writer.writeByte(mValue!!.length.toByte())
                writer.writeByte(mDelta)
                writer.writeString(mValue!!)
            }
        }
    }

    companion object {

        private val TAG = "DrawingProtocol"

        val DEBUG = XService.DRAWING_DEBUG

        private val HANDLED_OPS = byteArrayOf(Request.POLY_SEGMENT, Request.FILL_POLY, Request.POLY_LINE, Request.POLY_TEXT_8, Request.POLY_FILL_RECTANGLE, Request.SET_CLIP_RECTANGLES, Request.POLY_RECTANGLE, Request.IMAGE_TEXT_16)

        val COORDINATES_ABSOLUTE: Byte = 0
        val COORDINATES_RELATIVE: Byte = 1
    }
}
