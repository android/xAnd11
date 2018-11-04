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

package org.monksanctum.xand11

import android.content.Context
import android.util.DisplayMetrics
import android.view.Display
import android.view.WindowManager

import org.monksanctum.xand11.comm.XProtoReader
import org.monksanctum.xand11.comm.XProtoWriter
import org.monksanctum.xand11.comm.XSerializable
import org.monksanctum.xand11.display.Format
import org.monksanctum.xand11.display.Screen

import java.util.ArrayList

class XServerInfo : XSerializable {

    private val mFormats = ArrayList<Format>()
    private val mScreens = ArrayList<Screen>()

    private var mReleaseNumber = 1
    private var mMaxPacketLength = 0x7FFF
    private var mResourceIdMask = 0x00fffff
    private var mResourceIdBase = 0x1f00000
    private var mMotionBufferSize = 5
    private var mBitmapScanlineUnit = 8.toByte()
    private var mBitmapScanlinePad = 8.toByte()
    private var mMinKeycode = 8.toByte()
    private var mMaxKeycode = (-127).toByte()
    private var mImageOrder = ImageOrder.LSB_FIRST
    private var mBitmapOrder = BitmapOrder.MOST_BIT_FIRST
    private var mVendor = "org.monksactum" // ?

    val formats: List<Format>
        get() = mFormats

    val screens: List<Screen>
        get() = mScreens

    constructor(context: Context) {
        val display = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
                .defaultDisplay
        val metrics = DisplayMetrics()
        display.getMetrics(metrics)
        mScreens.add(Screen(metrics))
        mFormats.add(Format())
    }

    constructor() {
        // For reading.
    }

    @Throws(XProtoReader.ReadException::class)
    override fun read(reader: XProtoReader) {
        mReleaseNumber = reader.readCard32()
        mResourceIdBase = reader.readCard32()
        mResourceIdMask = reader.readCard32()
        mMotionBufferSize = reader.readCard32()

        val vendorLength = reader.readCard16()
        mMaxPacketLength = reader.readCard16()
        val numScreens = reader.readByte().toInt()
        val numFormats = reader.readByte().toInt()

        mImageOrder = reader.readByte()
        mBitmapOrder = reader.readByte()

        mBitmapScanlineUnit = reader.readByte()
        mBitmapScanlinePad = reader.readByte()
        mMinKeycode = reader.readByte()
        mMaxKeycode = reader.readByte()

        reader.readPadding(4)
        mVendor = reader.readPaddedString(vendorLength)
        mFormats.clear()
        mScreens.clear()
        for (i in 0 until numFormats) {
            val format = Format()
            format.read(reader)
            mFormats.add(format)
        }
        for (i in 0 until numScreens) {
            val screen = Screen()
            screen.read(reader)
            mScreens.add(screen)
        }
    }

    @Throws(XProtoWriter.WriteException::class)
    override fun write(writer: XProtoWriter) {
        val formats = formats
        val screens = screens

        writer.writeCard32(mReleaseNumber)
        writer.writeCard32(mResourceIdBase) // reseource-id-base
        writer.writeCard32(mResourceIdMask) // reseource-id-mask
        writer.writeCard32(mMotionBufferSize) // motion-buffer-size

        writer.writeCard16(mVendor.length)
        writer.writeCard16(mMaxPacketLength)
        writer.writeByte(screens.size.toByte())
        writer.writeByte(formats.size.toByte())

        writer.writeByte(mImageOrder)
        writer.writeByte(mBitmapOrder)

        writer.writeByte(mBitmapScanlineUnit) // bitmap-format-scanline-unit
        writer.writeByte(mBitmapScanlinePad) // bitmap-format-scanline-upad
        writer.writeByte(mMinKeycode) // min-keycode
        writer.writeByte(mMaxKeycode) // max-keycode

        writer.writePadding(4)
        writer.writePaddedString(mVendor)
        for (i in formats.indices) {
            formats[i].write(writer)
        }
        for (i in screens.indices) {
            screens[i].write(writer)
        }
    }

    override fun toString(): String {
        val builder = StringBuilder()
        builder.append("XServerInfo($mReleaseNumber) {")
        builder.append("  vendor=$mVendor").append('\n')
        builder.append("  resources;base=").append(Integer.toHexString(mResourceIdBase))
                .append(",mask=").append(Integer.toHexString(mResourceIdMask)).append('\n')
        builder.append("  motionBufferSize=").append(mMotionBufferSize).append('\n')
        builder.append("  maxPacket=").append(mMaxPacketLength).append('\n')
        builder.append("  order;image=").append(mImageOrder.toInt()).append(",bitmap=").append(mBitmapOrder.toInt())
                .append('\n')
        builder.append("  bitmapScan;unit=").append(mBitmapScanlineUnit.toInt()).append(",pad=")
                .append(mBitmapScanlinePad.toInt()).append('\n')
        builder.append("  keycode;min=").append(mMinKeycode.toInt()).append(",max=").append(mMaxKeycode.toInt())
                .append('\n')
        builder.append("  screens=[ \n")
        for (screen in mScreens) {
            builder.append("    ").append(screen.toString()).append(",\n")
        }
        builder.append("]\n")
        builder.append("  formats=[ ")
        for (format in mFormats) {
            builder.append(format.toString()).append(", ")
        }
        builder.append("]\n")
        builder.append("}")
        return builder.toString()
    }

    object ImageOrder {
        val LSB_FIRST: Byte = 0
        val MSB_FIRST: Byte = 1
    }

    object BitmapOrder {
        val LEAST_BIT_FIRST: Byte = 0
        val MOST_BIT_FIRST: Byte = 1
    }

    companion object {

        private val TAG = "XServerInfo"
    }
}
