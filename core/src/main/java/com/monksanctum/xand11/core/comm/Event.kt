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

import org.monksanctum.xand11.core.ObjectPool
import org.monksanctum.xand11.core.Rect
import org.monksanctum.xand11.core.Utils
import org.monksanctum.xand11.core.post


enum class Event(val code: Byte) {
    ERROR(0),
    REPLY(1),
    KEY_PRESS(2),
    KEY_RELEASE(3),
    BUTTON_PRESS(4),
    BUTTON_RELEASE(5),

    MOTION_NOTIFY(6),
    ENTER_NOTIFY(7),
    LEAVE_NOTIFY(8),

    FOCUS_IN(9),
    FOCUS_OUT(10),

    KEYMAP_NOTIFY(11),

    EXPOSE(12),
    GRAPHICS_EXPOSE(13),
    NO_EXPOSE(14),
    VISIBILITY_NOTIFY(15),
    CREATE_NOTIFY(16),
    DESTROY_NOTIFY(17),

    UNMAP_NOTIFY(18),
    MAP_NOTIFY(19),
    MAP_REQUEST(20),
    REPARENT_NOTIFY(21),

    CONFIGURE_NOTIFY(22),
    CONFIGURE_REQUEST(23),
    GRAVITY_NOTIFY(24),
    RESIZE_REQUEST(25),
    CIRCULATE_NOTIFY(26),
    CILCULATE_REQUEST(27),

    PROPERTY_NOTIFY(28),
    SELECTION_CLEAR(29),
    SELECTION_REQUEST(30),
    SELECTION_NOTIFY(31),
    COLORMAP_NOTIFY(32),

    CLIENT_MESSAGE(33),
    MAPPING_NOTIFY(34);

