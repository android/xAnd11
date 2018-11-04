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

import org.monksanctum.xand11.Client
import org.monksanctum.xand11.Dispatcher
import org.monksanctum.xand11.comm.PacketReader
import org.monksanctum.xand11.comm.PacketWriter
import org.monksanctum.xand11.comm.Request
import org.monksanctum.xand11.errors.XError

class XScreenSaverProtocol : Dispatcher.PacketHandler {

    override val opCodes: ByteArray
        get() = HANDLED_OPS

    @Throws(XError::class)
    override fun handleRequest(client: Client, reader: PacketReader, writer: PacketWriter) {
        when (reader.majorOpCode) {
            Request.GET_SCREEN_SAVER -> handleGetScreenSaver(reader, writer)
        }
    }

    private fun handleGetScreenSaver(reader: PacketReader, writer: PacketWriter) {
        writer.writeCard16(0) // Timeout
        writer.writeCard16(0) // interval
        writer.writeByte(0.toByte()) // Prefer-blanking
        writer.writeByte(0.toByte()) // Allow-exposures
        writer.writePadding(18)
    }

    companion object {

        private val HANDLED_OPS = byteArrayOf(Request.GET_SCREEN_SAVER)
    }
}
