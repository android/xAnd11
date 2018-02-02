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

package org.monksanctum.xand11.windows;

import android.graphics.Rect;
import android.util.Log;

import org.monksanctum.xand11.Client;
import org.monksanctum.xand11.Dispatcher;
import org.monksanctum.xand11.atoms.AtomManager;
import org.monksanctum.xand11.comm.BitmaskParser;
import org.monksanctum.xand11.comm.PacketReader;
import org.monksanctum.xand11.comm.PacketWriter;
import org.monksanctum.xand11.comm.Request;
import org.monksanctum.xand11.errors.AtomError;
import org.monksanctum.xand11.errors.MatchError;
import org.monksanctum.xand11.errors.PixmapError;
import org.monksanctum.xand11.errors.WindowError;
import org.monksanctum.xand11.errors.XError;
import org.monksanctum.xand11.graphics.ColorPaintable;
import org.monksanctum.xand11.windows.XWindow.Property;

import static org.monksanctum.xand11.windows.XWindow.STACK_ABOVE;
import static org.monksanctum.xand11.windows.XWindow.STACK_BELOW;
import static org.monksanctum.xand11.windows.XWindow.STACK_BOTTOM_IF;
import static org.monksanctum.xand11.windows.XWindow.STACK_OPPOSITE;
import static org.monksanctum.xand11.windows.XWindow.STACK_TOP_IF;

public class XWindowProtocol implements Dispatcher.PacketHandler {

    private static final byte[] HANDLED_OP_CODES = new byte[]{
            Request.GET_PROPERTY,
            Request.DELETE_PROPERTY,
            Request.CHANGE_PROPERTY,
            Request.CREATE_WINDOW,
            Request.CHANGE_WINDOW_ATTRIBUTES,
            Request.MAP_SUBWINDOWS,
            Request.MAP_WINDOW,
            Request.CONFIGURE_WINDOW,
            Request.GET_WINDOW_ATTRIBUTES,
            Request.CLEAR_AREA,
            Request.UNMAP_SUBWINDOWS,
            Request.UNMAP_WINDOW,
            Request.DESTROY_SUBWINDOWS,
            Request.DESTROY_WINDOW,
            Request.QUERY_TREE,
    };

    private final XWindowManager mWindowManager;

    public XWindowProtocol(XWindowManager manager) {
        mWindowManager = manager;
    }

    @Override
    public byte[] getOpCodes() {
        return HANDLED_OP_CODES;
    }

    @Override
    public void handleRequest(Client client, PacketReader reader, PacketWriter writer)
            throws XError {
        switch (reader.getMajorOpCode()) {
            case Request.CREATE_WINDOW:
                handleCreateWindow(client, reader);
                break;
            case Request.CHANGE_WINDOW_ATTRIBUTES:
                handleChangeWindowAttributes(client, reader);
                break;
            case Request.CHANGE_PROPERTY:
                handleChangeProperty(reader);
                break;
            case Request.GET_PROPERTY:
                handleGetProperty(reader, writer);
                break;
            case Request.DELETE_PROPERTY:
                handleDeleteProperty(reader);
                break;
            case Request.MAP_WINDOW:
                handleMapWindows(reader);
                break;
            case Request.MAP_SUBWINDOWS:
                handleMapSubwindows(reader);
                break;
            case Request.UNMAP_WINDOW:
                handleUnmapWindows(reader);
                break;
            case Request.UNMAP_SUBWINDOWS:
                handleUnmapSubwindows(reader);
                break;
            case Request.DESTROY_WINDOW:
                handleDestroyWindows(reader);
                break;
            case Request.DESTROY_SUBWINDOWS:
                handleDestroySubwindows(reader);
                break;
            case Request.CONFIGURE_WINDOW:
                handleConfigureWindow(reader);
                break;
            case Request.GET_WINDOW_ATTRIBUTES:
                handleGetWindowAttributes(client, reader, writer);
                break;
            case Request.CLEAR_AREA:
                handleClearArea(reader);
                break;
            case Request.QUERY_TREE:
                handleQueryTree(reader, writer);
                break;
        }
    }

