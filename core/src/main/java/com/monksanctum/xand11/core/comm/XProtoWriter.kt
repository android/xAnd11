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

package org.monksanctum.xand11.comm

import org.monksanctum.xand11.core.IOException
import org.monksanctum.xand11.core.OutputStream
import org.monksanctum.xand11.core.Platform
import org.monksanctum.xand11.core.Platform.Companion.intToHexString
import org.monksanctum.xand11.core.Throws

/**
 * Writes all of the basic types in the X11 protocol.
 */
open class XProtoWriter(private val mOutputStream: OutputStream?) {
    private var mMsb: Boolean = false
    private var mDebug: Boolean = false

    fun setDebug(debug: Boolean) {
        mDebug = debug
    }

    fun setMsb(isMsb: Boolean) {
        mMsb = isMsb
    }

    @Throws(XProtoWriter.WriteException::class)
    open fun writePaddedString(str: String) {
        val N = str.length
        writeString(str)
        writePadding((4 - N % 4) % 4)
    }

    @Throws(XProtoWriter.WriteException::class)
    open fun writeString(str: String) {
        val N = str.length
        for (i in 0 until N) {
            writeByte(str[i].toByte())
        }
    }

    @Throws(XProtoWriter.WriteException::class)
    open fun writePadding(length: Int) {
        for (i in 0 until length) {
            writeByte(0.toByte())
        }
    }

    @Throws(XProtoWriter.WriteException::class)
    open fun writeCard32(card32: Int) {
        if (mMsb) {
            writeByte(card32 shr 24)
            writeByte(card32 shr 16)
            writeByte(card32 shr 8)
            writeByte(card32 shr 0)
        } else {
            writeByte(card32 shr 0)
            writeByte(card32 shr 8)
            writeByte(card32 shr 16)
            writeByte(card32 shr 24)
        }
    }

    @Throws(XProtoWriter.WriteException::class)
    open fun writeCard16(card16: Int) {
        //if ((card16 & ~0xffff) != 0) {
        //    throw new IllegalArgumentException(card16 + " is not a card16");
        //}
        if (mMsb) {
            writeByte(card16 shr 8)
            writeByte(card16)
        } else {
            writeByte(card16)
            writeByte(card16 shr 8)
        }
    }

    @Throws(XProtoWriter.WriteException::class)
    private fun writeByte(i: Int) {
        writeByte((i and 0xff).toByte())
    }

    @Throws(XProtoWriter.WriteException::class)
    open fun writeByte(b: Byte) {
        if (LOG_EVERY_BYTE || mDebug) Platform.logd(TAG, "Write: " + intToHexString(b.toInt()))
        try {
            mOutputStream?.write(b.toInt())
        } catch (e: IOException) {
            throw WriteException(e)
        }

    }

    @Throws(XProtoWriter.WriteException::class)
    open fun flush() {
        try {
            mOutputStream?.flush()
        } catch (e: IOException) {
            throw WriteException(e)
        }

    }

    class WriteException : IOException {
        constructor(e: IOException) : super(null, e) {}

        constructor() {}
    }

    companion object {

        private val TAG = "XProtoWriter"
        // Enable with caution
        private val LOG_EVERY_BYTE = false
    }

}
