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
import org.monksanctum.xand11.core.InputStream
import org.monksanctum.xand11.core.Platform
import org.monksanctum.xand11.core.Platform.Companion.intToHexString
import org.monksanctum.xand11.core.Throws
import org.monksanctum.xand11.core.Utils.unsigned

/**
 * Reads all of the basic types specified in the X11 Protocol.
 */
open class XProtoReader(private val mInputStream: InputStream?) {
    private var mMsb: Boolean = false

    private var mDebug: Boolean = false

    fun setDebug(debug: Boolean) {
        mDebug = debug
    }

    fun setMsb(isMsb: Boolean) {
        mMsb = isMsb
    }

    @Throws(XProtoReader.ReadException::class)
    open fun readPaddedString(n: Int): String {
        val ret = readString(n)
        readPadding((4 - n % 4) % 4)
        return ret
    }

    @Throws(XProtoReader.ReadException::class)
    open fun readPaddedString16(n: Int): String {
        val ret = readString16(n)
        readPadding((4 - 2 * n % 4) % 4)
        return ret
    }

    @Throws(XProtoReader.ReadException::class)
    open fun readString16(n: Int): String {
        val builder = StringBuilder()
        for (i in 0 until n) {
            builder.append(readChar16().toChar())
        }
        return builder.toString()
    }

    @Throws(XProtoReader.ReadException::class)
    open fun readChar16(): Int {
        val first = readByte()
        val second = readByte()
        return if (mMsb) {
            unsigned(second) shl 8 or unsigned(first)
        } else {
            unsigned(first) shl 8 or unsigned(second)
        }
    }

    @Throws(XProtoReader.ReadException::class)
    open fun readString(n: Int): String {
        val builder = StringBuilder()
        for (i in 0 until n) {
            builder.append(readByte().toChar())
        }
        return builder.toString()
    }

    @Throws(XProtoReader.ReadException::class)
    open fun readPadding(n: Int) {
        for (i in 0 until n) {
            readByte()
        }
    }

    @Throws(XProtoReader.ReadException::class)
    open fun readCard16(): Int {
        val first = readByte()
        val second = readByte()
        return if (mMsb) {
            unsigned(first) shl 8 or unsigned(second)
        } else {
            unsigned(second) shl 8 or unsigned(first)
        }
    }

    @Throws(XProtoReader.ReadException::class)
    open fun readInt16(): Int {
        val first = readByte()
        val second = readByte()
        return if (mMsb) {
            first.toInt() * 256 or unsigned(second)
        } else {
            second.toInt() * 256 or unsigned(first)
        }
    }

    @Throws(XProtoReader.ReadException::class)
    open fun readCard32(): Int {
        val first = readCard16()
        val second = readCard16()
        return if (mMsb) {
            first * 65536 or second
        } else {
            second * 65536 or first
        }
    }

    @Throws(XProtoReader.ReadException::class)
    open fun readByte(): Byte {
        try {
            val read = mInputStream!!.read()
            if (LOG_EVERY_BYTE || mDebug) Platform.logd(TAG, "Read: " + intToHexString(read))
            if (read < 0) {
                throw ReadException()
            }
            return read.toByte()
        } catch (e: IOException) {
            throw ReadException(e)
        }

    }

    class ReadException : IOException {
        constructor(e: IOException) : super(null, e) {}

        constructor() {}
    }

    companion object {
        private val TAG = "XProtoReader"
        // Enable with caution
        private val LOG_EVERY_BYTE = false
    }
}
