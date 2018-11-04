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

package org.monksanctum.xand11.windows

import android.graphics.Rect

import org.monksanctum.xand11.Client
import org.monksanctum.xand11.Dispatcher
import org.monksanctum.xand11.atoms.AtomManager
import org.monksanctum.xand11.comm.BitmaskParser
import org.monksanctum.xand11.comm.PacketReader
import org.monksanctum.xand11.comm.PacketWriter
import org.monksanctum.xand11.comm.Request
import org.monksanctum.xand11.errors.AtomError
import org.monksanctum.xand11.errors.MatchError
import org.monksanctum.xand11.errors.PixmapError
import org.monksanctum.xand11.errors.WindowError
import org.monksanctum.xand11.errors.XError
import org.monksanctum.xand11.graphics.ColorPaintable
import org.monksanctum.xand11.windows.XWindow.Property

import org.monksanctum.xand11.windows.XWindow.Companion.FLAG_MAPPED

class XWindowProtocol(private val mWindowManager: XWindowManager) : Dispatcher.PacketHandler {

    override val opCodes: ByteArray
        get() = HANDLED_OP_CODES

    @Throws(XError::class)
    override fun handleRequest(client: Client, reader: PacketReader, writer: PacketWriter) {
        when (reader.majorOpCode) {
            Request.CREATE_WINDOW -> handleCreateWindow(client, reader)
            Request.CHANGE_WINDOW_ATTRIBUTES -> handleChangeWindowAttributes(client, reader)
            Request.CHANGE_PROPERTY -> handleChangeProperty(reader)
            Request.GET_PROPERTY -> handleGetProperty(reader, writer)
            Request.DELETE_PROPERTY -> handleDeleteProperty(reader)
            Request.MAP_WINDOW -> handleMapWindows(reader)
            Request.MAP_SUBWINDOWS -> handleMapSubwindows(reader)
            Request.UNMAP_WINDOW -> handleUnmapWindows(reader)
            Request.UNMAP_SUBWINDOWS -> handleUnmapSubwindows(reader)
            Request.DESTROY_WINDOW -> handleDestroyWindows(reader)
            Request.DESTROY_SUBWINDOWS -> handleDestroySubwindows(reader)
            Request.CONFIGURE_WINDOW -> handleConfigureWindow(reader)
            Request.GET_WINDOW_ATTRIBUTES -> handleGetWindowAttributes(client, reader, writer)
            Request.CLEAR_AREA -> handleClearArea(reader)
            Request.QUERY_TREE -> handleQueryTree(reader, writer)
            Request.REPARENT_WINDOW -> handleReparentWindow(reader)
        }
    }

    @Throws(XError::class)
    private fun handleReparentWindow(reader: PacketReader) {
        val window = mWindowManager.getWindow(reader.readCard32())
        val newParent = mWindowManager.getWindow(reader.readCard32())
        val x = reader.readInt16()
        val y = reader.readInt16()
        synchronized(window) {
            val mapped = window.visibility and FLAG_MAPPED.toInt() != 0
            if (mapped) window.requestUnmap()
            window.parent?.let {
                synchronized(it) {
                    it.removeChildLocked(window)
                }
            }
            val bounds = window.bounds
            val width = bounds.width()
            val height = bounds.height()
            bounds.left = x
            bounds.top = y
            bounds.right = x + width
            bounds.bottom = y + height
            window.setBounds(bounds)
            synchronized(newParent) {
                newParent.addChildLocked(window)
            }
            if (mapped) window.requestMap()
        }
    }

    @Throws(WindowError::class)
    private fun handleQueryTree(reader: PacketReader, writer: PacketWriter) {
        val window = mWindowManager.getWindow(reader.readCard32())
        synchronized(window) {
            writer.writeCard32(XWindowManager.ROOT_WINDOW)
            writer.writeCard32(if (window.parent != null) window.parent!!.id else 0)
            val count = window.childCountLocked
            writer.writeCard16(count)
            writer.writePadding(14)
            for (i in 0 until count) {
                writer.writeCard32(window.getChildAtLocked(i).id)
            }
        }
    }

    @Throws(WindowError::class)
    private fun handleMapWindows(reader: PacketReader) {
        val window = mWindowManager.getWindow(reader.readCard32())
        synchronized(window) {
            window.requestMap()
        }
    }

