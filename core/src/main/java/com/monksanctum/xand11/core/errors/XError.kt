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

package org.monksanctum.xand11.errors

import org.monksanctum.xand11.comm.Event
import org.monksanctum.xand11.comm.PacketReader
import org.monksanctum.xand11.comm.XProtoReader
import org.monksanctum.xand11.comm.XProtoReader.ReadException
import org.monksanctum.xand11.comm.XProtoWriter
import org.monksanctum.xand11.comm.XProtoWriter.WriteException
import org.monksanctum.xand11.comm.XSerializable
import org.monksanctum.xand11.core.Platform
import org.monksanctum.xand11.core.Throws

abstract class XError(val code: Byte, message: String) : Exception(message), XSerializable {
    private var mReader: PacketReader? = null

    abstract val extraData: Int

    fun setPacket(reader: PacketReader) {
        mReader = reader
    }

    @Throws(WriteException::class)
    override fun write(writer: XProtoWriter) {
        writer.writeCard32(extraData)
        writer.writeCard16(mReader!!.minorOpCode.toInt())
        writer.writeByte(mReader!!.majorOpCode)
        writer.writePadding(21)
    }

    @Throws(ReadException::class)
    override fun read(reader: XProtoReader) {
        Platform.logw(TAG, "Errors cannot be read")
        reader.readPadding(28)
    }

    companion object {

        val REQUEST: Byte = 1
        val VALUE: Byte = 2
        val WINDOW: Byte = 3
        val PIXMAP: Byte = 4
        val ATOM: Byte = 5
        val CURSOR: Byte = 6
        val FONT: Byte = 7
        val MATCH: Byte = 8
        val DRAWABLE: Byte = 9
        val ACCESS: Byte = 10
        val ALLOC: Byte = 11
        val COLORMAP: Byte = 12
        val GCONTEXT: Byte = 13
        val IDCHOICE: Byte = 14
        val NAME: Byte = 15
        val LENGTH: Byte = 16
        val IMPLEMENTATION: Byte = 17

        private val TAG = "XError"
    }
}
