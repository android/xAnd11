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

package org.monksanctum.xand11.display

import org.monksanctum.xand11.comm.XProtoReader
import org.monksanctum.xand11.comm.XProtoReader.ReadException
import org.monksanctum.xand11.comm.XProtoWriter
import org.monksanctum.xand11.comm.XSerializable
import org.monksanctum.xand11.core.Platform
import org.monksanctum.xand11.core.Platform.Companion.intToHexString
import org.monksanctum.xand11.core.Throws
import org.monksanctum.xand11.windows.XWindowManager

import kotlin.math.max

class Screen : XSerializable {

    private val mDepths = ArrayList<Depth>()

    private var mRootWindow: Int = 0
    private var mColorMap: Int = 0
    private var mWhitePixel: Int = 0
    private var mBlackPixel: Int = 0
    private var mEventMasks: Int = 0

    var size: Int = 0
        private set // Card16
    private var mHeight: Int = 0 // Card16
    private var mWidthMillis: Int = 0 // Card16
    private var mHeightMillis: Int = 0 // Card16

    private var mMinInstalledMaps: Int = 0 // Card16
    private var mMaxInstalledMaps: Int = 0 // Card16

    private var mVisialId: Int = 0

    private var mBackingStore: Byte = 0 // BackingStore

    private var mSaveUnders: Byte = 0 // Bool
    private var mRootDepth: Byte = 0

    constructor(width : Int, height : Int) {
        val size = max(width, height)
        mVisialId = 1
        mHeight = size
        this.size = mHeight
        mHeightMillis = 300
        mWidthMillis = mHeightMillis
        mMinInstalledMaps = 1
        mMaxInstalledMaps = 4
        mRootWindow = XWindowManager.ROOT_WINDOW
        mRootDepth = 32
        mWhitePixel = -0x1
        mBlackPixel = -0x1000000
        mEventMasks = 0
        mColorMap = 4
        mBackingStore = BackingStore.ALWAYS.toByte()
        mSaveUnders = 0
        mRootDepth = 32
        mDepths.add(Depth(32.toByte()))
    }

    constructor() {
        // For reading.
    }

    @Throws(XProtoWriter.WriteException::class)
    override fun write(writer: XProtoWriter) {
        writer.writeCard32(mRootWindow)
        writer.writeCard32(mColorMap)
        writer.writeCard32(mWhitePixel)
        writer.writeCard32(mBlackPixel)
        writer.writeCard32(mEventMasks)

        writer.writeCard16(size)
        writer.writeCard16(mHeight)
        writer.writeCard16(mWidthMillis)
        writer.writeCard16(mHeightMillis)

        writer.writeCard16(mMinInstalledMaps)
        writer.writeCard16(mMaxInstalledMaps)
        writer.writeCard32(mVisialId)

        writer.writeByte(mBackingStore)
        writer.writeByte(mSaveUnders)
        val N = mDepths.size
        writer.writeByte(mRootDepth)
        writer.writeByte(N.toByte())
        for (i in 0 until N) {
            mDepths[i].write(writer)
        }
    }

    @Throws(ReadException::class)
    override fun read(reader: XProtoReader) {
        mRootWindow = reader.readCard32()
        mColorMap = reader.readCard32()
        mWhitePixel = reader.readCard32()
        mBlackPixel = reader.readCard32()
        mEventMasks = reader.readCard32()

        size = reader.readCard16()
        mHeight = reader.readCard16()
        mWidthMillis = reader.readCard16()
        mHeightMillis = reader.readCard16()

        mMinInstalledMaps = reader.readCard16()
        mMaxInstalledMaps = reader.readCard16()
        mVisialId = reader.readCard32()

        mBackingStore = reader.readByte()
        mSaveUnders = reader.readByte()
        mRootDepth = reader.readByte()
        val N = reader.readByte().toInt()
        for (i in 0 until N) {
            val depth = Depth()
            depth.read(reader)
            mDepths.add(depth)
        }
    }

    class Depth : XSerializable {
        private var mDepth: Byte = 0
        private val mVisualTypes = ArrayList<VisualType>()

        constructor() {
            // Used for reading.
        }

        constructor(depth: Byte) {
            mDepth = depth
            mVisualTypes.add(VisualType())
        }