    @Throws(WindowError::class)
    private fun handleMapSubwindows(reader: PacketReader) {
        val window = mWindowManager.getWindow(reader.readCard32())
        synchronized(window) {
            window.requestSubwindowMap()
        }
    }

    @Throws(WindowError::class)
    private fun handleUnmapWindows(reader: PacketReader) {
        val window = mWindowManager.getWindow(reader.readCard32())
        synchronized(window) {
            window.requestUnmap()
        }
    }

    @Throws(WindowError::class)
    private fun handleUnmapSubwindows(reader: PacketReader) {
        val window = mWindowManager.getWindow(reader.readCard32())
        synchronized(window) {
            window.requestSubwindowUnmap()
        }
    }

    @Throws(WindowError::class)
    private fun handleDestroyWindows(reader: PacketReader) {
        val window = mWindowManager.getWindow(reader.readCard32())
        synchronized(window) {
            window.destroyLocked()
        }
    }

    @Throws(WindowError::class)
    private fun handleDestroySubwindows(reader: PacketReader) {
        val window = mWindowManager.getWindow(reader.readCard32())
        synchronized(window) {
            window.requestSubwindowUnmap()
        }
    }

    @Throws(XError::class)
    private fun handleChangeWindowAttributes(client: Client, reader: PacketReader) {
        val window = mWindowManager.getWindow(reader.readCard32())
        synchronized(window) {
            readWindowAttributesLocked(client, window, reader)
        }
    }

    @Throws(XError::class)
    private fun handleCreateWindow(client: Client, reader: PacketReader) {
        val depth = reader.minorOpCode
        if (depth.toInt() != 32 && depth.toInt() != 0) {
            throw MatchError(depth.toInt())
        }
        val windowId = reader.readCard32()
        val parentId = reader.readCard32()
        val x = reader.readInt16()
        val y = reader.readInt16()
        val width = reader.readCard16()
        val height = reader.readCard16()
        val borderWidth = reader.readCard16()
        val windowClass = reader.readCard16()
        val visualId = reader.readCard32()
        if (visualId > 1) {
            throw MatchError(visualId)
        }
        val window = mWindowManager.createWindow(windowId, width, height,
                windowClass.toByte(), parentId)
        synchronized(window) {
            window.setBorderWidth(borderWidth)
            window.setBounds(Rect(x, y, x + width, y + height))
            readWindowAttributesLocked(client, window, reader)
        }
        // Eat up the extra bytes so we don't get a warning.
        // This is expected to have some arbitrary padding.
        reader.readPadding(reader.remaining)
    }

    @Throws(XError::class)
    private fun readWindowAttributesLocked(client: Client, window: XWindow,
                                           reader: PacketReader) {
        val value = reader.readCard32()
        synchronized(window) {
            object : BitmaskParser(value, 0x4000) {
                @Throws(PixmapError::class)
                override fun readValue(mask: Int) {
                    when (mask) {
                        0x01 -> {
                            val pixmap = reader.readCard32()
                            if (pixmap == XWindow.BACKGROUND_NONE) {
                                window.background = null
                            } else if (pixmap == XWindow.BACKGROUND_PARENT_RELATIVE) {
                                window.setBackgroundParent()
                            } else {
                                window.background = mWindowManager.graphicsManager.getPixmap(
                                        pixmap)
                            }
                        }
                        0x02 -> {
                            val color = reader.readCard32()
                            window.background = ColorPaintable(color)
                        }
                        0x04 -> {
                            val borderPixmap = reader.readCard32()
                            if (borderPixmap == XWindow.BORDER_COPY_PARENT) {
                                window.border = window.parent!!.border
                            } else {
                                window.border = mWindowManager.graphicsManager.getPixmap(
                                        borderPixmap)
                            }
                        }
                        0x08 -> window.border = ColorPaintable(reader.readCard32())
                        0x10 -> {
                            window.bitGravity = reader.readByte()
                            reader.readPadding(3)
                        }
                        0x20 -> {
                            window.winGravity = reader.readByte()
                            reader.readPadding(3)
                        }
                        0x40 -> {
                            window.backing = reader.readByte()
                            reader.readPadding(3)
                        }
                        0x80 -> window.backingPlanes = reader.readCard32()
                        0x100 -> window.backingPixels = reader.readCard32()
                        0x200 -> {
                            window.isOverrideRedirect = reader.readByte().toInt() != 0
                            reader.readPadding(3)
                        }
                        0x400 -> {
                            window.isSaveUnder = reader.readByte().toInt() != 0
                            reader.readPadding(3)
                        }
                        0x800 -> {
                            val eventMask = reader.readCard32()
                            window.setEventMask(client, eventMask)
                        }
                        0x1000 -> window.setDoNotPropagate(reader.readCard32())
                        0x2000 -> {
                            var colorMap = reader.readCard32()
                            if (colorMap == XWindow.COLORMAP_COPY_PARENT) {
                                colorMap = window.parent!!.colorMap
                            }
                            window.colorMap = colorMap
                        }
                        0x4000 -> window.cursor = reader.readCard32()
                    }
                }
            }
        }
    }

