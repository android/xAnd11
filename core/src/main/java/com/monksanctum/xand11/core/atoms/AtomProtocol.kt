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

package org.monksanctum.xand11.atoms

import org.monksanctum.xand11.comm.*
import org.monksanctum.xand11.core.Throws
import org.monksanctum.xand11.errors.AtomError
import org.monksanctum.xand11.errors.XError

class AtomProtocol : Dispatcher.PacketHandler {

    private val mAtomManager: AtomManager

    override val opCodes: ByteArray
        get() = HANDLED_OPS

    init {
        mAtomManager = AtomManager.instance
    }

    @Throws(XError::class)
    override fun handleRequest(client: Client, reader: PacketReader, writer: PacketWriter) {
        when (reader.majorOpCode) {
            Request.INTERN_ATOM.code -> handleInternAtom(reader, writer)
            Request.GET_ATOM_NAME.code -> handleGetAtomName(reader, writer)
        }
    }

    private fun handleInternAtom(reader: PacketReader, writer: PacketWriter) {
        val length = reader.readCard16()
        reader.readPadding(2)
        val name = reader.readPaddedString(length)
        writer.writeCard32(mAtomManager.internAtom(name))
        writer.writePadding(20)
    }

    @Throws(AtomError::class)
    private fun handleGetAtomName(reader: PacketReader, writer: PacketWriter) {
        val atom = reader.readCard32()
        val str = mAtomManager.getString(atom)
        writer.writeCard16(str.length)
        writer.writePadding(22)
        writer.writePaddedString(str)
    }

    companion object {

        private val HANDLED_OPS = byteArrayOf(Request.INTERN_ATOM.code, Request.GET_ATOM_NAME.code)
    }
}