        override fun toString(): String {
            return "depth=" + mDepth + ",types=" + Platform.join(";", mVisualTypes)
        }

        @Throws(XProtoWriter.WriteException::class)
        override fun write(writer: XProtoWriter) {
            writer.writeByte(mDepth)
            writer.writePadding(1)
            val N = mVisualTypes.size
            writer.writeCard16(N)
            writer.writePadding(4)
            for (i in 0 until N) {
                mVisualTypes[i].write(writer)
            }
        }

        @Throws(ReadException::class)
        override fun read(reader: XProtoReader) {
            mDepth = reader.readByte()
            reader.readPadding(1)
            val N = reader.readCard16()
            reader.readPadding(4)
            for (i in 0 until N) {
                val type = VisualType()
                type.read(reader)
                mVisualTypes.add(type)
            }
        }
    }

    class VisualType : XSerializable {
        private var mVisualId: Int = 0
        private var mClass: Byte = 0 // VisualClass
        private var mBitsPerRgb: Byte = 0
        private var mColorMapEntries: Int = 0 // Card16
        private var mRedMask: Int = 0
        private var mGreenMask: Int = 0
        private var mBlueMask: Int = 0

        init {
            mVisualId = 1
            mClass = VisualClass.TRUE_COLOR
            mBitsPerRgb = 8
            mColorMapEntries = 256
            mRedMask = 0xff0000
            mGreenMask = 0x00ff00
            mBlueMask = 0x0000ff
        }

        override fun toString(): String {
            return ("(" + mVisualId + "," + mClass + "," + mBitsPerRgb + "," + mColorMapEntries
                    + "," + intToHexString(mRedMask) + "," + intToHexString(mGreenMask)
                    + "," + intToHexString(mBlueMask) + ")")
        }

        @Throws(XProtoWriter.WriteException::class)
        override fun write(writer: XProtoWriter) {
            writer.writeCard32(mVisualId)
            writer.writeByte(mClass)
            writer.writeByte(mBitsPerRgb)
            writer.writeCard16(mColorMapEntries)
            writer.writeCard32(mRedMask)
            writer.writeCard32(mGreenMask)
            writer.writeCard32(mBlueMask)
            writer.writePadding(4)
        }

        @Throws(ReadException::class)
        override fun read(reader: XProtoReader) {
            mVisualId = reader.readCard32()
            mClass = reader.readByte()
            mBitsPerRgb = reader.readByte()
            mColorMapEntries = reader.readCard16()
            mRedMask = reader.readCard32()
            mGreenMask = reader.readCard32()
            mBlueMask = reader.readCard32()
            reader.readPadding(4)
        }
    }

    override fun toString(): String {
        val builder = StringBuilder()
        builder.append("Screen(").append(mVisialId).append(',').append(mRootDepth.toInt()).append(")\n")
        builder.append("    rootWindow=").append(mRootWindow).append('\n')
        builder.append("    colorMap=").append(mColorMap).append(",min=").append(mMinInstalledMaps)
                .append(",max=").append(mMaxInstalledMaps).append('\n')
        builder.append("    white=").append(intToHexString(mWhitePixel))
                .append(",black=").append(intToHexString(mBlackPixel))
                .append('\n')
        builder.append("    eventMasks=").append(mEventMasks).append('\n')
        builder.append("    width=").append(size).append(",height=").append(mHeight).append('\n')
        builder.append("    millis;width=").append(mWidthMillis)
                .append(",height=").append(mHeightMillis).append('\n')
        builder.append("    backing=").append(mBackingStore.toInt()).append(",save=").append(mSaveUnders.toInt())
                .append('\n')
        builder.append("    depths={")
        for (depth in mDepths) {
            builder.append(depth.toString()).append(", ")
        }
        builder.append("}\n")
        return builder.toString()
    }

    object BackingStore {
        val NEVER = 0
        val WHEN_MAPPED = 1
        val ALWAYS = 2
    }

    object VisualClass {
        val STATIC_GRAY: Byte = 0
        val GRAY_SCALE: Byte = 1
        val STATIC_COLOR: Byte = 2
        val PSEUDO_COLOR: Byte = 3
        val TRUE_COLOR: Byte = 4
        val DIRECT_COLOR: Byte = 5
    }

}