    @Throws(WindowError::class, AtomError::class, MatchError::class)
    private fun handleDeleteProperty(reader: PacketReader) {
        val window = mWindowManager.getWindow(reader.readCard32())
        val name = reader.readCard32()
        AtomManager.instance.getString(name)
        synchronized(window) {
            window.removePropertyLocked(name)
        }
    }

    @Throws(WindowError::class, AtomError::class, MatchError::class)
    private fun handleChangeProperty(reader: PacketReader) {
        val window = mWindowManager.getWindow(reader.readCard32())
        val name = reader.readCard32()
        AtomManager.instance.getString(name)
        val mode = reader.minorOpCode
        val type = reader.readCard32()
        val format = reader.readByte()
        reader.readPadding(3)
        var length = reader.readCard32()
        if (format == Property.FORMAT_N_2) {
            length *= 2
        } else if (format == Property.FORMAT_N_4) {
            length *= 4
        }
        val value = reader.readPaddedString(length)
        var property: Property?
        synchronized(window) {
            property = window.getPropertyLocked(name, false) ?: Property(0).also {
                it.value = ByteArray(0)
                it.typeAtom = type
                it.format = format
                window.addPropertyLocked(name, it)
            }
        }
        property?.let {
            synchronized(it) {
            if (it.typeAtom != type) {
                throw MatchError(type)
            }
            if (it.format != format) {
                throw MatchError(format.toInt())
            }
            it.change(mode, value.toByteArray())
        }
        }
        synchronized(window) {
            window.notifyPropertyChanged(name)
        }
    }

    @Throws(XError::class)
    private fun handleGetWindowAttributes(client: Client, reader: PacketReader, writer: PacketWriter) {
        val window = mWindowManager.getWindow(reader.readCard32())
        synchronized(window) {
            writer.minorOpCode = window.backing
            writer.writeCard32(window.id)
            writer.writeCard16(window.windowClass.toInt())
            writer.writeByte(window.bitGravity)
            writer.writeByte(window.winGravity)
            writer.writeCard32(window.backingPlanes)
            writer.writeCard32(window.backingPixels)
            writer.writeByte((if (window.isSaveUnder) 1 else 0).toByte())
            writer.writeByte(1.toByte())
            writer.writeByte((if (window.visibility == 0)
                0
            else if (window.visibility > 1)
                2
            else
                1).toByte())
            writer.writeByte((if (window.isOverrideRedirect) 1 else 0).toByte())
            writer.writeCard32(window.colorMap)
            writer.writeCard32(window.eventMask)
            writer.writeCard32(window.getEventMask(client))
            writer.writeCard16(window.doNotPropogate)
            writer.writePadding(2)
        }
    }

    @Throws(XError::class)
    private fun handleGetProperty(reader: PacketReader, writer: PacketWriter) {
        // TODO: Use this.
        val delete = reader.minorOpCode.toInt() != 0

        val windowId = reader.readCard32()
        val atom = reader.readCard32()

        // TODO: Use these.
        val type = reader.readCard32()
        val offset = reader.readCard32()
        val length = reader.readCard32()

        val window = mWindowManager.getWindow(windowId)
        // Get the atom to check validity, but don't hang onto it.
        AtomManager.instance.getString(atom)

        var property: Property = synchronized(window) {
            window.getPropertyLocked(atom, delete)
        } ?: Property(0).also {
            it.value = ByteArray(0)
        }
        synchronized(property) {
            writer.minorOpCode = property.format
            writer.writeCard32(property.typeAtom)
            writer.writeCard32(property.value.size)
            writer.writeCard32(getLength(property.value.size, property.format))
            writer.writePadding(12)
            writer.writePaddedString(String(property.value))
        }
    }

