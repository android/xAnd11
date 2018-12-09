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

package org.monksanctum.xand11.extension

import org.monksanctum.xand11.comm.*
import org.monksanctum.xand11.comm.XProtoWriter.WriteException

class ExtensionProtocol(private val mExtensionManager: ExtensionManager) : Dispatcher.PacketHandler {

    override val opCodes: ByteArray
        get() = HANDLED_OPS

    override fun handleRequest(client: Client, reader: PacketReader, writer: PacketWriter) {
        when (reader.majorOpCode) {
            Request.QUERY_EXTENSION.code -> handleQueryExtension(reader, writer)
            Request.LIST_EXTENSIONS.code -> handleListExtension(reader, writer)
        }
    }

    private fun handleQueryExtension(packet: PacketReader, writer: PacketWriter) {
        val n = packet.readCard16()
        packet.readPadding(2)
        val name = packet.readPaddedString(n)
        val info = mExtensionManager.queryExtension(name)
        try {
            info.write(writer)
        } catch (e: WriteException) {
        }

    }

    private fun handleListExtension(packet: PacketReader, writer: PacketWriter) {
        var count = 0
        val exts = mExtensionManager.extensions
        writer.minorOpCode = exts.size.toByte()
        writer.writePadding(24)
        for (name in exts) {
            writer.writeString(name)
            writer.writeByte(0.toByte())
            count += name.length + 1
        }
        while (count++ % 4 != 0) {
            writer.writePadding(1)
        }
    }

    companion object {

        private val HANDLED_OPS = byteArrayOf(Request.QUERY_EXTENSION.code,
                Request.LIST_EXTENSIONS.code)
    }
}