    private void handleQueryTree(PacketReader reader, PacketWriter writer) throws WindowError {
        XWindow window = mWindowManager.getWindow(reader.readCard32());
        synchronized (window) {
            writer.writeCard32(XWindowManager.ROOT_WINDOW);
            writer.writeCard32(window.getParent() != null ? window.getParent().getId() : 0);
            int count = window.getChildCountLocked();
            writer.writeCard16(count);
            writer.writePadding(14);
            for (int i = 0; i < count; i++) {
                writer.writeCard32(window.getChildAtLocked(i).getId());
            }
        }
    }

    private void handleMapWindows(PacketReader reader) throws WindowError {
        XWindow window = mWindowManager.getWindow(reader.readCard32());
        synchronized (window) {
            window.requestMap();
        }
    }

    private void handleMapSubwindows(PacketReader reader) throws WindowError {
        XWindow window = mWindowManager.getWindow(reader.readCard32());
        synchronized (window) {
            window.requestSubwindowMap();
        }
    }

    private void handleUnmapWindows(PacketReader reader) throws WindowError {
        XWindow window = mWindowManager.getWindow(reader.readCard32());
        synchronized (window) {
            window.requestUnmap();
        }
    }

    private void handleUnmapSubwindows(PacketReader reader) throws WindowError {
        XWindow window = mWindowManager.getWindow(reader.readCard32());
        synchronized (window) {
            window.requestSubwindowUnmap();
        }
    }

    private void handleDestroyWindows(PacketReader reader) throws WindowError {
        XWindow window = mWindowManager.getWindow(reader.readCard32());
        synchronized (window) {
            window.destroyLocked();
        }
    }

    private void handleDestroySubwindows(PacketReader reader) throws WindowError {
        XWindow window = mWindowManager.getWindow(reader.readCard32());
        synchronized (window) {
            window.requestSubwindowUnmap();
        }
    }

    private void handleChangeWindowAttributes(Client client, PacketReader reader)
            throws XError {
        XWindow window = mWindowManager.getWindow(reader.readCard32());
        synchronized (window) {
            readWindowAttributesLocked(client, window, reader);
        }
    }

    private void handleCreateWindow(final Client client, final PacketReader reader) throws XError {
        byte depth = reader.getMinorOpCode();
        if (depth != 32 && depth != 0) {
            throw new MatchError(depth);
        }
        int windowId = reader.readCard32();
        int parentId = reader.readCard32();
        int x = reader.readCard16();
        int y = reader.readCard16();
        int width = reader.readCard16();
        int height = reader.readCard16();
        int borderWidth = reader.readCard16();
        int windowClass = reader.readCard16();
        int visualId = reader.readCard32();
        if (visualId > 1) {
            throw new MatchError(visualId);
        }
        final XWindow window = mWindowManager.createWindow(windowId, width, height,
                (byte) windowClass, parentId);
        synchronized (window) {
            window.setBorderWidth(borderWidth);
            window.setBounds(new Rect(x, y, x + width, y + height));
            readWindowAttributesLocked(client, window, reader);
        }
        // Eat up the extra bytes so we don't get a warning.
        // This is expected to have some arbitrary padding.
        reader.readPadding(reader.getRemaining());
    }

