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

package org.monksanctum.xand11

import android.util.Log

import org.monksanctum.xand11.comm.PacketReader
import org.monksanctum.xand11.comm.PacketWriter
import org.monksanctum.xand11.comm.XProtoWriter.WriteException
import org.monksanctum.xand11.errors.XError

import org.monksanctum.xand11.Utils.unsigned

/**
 * Connects up the input with all of the things that want to receive stuffs from the input.
 */
class Dispatcher {

    private val mHandlers = arrayOfNulls<PacketHandler>(256)

    fun addPacketHandler(handler: PacketHandler) {
        for (b in handler.opCodes) {
            mHandlers[unsigned(b)] = handler
        }
    }

    fun getHandler(opCode: Int): PacketHandler? {
        return mHandlers[opCode]
    }

    interface PacketHandler {
        val opCodes: ByteArray
        @Throws(XError::class)
        fun handleRequest(client: Client, reader: PacketReader, writer: PacketWriter)
    }

    companion object {

        private val TAG = "Dispatcher"
    }
}