    @Throws(WindowError::class)
    private fun handleConfigureWindow(reader: PacketReader) {
        val windowId = reader.readCard32()
        val window = mWindowManager.getWindow(windowId)
        val mask = reader.readCard16()
        var r: (() -> Unit)? = null
        var change = false
        reader.readPadding(2)
        synchronized(window) {
            val bounds = window.bounds
            val beforeWidth = bounds.width()
            val beforeHeight = bounds.height()
            if (mask and 0x01 != 0) {
                bounds.left = reader.readInt16()
                reader.readPadding(2)
            }
            if (mask and 0x02 != 0) {
                bounds.top = reader.readInt16()
                reader.readPadding(2)
            }
            if (mask and 0x04 != 0) {
                val width = reader.readCard16()
                reader.readPadding(2)
                bounds.right = width + bounds.left
            } else {
                bounds.right = beforeWidth + bounds.left
            }
            if (mask and 0x08 != 0) {
                val height = reader.readCard16()
                reader.readPadding(2)
                bounds.bottom = height + bounds.top
            } else {
                bounds.bottom = beforeHeight + bounds.top
            }
            change = window.setBounds(bounds)
            if (mask and 0x10 != 0) {
                change = change or window.setBorderWidth(reader.readCard16())
                reader.readPadding(2)
            }
            if (mask and 0x40 != 0) {
                var siblingId = 0
                if (mask and 0x20 != 0) {
                    siblingId = reader.readCard32()
                }
                val stackMode = reader.readCard32()
                val parent = window.parent
                if (siblingId != 0) {
                    val sibling = mWindowManager.getWindow(siblingId)
                    r = {
                        parent?.let {
                            synchronized(it) {
                                try {
                                    it.stackWindowLocked(window, sibling, stackMode)
                                } catch (windowError: WindowError) {
                                    windowError.printStackTrace()
                                }

                            }
                        }
                    }
                } else {
                    r = {
                        parent?.let {
                            synchronized(it) {
                                try {
                                    it.stackWindowLocked(window, stackMode)
                                } catch (windowError: WindowError) {
                                    windowError.printStackTrace()
                                }

                            }
                        }
                    }
                }
                change = true
            }
        }
        r?.invoke()
        if (change) {
            window.notifyConfigureWindow()
        }
    }

    @Throws(WindowError::class)
    private fun handleClearArea(reader: PacketReader) {
        val exposures = reader.minorOpCode.toInt() != 0
        val windowId = reader.readCard32()
        val x = reader.readCard16()
        val y = reader.readCard16()
        var width = reader.readCard16()
        var height = reader.readCard16()
        val window = mWindowManager.getWindow(windowId)
        synchronized(window) {
            if (width == 0) {
                width = window.width - x
            }
            if (height == 0) {
                height = window.height - y
            }
            window.clearArea(x, y, width, height)
        }
    }

    private fun getLength(length: Int, format: Byte): Int {
        when (format) {
            Property.FORMAT_N -> return length
            Property.FORMAT_N_2 -> return length / 2
            Property.FORMAT_N_4 -> return length / 4
        }
        return 0
    }

    companion object {

        private val HANDLED_OP_CODES = byteArrayOf(Request.GET_PROPERTY, Request.DELETE_PROPERTY, Request.CHANGE_PROPERTY, Request.CREATE_WINDOW, Request.CHANGE_WINDOW_ATTRIBUTES, Request.MAP_SUBWINDOWS, Request.MAP_WINDOW, Request.CONFIGURE_WINDOW, Request.GET_WINDOW_ATTRIBUTES, Request.CLEAR_AREA, Request.UNMAP_SUBWINDOWS, Request.UNMAP_WINDOW, Request.DESTROY_SUBWINDOWS, Request.DESTROY_WINDOW, Request.QUERY_TREE, Request.REPARENT_WINDOW)
    }
}
