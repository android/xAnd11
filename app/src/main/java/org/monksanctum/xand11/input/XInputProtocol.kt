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

package org.monksanctum.xand11.input

import org.monksanctum.xand11.Client
import org.monksanctum.xand11.Dispatcher
import org.monksanctum.xand11.comm.PacketReader
import org.monksanctum.xand11.comm.PacketWriter
import org.monksanctum.xand11.comm.Request
import org.monksanctum.xand11.errors.WindowError
import org.monksanctum.xand11.errors.XError
import org.monksanctum.xand11.windows.XWindow
import org.monksanctum.xand11.windows.XWindowManager

class XInputProtocol(private val mManager: XInputManager, private val mWindowManager: XWindowManager) : Dispatcher.PacketHandler {

    override val opCodes: ByteArray
        get() = HANDLED_OPS

    @Throws(XError::class)
    override fun handleRequest(client: Client, reader: PacketReader, writer: PacketWriter) {
        when (reader.majorOpCode) {
            Request.GET_INPUT_FOCUS -> handleGetInputFocus(reader, writer)
            Request.GET_KEYBOARD_MAPPING -> handleGetKeyboardMapping(reader, writer)
            Request.GET_MODIFIER_MAPPING -> handleGetModifierMapping(reader, writer)
            Request.GET_KEYBOARD_CONTROL -> handleGetKeyboardControl(reader, writer)
            Request.GET_SELECTION_OWNER -> handleGetSelectionOwner(reader, writer)
            Request.SET_SELECTION_OWNER -> handleSetSelectionOwner(client, reader, writer)
            Request.CONVERT_SELECTION -> handleConvertSelectionOwner(client, reader, writer)
            Request.QUERY_POINTER -> handleQueryPointer(reader, writer)
            Request.GET_POINTER_CONTROL -> handleGetPointerControl(reader, writer)
        }
    }

    @Throws(WindowError::class)
    private fun handleConvertSelectionOwner(client: Client, reader: PacketReader,
                                            writer: PacketWriter) {
        var requestor = reader.readCard32()
        val selection = reader.readCard32()
        val target = reader.readCard32()
        val prop = reader.readCard32()
        var timestamp = reader.readCard32()

        val owner = mManager.selection.getOwner(selection)
        if (owner != 0) {
            // Send SelectionRequest
            val window = mWindowManager.getWindow(owner)
            window.sendSelectionRequest(timestamp, owner, requestor, selection, target, prop)
        } else {
            // Send SelectionNotify
            val window = mWindowManager.getWindow(requestor)
            if (timestamp == 0) timestamp = client.timestamp
            requestor = 0
            window.sendSelectionNotify(timestamp, requestor, selection, target, prop)
        }
    }

    @Throws(WindowError::class)
    private fun handleSetSelectionOwner(client: Client, reader: PacketReader, writer: PacketWriter) {
        val owner = reader.readCard32()
        val atom = reader.readCard32()
        var timestamp = reader.readCard32()
        if (timestamp == 0) timestamp = client.timestamp
        val currentOwner = mManager.selection.getOwner(atom)
        if (currentOwner == owner) {
            return
        }
        if (mManager.selection.setSelection(atom, owner, timestamp)) {
            if (currentOwner != 0) {
                val window = mWindowManager.getWindow(currentOwner)
                window.sendSelectionClear(timestamp, owner, atom)
            }
        }
    }

    private fun handleGetSelectionOwner(reader: PacketReader, writer: PacketWriter) {
        val atom = reader.readCard32()
        val window = mManager.selection.getOwner(atom)
        writer.writeCard32(window)
        writer.writePadding(20)
    }

    private fun handleGetInputFocus(reader: PacketReader, writer: PacketWriter) {
        val focus = mManager.focusState
        writer.minorOpCode = focus.revert
        writer.writeCard32(focus.current)
        writer.writePadding(20)
    }

    private fun handleGetKeyboardMapping(reader: PacketReader, writer: PacketWriter) {
        reader.readPadding(4)
        // TODO: Implement.
        writer.minorOpCode = mManager.keyboardManager.keysPerSym.toByte()
        writer.writePadding(24)
        val keyboardMap = mManager.keyboardManager.keyboardMap
        for (i in keyboardMap.indices) {
            writer.writeCard32(keyboardMap[i])
        }
    }

    private fun handleGetModifierMapping(reader: PacketReader, writer: PacketWriter) {
        // TODO: Implement.
        writer.minorOpCode = 2.toByte()
        writer.writePadding(24)
        val modifiers = mManager.keyboardManager.modifiers
        for (i in modifiers.indices) {
            if (modifiers[i].toInt() != 0) {
                writer.writeByte(mManager.translate(modifiers[i].toInt()).toByte())
            } else {
                writer.writeByte(0.toByte())
            }
        }
    }

    private fun handleGetKeyboardControl(reader: PacketReader, writer: PacketWriter) {
        writer.minorOpCode = 1.toByte()
        writer.writeCard32(0) // LED Mask.
        writer.writeByte(0.toByte())
        writer.writeByte(50.toByte())
        writer.writeCard16(400)
        writer.writeCard16(100)
        writer.writePadding(2)
        writer.writePadding(32)
    }

    private fun handleQueryPointer(reader: PacketReader, writer: PacketWriter) {
        writer.minorOpCode = 1.toByte()
        writer.writeCard32(3) // Root window
        writer.writeCard32(0) // No child
        // Location, 0
        writer.writeCard16(0) // Global X
        writer.writeCard16(0) // Global Y
        writer.writeCard16(0) // Local X
        writer.writeCard16(0) // Local Y

        writer.writeCard16(0) // Button mask
        writer.writePadding(6)
    }

    private fun handleGetPointerControl(reader: PacketReader, writer: PacketWriter) {
        writer.writeCard16(0) // Acceleration-numerator
        writer.writeCard16(0) // Acceleration-denominator
        writer.writeCard16(0) // threshold
        writer.writePadding(18)
    }

    companion object {

        private val HANDLED_OPS = byteArrayOf(Request.GET_INPUT_FOCUS, Request.GET_KEYBOARD_MAPPING, Request.GET_MODIFIER_MAPPING, Request.GET_KEYBOARD_CONTROL, Request.GET_SELECTION_OWNER, Request.SET_SELECTION_OWNER, Request.CONVERT_SELECTION, Request.QUERY_POINTER, Request.GET_POINTER_CONTROL)
    }
}