    private void readWindowAttributesLocked(final Client client, final XWindow window,
            final PacketReader reader) throws XError {
        int value = reader.readCard32();
        synchronized (window) {
            new BitmaskParser(value, 0x4000) {
                @Override
                public void readValue(int mask) throws PixmapError {
                    switch (mask) {
                        case 0x01:
                            int pixmap = reader.readCard32();
                            if (pixmap == XWindow.BACKGROUND_NONE) {
                                window.setBackground(null);
                            } else if (pixmap == XWindow.BACKGROUND_PARENT_RELATIVE) {
                                window.setBackgroundParent();
                            } else {
                                window.setBackground(mWindowManager.getGraphicsManager().getPixmap(
                                        pixmap));
                            }
                            break;
                        case 0x02:
                            int color = reader.readCard32();
                            Log.d("XWindowProtocol", "Window background " + Integer.toHexString(color));
                            window.setBackground(new ColorPaintable(color));
                            break;
                        case 0x04:
                            int borderPixmap = reader.readCard32();
                            if (borderPixmap == XWindow.BORDER_COPY_PARENT) {
                                window.setBorder(window.getParent().getBorder());
                            } else {
                                window.setBorder(mWindowManager.getGraphicsManager().getPixmap(
                                        borderPixmap));
                            }
                            break;
                        case 0x08:
                            window.setBorder(new ColorPaintable(reader.readCard32()));
                            break;
                        case 0x10:
                            window.setBitGravity(reader.readByte());
                            reader.readPadding(3);
                            break;
                        case 0x20:
                            window.setWinGravity(reader.readByte());
                            reader.readPadding(3);
                            break;
                        case 0x40:
                            window.setBacking(reader.readByte());
                            reader.readPadding(3);
                            break;
                        case 0x80:
                            window.setBackingPlanes(reader.readCard32());
                            break;
                        case 0x100:
                            window.setBackingPixels(reader.readCard32());
                            break;
                        case 0x200:
                            window.setOverrideRedirect(reader.readByte() != 0);
                            reader.readPadding(3);
                            break;
                        case 0x400:
                            window.setSaveUnder(reader.readByte() != 0);
                            reader.readPadding(3);
                            break;
                        case 0x800:
                            int eventMask = reader.readCard32();
                            window.setEventMask(client, eventMask);
                            break;
                        case 0x1000:
                            window.setDoNotPropagate(reader.readCard32());
                            break;
                        case 0x2000:
                            int colorMap = reader.readCard32();
                            if (colorMap == XWindow.COLORMAP_COPY_PARENT) {
                                colorMap = window.getParent().getColorMap();
                            }
                            window.setColorMap(colorMap);
                            break;
                        case 0x4000:
                            window.setCursor(reader.readCard32());
                            break;
                    }
                }
            };
        }
    }

    private void handleDeleteProperty(PacketReader reader) throws WindowError, AtomError,
            MatchError {
        XWindow window = mWindowManager.getWindow(reader.readCard32());
        int name = reader.readCard32();
        AtomManager.getInstance().getString(name);
        synchronized (window) {
            window.removePropertyLocked(name);
        }
    }

    private void handleChangeProperty(PacketReader reader) throws WindowError, AtomError,
            MatchError {
        XWindow window = mWindowManager.getWindow(reader.readCard32());
        int name = reader.readCard32();
        AtomManager.getInstance().getString(name);
        byte mode = reader.getMinorOpCode();
        int type = reader.readCard32();
        byte format = reader.readByte();
        reader.readPadding(3);
        int length = reader.readCard32();
        if (format == Property.FORMAT_N_2) {
            length *= 2;
        } else if (format == Property.FORMAT_N_4) {
            length *= 4;
        }
        String value = reader.readPaddedString(length);
        Property property;
        synchronized (window) {
            property = window.getPropertyLocked(name, false);
            if (property == null) {
                property = new Property(0);
                property.value = new byte[0];
                property.typeAtom = type;
                property.format = format;
                window.addPropertyLocked(name, property);
            }
        }
        synchronized (property) {
            if (property.typeAtom != type) {
                throw new MatchError(type);
            }
            if (property.format != format) {
                throw new MatchError(format);
            }
            property.change(mode, value.getBytes());
        }
        synchronized (window) {
            window.notifyPropertyChanged(name);
        }
    }

