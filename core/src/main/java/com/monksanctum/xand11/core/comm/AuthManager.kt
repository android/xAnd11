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

import org.monksanctum.xand11.core.*
import org.monksanctum.xand11.core.XServerInfo
import org.monksanctum.xand11.hosts.HostsManager

/**
 * Handles the auth for the server.
 */
class AuthManager(private val mInfo: XServerInfo, // TODO: Actually use hosts.
                  private val mHosts: HostsManager) {

    @Throws(XProtoReader.ReadException::class, XProtoWriter.WriteException::class)
    fun handleAuth(reader: XProtoReader, writer: XProtoWriter, connection: Socket): Boolean {
        val majorVersion = reader.readCard16()
        val minorVersion = reader.readCard16()
        val n = reader.readCard16()
        val d = reader.readCard16()
        reader.readPadding(2)
        val authProtoName = reader.readPaddedString(n)
        val authProtoData = reader.readPaddedString(d)
        val host = connection.getHostAddress()
        //inetAddress.hostAddress

        if (DEBUG) {
            Platform.logd(TAG, "Incoming connection, host: " + host
                    + " protocol: " + majorVersion + "," + minorVersion
                    + " auth: " + authProtoName + " auth data: " + authProtoData)
        }

        if (!CARE_ABOUT_AUTH || Platform.isTextEmpty(authProtoName)) {
            acceptConnection(writer)
            return true
        } else {
            rejectConnection(writer, "Unknown auth $authProtoName")
            return false
        }
    }

    @Throws(XProtoWriter.WriteException::class)
    private fun acceptConnection(writer: XProtoWriter) {
        if (DEBUG) Platform.logd(TAG, "Writing accept")
        writer.writeByte(CONN_SUCCEEDED)
        writer.writePadding(1)
        writer.writeCard16(MAJOR_VERSION)
        writer.writeCard16(MINOR_VERSION)
        val packet = PacketWriter(writer)
        mInfo.write(packet)
        if (DEBUG) Platform.logd(TAG, "Packet length: ${packet.WordCount}")
        writer.writeCard16(packet.WordCount)
        packet.flush()
    }

    @Throws(XProtoWriter.WriteException::class)
    private fun rejectConnection(writer: XProtoWriter, reason: String) {
        if (DEBUG) Platform.logd(TAG, "Writing reject")
        writer.writeByte(CONN_FAILED)
        writer.writeByte(reason.length.toByte())
        writer.writeCard16(MAJOR_VERSION)
        writer.writeCard16(MINOR_VERSION)
        writer.writeCard16((reason.length + 3) / 4)
        writer.writePaddedString(reason)
    }

    companion object {

        private val TAG = "AuthManager"
        private val DEBUG = COMM_DEBUG

        private val CARE_ABOUT_AUTH = false

        val CONN_FAILED: Byte = 0
        val CONN_SUCCEEDED: Byte = 1
        val CONN_AUTH: Byte = 2
    }
}
