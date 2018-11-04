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

/**
 * Writes all bytes into a buffer so they can be easily counted
 * before being written to the XProtoWriter.
 */
class PacketWriter(// TODO: Something more efficient than ArrayList.
        private val mWriter: XProtoWriter) : XProtoWriter(null) {

    var minorOpCode: Byte = 0
    private var mPacket = ByteArray(32)
    var length = 0
        private set

    val WordCount:Int get() {
        return length / 4
    }

    fun copyToReader(): PacketReader {
        return PacketReader(0.toByte(), minorOpCode, mPacket, length)
    }

    override fun writeByte(b: Byte) {
        mPacket[length++] = b
        if (length == mPacket.size) {
            val nPacket = ByteArray(mPacket.size * 2)
            for (i in mPacket.indices) {
                nPacket[i] = mPacket[i]
            }
            mPacket = nPacket
        }
    }

    internal fun reset() {
        length = 0
    }

    @Throws(XProtoWriter.WriteException::class)
    override fun flush() {
        for (i in 0 until length) {
            mWriter.writeByte(mPacket[i])
        }
        mWriter.flush()
    }

    // --- Versions of writeX that don't throw exceptions because they aren't possible ---

    override fun writeCard16(card16: Int) {
        try {
            super.writeCard16(card16)
        } catch (e: XProtoWriter.WriteException) {
            throw RuntimeException(e)
        }

    }

    override fun writeCard32(card32: Int) {
        try {
            super.writeCard32(card32)
        } catch (e: XProtoWriter.WriteException) {
            throw RuntimeException(e)
        }

    }

    override fun writePaddedString(str: String) {
        try {
            super.writePaddedString(str)
        } catch (e: XProtoWriter.WriteException) {
            throw RuntimeException(e)
        }

    }

    override fun writePadding(length: Int) {
        try {
            super.writePadding(length)
        } catch (e: XProtoWriter.WriteException) {
            throw RuntimeException(e)
        }

    }

    override fun writeString(str: String) {
        try {
            super.writeString(str)
        } catch (e: XProtoWriter.WriteException) {
            throw RuntimeException(e)
        }

    }
}