    private void handleGetWindowAttributes(Client client, PacketReader reader, PacketWriter writer)
            throws XError {
        XWindow window = mWindowManager.getWindow(reader.readCard32());
        synchronized (window) {
            writer.setMinorOpCode(window.getBacking());
            writer.writeCard32(window.getId());
            writer.writeCard16(window.getWindowClass());
            writer.writeByte(window.getBitGravity());
            writer.writeByte(window.getWinGravity());
            writer.writeCard32(window.getBackingPlanes());
            writer.writeCard32(window.getBackingPixels());
            writer.writeByte((byte) (window.isSaveUnder() ? 1 : 0));
            writer.writeByte((byte) (1));
            writer.writeByte((byte) (window.getVisibility() == 0 ? 0
                    : window.getVisibility() > 1 ? 2
                    : 1));
            writer.writeByte((byte) (window.isOverrideRedirect() ? 1 : 0));
            writer.writeCard32(window.getColorMap());
            writer.writeCard32(window.getEventMask());
            writer.writeCard32(window.getEventMask(client));
            writer.writeCard16(window.getDoNotPropogate());
            writer.writePadding(2);
        }
    }

    private void handleGetProperty(PacketReader reader, PacketWriter writer) throws XError {
        // TODO: Use this.
        boolean delete = reader.getMinorOpCode() != 0;

        int windowId = reader.readCard32();
        int atom = reader.readCard32();

        // TODO: Use these.
        int type = reader.readCard32();
        int offset = reader.readCard32();
        int length = reader.readCard32();

        XWindow window = mWindowManager.getWindow(windowId);
        // Get the atom to check validity, but don't hang onto it.
        AtomManager.getInstance().getString(atom);

        Property property;
        synchronized (window) {
            property = window.getPropertyLocked(atom, delete);
        }
        if (property == null) {
            property = new Property(0);
            property.value = new byte[0];
        }
        synchronized (property) {
            writer.setMinorOpCode(property.format);
            writer.writeCard32(property.typeAtom);
            writer.writeCard32(property.value.length);
            writer.writeCard32(getLength(property.value.length, property.format));
            writer.writePadding(12);
            writer.writePaddedString(new String(property.value));
        }
    }

    private void handleConfigureWindow(PacketReader reader) throws WindowError {
        int windowId = reader.readCard32();
        XWindow window = mWindowManager.getWindow(windowId);
        int mask = reader.readCard16();
        reader.readPadding(2);
        synchronized (window) {
            Rect bounds = window.getBounds();
            if ((mask & 0x01) != 0) {
                bounds.left = reader.readCard32();
            }
            if ((mask & 0x02) != 0) {
                bounds.top = reader.readCard32();
            }
            if ((mask & 0x04) != 0) {
                int width = reader.readCard32();
                bounds.right = width + bounds.left;
            }
            if ((mask & 0x08) != 0) {
                int height = reader.readCard32();
                bounds.bottom = height + bounds.top;
            }
            boolean change = window.setBounds(bounds);
            if ((mask & 0x10) != 0) {
                change |= window.setBorderWidth(reader.readCard32());
            }
            if ((mask & 0x40) != 0) {
                int siblingId = 0;
                if ((mask & 0x20) != 0) {
                    siblingId = reader.readCard32();
                }
                int stackMode = reader.readCard32();
                XWindow parent = window.getParent();
                if (siblingId != 0) {
                    XWindow sibling = mWindowManager.getWindow(siblingId);
                    synchronized (parent) {
                        parent.stackWindowLocked(window, sibling, stackMode);
                    }
                } else {
                    synchronized (parent) {
                        parent.stackWindowLocked(window, stackMode);
                    }
                }
                change = true;
            }
            if (change) {
                window.notifyConfigureWindow();
            }
        }
    }

    private void handleClearArea(PacketReader reader) throws WindowError {
        boolean exposures = reader.getMinorOpCode() != 0;
        int windowId = reader.readCard32();
        int x = reader.readCard16();
        int y = reader.readCard16();
        int width = reader.readCard16();
        int height = reader.readCard16();
        XWindow window = mWindowManager.getWindow(windowId);
        synchronized (window) {
            if (width == 0) {
                width = window.getWidth() - x;
            }
            if (height == 0) {
                height = window.getHeight() - y;
            }
            window.clearArea(x, y, width, height);
        }
    }

    private int getLength(int length, byte format) {
        switch (format) {
            case Property.FORMAT_N:
                return length;
            case Property.FORMAT_N_2:
                return length / 2;
            case Property.FORMAT_N_4:
                return length / 4;
        }
        return 0;
    }
}
