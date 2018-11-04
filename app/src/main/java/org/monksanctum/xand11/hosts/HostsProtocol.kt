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

package org.monksanctum.xand11.hosts

import org.monksanctum.xand11.Client
import org.monksanctum.xand11.Dispatcher
import org.monksanctum.xand11.comm.PacketReader
import org.monksanctum.xand11.comm.PacketWriter
import org.monksanctum.xand11.comm.Request
import org.monksanctum.xand11.comm.XProtoReader
import org.monksanctum.xand11.comm.XProtoReader.ReadException
import org.monksanctum.xand11.comm.XProtoWriter
import org.monksanctum.xand11.comm.XProtoWriter.WriteException
import org.monksanctum.xand11.errors.XError

class HostsProtocol(private val mHostManager: HostsManager) : Dispatcher.PacketHandler {

    override val opCodes: ByteArray
        get() = HANDLED_OPS

    private fun handleListHosts(reader: PacketReader, writer: PacketWriter) {
        val hosts = mHostManager.hosts
        val N = hosts.size
        writer.writeCard16(hosts.size)
        writer.writePadding(22)
        for (i in 0 until N) {
            try {
                writeHost(hosts[i], writer)
            } catch (e: WriteException) {
                // Writing to PacketWriter, this can't happen.
            }

        }
    }

    private fun handleChangeHosts(reader: PacketReader, writer: PacketWriter) {
        val delete = reader.minorOpCode.toInt() != 0
        try {
            val host = readHost(reader)
            if (delete) {
                mHostManager.remHost(host)
            } else {
                mHostManager.addHost(host)
            }
        } catch (e: ReadException) {
            // Can't happen from PacketReader.
        }

    }

    @Throws(XError::class)
    override fun handleRequest(client: Client, reader: PacketReader, writer: PacketWriter) {
        when (reader.majorOpCode) {
            Request.LIST_HOSTS -> handleListHosts(reader, writer)
            Request.CHANGE_HOSTS -> handleChangeHosts(reader, writer)
        }
    }

    companion object {

        private val HANDLED_OPS = byteArrayOf(Request.LIST_HOSTS, Request.CHANGE_HOSTS)

        @Throws(XProtoWriter.WriteException::class)
        fun writeHost(host: Host, writer: XProtoWriter) {
            writer.writeByte(host.type)
            writer.writePadding(1)
            writer.writeCard16(host.address!!.size)
            writer.writePaddedString(String(host.address!!))
        }

        @Throws(XProtoReader.ReadException::class)
        fun readHost(reader: XProtoReader): Host {
            val host = Host()
            host.type = reader.readByte()
            reader.readPadding(1)
            val bytes = reader.readPaddedString(reader.readCard16())
            host.address = bytes.toByteArray()
            return host
        }
    }
}