    companion object {

        private val sNames = arrayOfNulls<String>(256)

        // TODO: Probobly should be per client or something...?
        private val sEvent = 1

        private val sEventPool: ObjectPool<EventInfo, String> = object : ObjectPool<EventInfo, String>() {
            override fun create(vararg arg: String): EventInfo {
                return EventInfo()
            }
        }

        fun sendPropertyChange(client: Client, window: Int, atom: Int, deleted: Boolean) {
            val writer = PacketWriter(client.clientListener.writer)

            writer.writeCard32(window)
            writer.writeCard32(atom)
            writer.writeCard32(client.timestamp)
            writer.writeByte((if (deleted) 1 else 0).toByte())
            writer.writePadding(15)
            client.clientListener.sendPacket(MAP_NOTIFY, writer)
        }

        fun sendMapNotify(client: Client, window: Int, event: Int,
                          overrideRedirect: Boolean) {
            val writer = PacketWriter(client.clientListener.writer)
            writer.writeCard32(event)
            writer.writeCard32(window)
            writer.writeByte((if (overrideRedirect) 1 else 0).toByte())
            writer.writePadding(19)
            client.clientListener.sendPacket(MAP_NOTIFY, writer)
        }

        fun sendUnmapNotify(client: Client, window: Int, event: Int,
                            fromConfigure: Boolean) {
            val writer = PacketWriter(client.clientListener.writer)
            writer.writeCard32(event)
            writer.writeCard32(window)
            writer.writeByte((if (fromConfigure) 1 else 0).toByte())
            writer.writePadding(19)
            client.clientListener.sendPacket(UNMAP_NOTIFY, writer)
        }

        fun sendSelectionRequest(client: Client, timestamp: Int, owner: Int, requestor: Int,
                                 selection: Int, target: Int, property: Int) {
            val writer = PacketWriter(client.clientListener.writer)
            writer.writeCard32(timestamp)
            writer.writeCard32(owner)
            writer.writeCard32(requestor)
            writer.writeCard32(selection)
            writer.writeCard32(target)
            writer.writeCard32(property)
            writer.writePadding(4)
            client.clientListener.sendPacket(SELECTION_REQUEST, writer)
        }

        fun sendSelectionNotify(client: Client, timestamp: Int, requestor: Int,
                                selection: Int, target: Int, property: Int) {
            val writer = PacketWriter(client.clientListener.writer)
            writer.writeCard32(timestamp)
            writer.writeCard32(requestor)
            writer.writeCard32(selection)
            writer.writeCard32(target)
            writer.writeCard32(property)
            writer.writePadding(8)
            client.clientListener.sendPacket(SELECTION_NOTIFY, writer)
        }

        fun sendSelectionClear(client: Client, timestamp: Int, owner: Int, selection: Int) {
            val writer = PacketWriter(client.clientListener.writer)
            writer.writeCard32(timestamp)
            writer.writeCard32(owner)
            writer.writeCard32(selection)
            writer.writePadding(16)
            client.clientListener.sendPacket(SELECTION_CLEAR, writer)
        }

        fun sendExpose(client: Client, window: Int, bounds: Rect) {
            val writer = PacketWriter(client.clientListener.writer)
            writer.writeCard32(window)
            writer.writeCard16(bounds.left)
            writer.writeCard16(bounds.top)
            writer.writeCard16(bounds.width())
            writer.writeCard16(bounds.height())
            writer.writeCard16(0) // What?
            writer.writePadding(14)

            Utils.sBgHandler.post { client.clientListener.sendPacket(EXPOSE, writer) }
        }

        fun sendConfigureWindow(client: Client, event: Int, window: Int, sibling: Int,
                                bounds: Rect, borderWidth: Int, overrideDirect: Boolean) {
            val writer = PacketWriter(client.clientListener.writer)
            writer.writeCard32(event)
            writer.writeCard32(window)
            writer.writeCard32(sibling)
            writer.writeCard16(bounds.left)
            writer.writeCard16(bounds.top)
            writer.writeCard16(bounds.width())
            writer.writeCard16(bounds.height())
            writer.writeCard16(borderWidth)
            writer.writeByte((if (overrideDirect) 1 else 0).toByte())
            writer.writePadding(5)

            client.clientListener.sendPacket(CONFIGURE_NOTIFY, writer)
        }

        fun sendKeyDown(client: Client, id: Int, x: Int, y: Int, keyCode: Int, state: Int) {
            val writer = PacketWriter(client.clientListener.writer)
            writer.minorOpCode = keyCode.toByte()
            writer.writeCard32(client.timestamp)
            writer.writeCard32(3)
            writer.writeCard32(id)
            writer.writeCard32(0) // Child
            writer.writeCard16(x) // Root-x
            writer.writeCard16(y) // Root-y
            writer.writeCard16(x)
            writer.writeCard16(y)

            writer.writeCard16(state) // Keybutton state
            writer.writeByte(1.toByte()) // Same screen.
            writer.writePadding(1)

            client.clientListener.sendPacket(KEY_PRESS, writer)
        }

        fun sendKeyUp(client: Client, id: Int, x: Int, y: Int, keyCode: Int, state: Int) {
            val writer = PacketWriter(client.clientListener.writer)
            writer.minorOpCode = keyCode.toByte()
            writer.writeCard32(client.timestamp)
            writer.writeCard32(3)
            writer.writeCard32(id)
            writer.writeCard32(0) // Child
            writer.writeCard16(0) // Root-x
            writer.writeCard16(0) // Root-y
            writer.writeCard16(x)
            writer.writeCard16(y)

            writer.writeCard16(state) // Keybutton state
            writer.writeByte(1.toByte())
            writer.writePadding(1)

            client.clientListener.sendPacket(KEY_RELEASE, writer)
        }

        fun sendEnter(client: Client, id: Int, x: Int, y: Int) {
            val writer = PacketWriter(client.clientListener.writer)
            writer.minorOpCode = 1.toByte()

            writer.writeCard32(client.timestamp)
            writer.writeCard32(id)
            writer.writeCard32(id)
            writer.writeCard32(0)

            writer.writeCard16(0) // Root-x
            writer.writeCard16(0) // Root-y
            writer.writeCard16(x)
            writer.writeCard16(y)
            writer.writeCard16(0) // Keybutton state

            writer.writeByte(0.toByte()) // Normal, not grab
            writer.writeByte(3.toByte()) // Same screen, focus

            client.clientListener.sendPacket(ENTER_NOTIFY, writer)
        }

        fun sendLeave(client: Client, id: Int, x: Int, y: Int) {
            val writer = PacketWriter(client.clientListener.writer)
            writer.minorOpCode = 1.toByte()

            writer.writeCard32(client.timestamp)
            writer.writeCard32(id)
            writer.writeCard32(id)
            writer.writeCard32(0)

            writer.writeCard16(0) // Root-x
            writer.writeCard16(0) // Root-y
            writer.writeCard16(x)
            writer.writeCard16(y)
            writer.writeCard16(0) // Keybutton state

            writer.writeByte(0.toByte()) // Normal, not grab
            writer.writeByte(3.toByte()) // Same screen, focus

            client.clientListener.sendPacket(LEAVE_NOTIFY, writer)
        }

        fun sendDestroy(client: Client, id: Int, arg: Int) {
            val writer = PacketWriter(client.clientListener.writer)
            writer.writeCard32(id)
            writer.writeCard32(arg)
            writer.writePadding(20)
            client.clientListener.sendPacket(DESTROY_NOTIFY, writer)
        }

        fun sendCreate(client: Client, id: Int, child: Int, x: Int, y: Int, width: Int,
                       height: Int, borderWidth: Int, overrideRedirect: Boolean) {
            val writer = PacketWriter(client.clientListener.writer)

            writer.writeCard32(id)
            writer.writeCard32(child)
            writer.writeCard16(x)
            writer.writeCard16(y)
            writer.writeCard16(width)
            writer.writeCard16(height)
            writer.writeCard16(borderWidth)
            writer.writeByte((if (overrideRedirect) 1 else 0).toByte())
            writer.writePadding(9)

            client.clientListener.sendPacket(CREATE_NOTIFY, writer)
        }

        fun populateNames() {
            enumValues<Event>().forEach {
                sNames[it.code.toInt()] = it.name
            }
        }

        fun getName(what: Int): String? {
            return if (sNames[what] == null) {
                "Unknown($what)"
            } else sNames[what]
        }

        fun obtainInfo(type: Event, id: Int, arg: Int): EventInfo {
            val e = sEventPool.obtain()
            e.type = type
            e.id = id
            e.arg = arg
            return e
        }

        fun obtainInfo(type: Event, id: Int, arg: Int, flag: Boolean): EventInfo {
            val e = sEventPool.obtain()
            e.type = type
            e.id = id
            e.arg = arg
            e.flag = flag
            return e
        }

        fun obtainInfo(type: Event, id: Int, arg: Int, arg2: Int, arg3: Int, arg4: Int,
                       arg5: Int, arg6: Int, flag: Boolean): EventInfo {
            val e = sEventPool.obtain()
            e.type = type
            e.id = id
            e.arg = arg
            e.arg2 = arg2
            e.arg3 = arg3
            e.arg4 = arg4
            e.arg5 = arg5
            e.arg6 = arg6
            e.flag = flag
            return e
        }

        fun obtainInfo(type: Event, id: Int, arg: Rect, flag: Boolean): EventInfo {
            val e = sEventPool.obtain()
            e.type = type
            e.id = id
            e.rect = arg
            e.flag = flag
            return e
        }

        fun obtainInfo(type: Event, id: Int, arg1: Int, arg2: Int, arg3: Int, rect: Rect,
                       flag: Boolean): EventInfo {
            val e = sEventPool.obtain()
            e.type = type
            e.id = id
            e.arg = arg1
            e.arg2 = arg2
            e.arg3 = arg3
            e.rect = rect
            e.flag = flag
            return e
        }

        fun obtainInfo(type: Event, id: Int, arg1: Int, arg2: Int, arg3: Int, arg4: Int,
                       arg5: Int): EventInfo {
            val e = sEventPool.obtain()
            e.type = type
            e.id = id
            e.arg = arg1
            e.arg2 = arg2
            e.arg3 = arg3
            e.arg4 = arg4
            e.arg5 = arg5
            return e
        }

        fun obtainInfo(type: Event, id: Int, arg1: Int, arg2: Int): EventInfo {
            val e = sEventPool.obtain()
            e.type = type
            e.id = id
            e.arg = arg1
            e.arg2 = arg2
            return e
        }

        fun obtainInfo(type: Event, id: Int, arg1: Int, arg2: Int, arg3: Int): EventInfo {
            val e = sEventPool.obtain()
            e.type = type
            e.id = id
            e.arg = arg1
            e.arg2 = arg2
            e.arg3 = arg3
            return e
        }

        fun obtainInfo(type: Event, id: Int, arg1: Int, arg2: Int, arg3: Int, arg4: Int): EventInfo {
            val e = sEventPool.obtain()
            e.type = type
            e.id = id
            e.arg = arg1
            e.arg2 = arg2
            e.arg3 = arg3
            e.arg4 = arg4
            return e
        }
    }

    class EventInfo : ObjectPool.Recycleable() {
        var type: Event = Event.ERROR
        var id: Int = 0
        var arg: Int = 0
        var arg2: Int = 0
        var arg3: Int = 0
        var arg5: Int = 0
        var arg4: Int = 0
        var arg6: Int = 0
        var flag: Boolean = false
        var rect: Rect? = null
    }
}
