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

import org.monksanctum.xand11.ObjectPool
import org.monksanctum.xand11.ObjectPool.Recycleable

/**
 * Reads all of the bytes of a packet into a buffer, so that it can
 * be passed off to another thread for processing, and not block
 * the reader.
 */
class PacketReader(val majorOpCode: Byte, val minorOpCode: Byte) : XProtoReader(null), AutoCloseable {
    private var mBuffer: ByteHolder? = null
    private var mIndex: Int = 0
    var length: Int = 0
        private set

    val remaining: Int
        get() = length - mIndex

    internal constructor(major: Byte, minor: Byte, bytes: ByteArray, len: Int) : this(major, minor) {
        length = bytes.size
        mBuffer = sArrays.obtain(Integer(length))
        for (i in bytes.indices) {
            mBuffer!!.mHolder[i] = bytes[i]
        }
    }

    override fun close() {
        if (mBuffer != null) {
            mBuffer!!.close()
            mBuffer = null
        }
    }

    /**
     * Note: May only be called once per PacketReader.
     */
    @Throws(XProtoReader.ReadException::class)
    fun readBytes(reader: XProtoReader, length: Int) {
        this.length = length
        mBuffer = sArrays.obtain(Integer(length))
        mIndex = 0
        for (i in 0 until length) {
            mBuffer!!.mHolder[i] = reader.readByte()
        }
    }

    fun reset() {
        mIndex = 0
    }

    override fun readByte(): Byte {
        return mBuffer!!.mHolder[mIndex++]
    }

    // --- Versions of readX that don't throw exceptions because they arent possible ---
    override fun readCard16(): Int {
        try {
            return super.readCard16()
        } catch (e: XProtoReader.ReadException) {
            throw RuntimeException(e)
        }

    }

    override fun readInt16(): Int {
        try {
            return super.readInt16()
        } catch (e: XProtoReader.ReadException) {
            throw RuntimeException(e)
        }

    }

    override fun readChar16(): Int {
        try {
            return super.readChar16()
        } catch (e: XProtoReader.ReadException) {
            throw RuntimeException(e)
        }

    }

    override fun readCard32(): Int {
        try {
            return super.readCard32()
        } catch (e: XProtoReader.ReadException) {
            throw RuntimeException(e)
        }

    }

    override fun readPaddedString(n: Int): String {
        try {
            return super.readPaddedString(n)
        } catch (e: XProtoReader.ReadException) {
            throw RuntimeException(e)
        }

    }

    override fun readString(n: Int): String {
        try {
            return super.readString(n)
        } catch (e: XProtoReader.ReadException) {
            throw RuntimeException(e)
        }

    }

    override fun readPaddedString16(n: Int): String {
        try {
            return super.readPaddedString16(n)
        } catch (e: XProtoReader.ReadException) {
            throw RuntimeException(e)
        }

    }

    override fun readString16(n: Int): String {
        try {
            return super.readString16(n)
        } catch (e: XProtoReader.ReadException) {
            throw RuntimeException(e)
        }

    }

    override fun readPadding(n: Int) {
        try {
            super.readPadding(n)
        } catch (e: XProtoReader.ReadException) {
            throw RuntimeException(e)
        }

    }

    class ByteHolder(size: Int) : Recycleable() {
        internal val mHolder: ByteArray

        init {
            mHolder = ByteArray(size)
        }
    }

    companion object {
        private val sArrays: ObjectPool<ByteHolder, Integer> =
            object : ObjectPool<ByteHolder, Integer>() {
                protected override fun validate(inst: ByteHolder, vararg arg: Integer): Boolean {
                    val length = inst.mHolder.size
                    return if (length > arg[0].toInt() * 4) {
                        false // Save really big ones for big packets.
                    } else length >= arg[0].toInt()
                }

                protected override fun create(vararg arg: Integer): ByteHolder {
                    return ByteHolder(arg[0].toInt())
                }
            }
    }
}
