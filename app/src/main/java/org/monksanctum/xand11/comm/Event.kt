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

import android.graphics.Rect

import org.monksanctum.xand11.Client
import org.monksanctum.xand11.ObjectPool
import org.monksanctum.xand11.ObjectPool.Recycleable
import org.monksanctum.xand11.Utils

import java.lang.reflect.Field
import java.lang.reflect.Modifier

object Event {
    val ERROR: Byte = 0
    val REPLY: Byte = 1
    val KEY_PRESS: Byte = 2
    val KEY_RELEASE: Byte = 3
    val BUTTON_PRESS: Byte = 4
    val BUTTON_RELEASE: Byte = 5

    val MOTION_NOTIFY: Byte = 6
    val ENTER_NOTIFY: Byte = 7
    val LEAVE_NOTIFY: Byte = 8

    val FOCUS_IN: Byte = 9
    val FOCUS_OUT: Byte = 10

    val KEYMAP_NOTIFY: Byte = 11

    val EXPOSE: Byte = 12
    val GRAPHICS_EXPOSE: Byte = 13
    val NO_EXPOSE: Byte = 14
    val VISIBILITY_NOTIFY: Byte = 15
    val CREATE_NOTIFY: Byte = 16
    val DESTROY_NOTIFY: Byte = 17

    val UNMAP_NOTIFY: Byte = 18
    val MAP_NOTIFY: Byte = 19
    val MAP_REQUEST: Byte = 20
    val REPARENT_NOTIFY: Byte = 21

    val CONFIGURE_NOTIFY: Byte = 22
    val CONFIGURE_REQUEST: Byte = 23
    val GRAVITY_NOTIFY: Byte = 24
    val RESIZE_REQUEST: Byte = 25
    val CIRCULATE_NOTIFY: Byte = 26
    val CILCULATE_REQUEST: Byte = 27

    val PROPERTY_NOTIFY: Byte = 28
    val SELECTION_CLEAR: Byte = 29
    val SELECTION_REQUEST: Byte = 30
    val SELECTION_NOTIFY: Byte = 31
    val COLORMAP_NOTIFY: Byte = 32

    val CLIENT_MESSAGE: Byte = 33
    val MAPPING_NOTIFY: Byte = 34

    private val sNames = arrayOfNulls<String>(256)

    // TODO: Probobly should be per client or something...?
    private val sEvent = 1

    private val sEventPool = object : ObjectPool<EventInfo, Void>() {
        override fun create(vararg arg: Void): EventInfo {
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
        val cls = Event::class.java
        for (field in cls!!.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType() == Byte::class.javaPrimitiveType) {
                try {
                    val index = field.get(null) as Byte
                    sNames[index.toInt()] = field.name
                } catch (e: IllegalAccessException) {
                }

            }
        }
    }

    fun getName(what: Int): String? {
        return if (sNames[what] == null) {
            "Unknown($what)"
        } else sNames[what]
    }

    fun obtainInfo(type: Byte, id: Int, arg: Int): EventInfo {
        val e = sEventPool.obtain()
        e.type = type
        e.id = id
        e.arg = arg
        return e
    }

    fun obtainInfo(type: Byte, id: Int, arg: Int, flag: Boolean): EventInfo {
        val e = sEventPool.obtain()
        e.type = type
        e.id = id
        e.arg = arg
        e.flag = flag
        return e
    }

    fun obtainInfo(type: Byte, id: Int, arg: Int, arg2: Int, arg3: Int, arg4: Int,
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

    fun obtainInfo(type: Byte, id: Int, arg: Rect, flag: Boolean): EventInfo {
        val e = sEventPool.obtain()
        e.type = type
        e.id = id
        e.rect = arg
        e.flag = flag
        return e
    }

    fun obtainInfo(type: Byte, id: Int, arg1: Int, arg2: Int, arg3: Int, rect: Rect,
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

    fun obtainInfo(type: Byte, id: Int, arg1: Int, arg2: Int, arg3: Int, arg4: Int,
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

    fun obtainInfo(type: Byte, id: Int, arg1: Int, arg2: Int): EventInfo {
        val e = sEventPool.obtain()
        e.type = type
        e.id = id
        e.arg = arg1
        e.arg2 = arg2
        return e
    }

    fun obtainInfo(type: Byte, id: Int, arg1: Int, arg2: Int, arg3: Int): EventInfo {
        val e = sEventPool.obtain()
        e.type = type
        e.id = id
        e.arg = arg1
        e.arg2 = arg2
        e.arg3 = arg3
        return e
    }

    fun obtainInfo(type: Byte, id: Int, arg1: Int, arg2: Int, arg3: Int, arg4: Int): EventInfo {
        val e = sEventPool.obtain()
        e.type = type
        e.id = id
        e.arg = arg1
        e.arg2 = arg2
        e.arg3 = arg3
        e.arg4 = arg4
        return e
    }

    class EventInfo : Recycleable() {
        var type: Byte = 0
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
