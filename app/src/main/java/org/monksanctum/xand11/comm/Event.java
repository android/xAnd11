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

package org.monksanctum.xand11.comm;

import android.graphics.Rect;

import org.monksanctum.xand11.Client;
import org.monksanctum.xand11.ObjectPool;
import org.monksanctum.xand11.ObjectPool.Recycleable;
import org.monksanctum.xand11.Utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class Event {
    public static final byte ERROR = 0;
    public static final byte REPLY = 1;
    public static final byte KEY_PRESS = 2;
    public static final byte KEY_RELEASE = 3;
    public static final byte BUTTON_PRESS = 4;
    public static final byte BUTTON_RELEASE = 5;

    public static final byte MOTION_NOTIFY = 6;
    public static final byte ENTER_NOTIFY = 7;
    public static final byte LEAVE_NOTIFY = 8;

    public static final byte FOCUS_IN = 9;
    public static final byte FOCUS_OUT = 10;

    public static final byte KEYMAP_NOTIFY = 11;

    public static final byte EXPOSE = 12;
    public static final byte GRAPHICS_EXPOSE = 13;
    public static final byte NO_EXPOSE = 14;
    public static final byte VISIBILITY_NOTIFY = 15;
    public static final byte CREATE_NOTIFY = 16;
    public static final byte DESTROY_NOTIFY = 17;

    public static final byte UNMAP_NOTIFY = 18;
    public static final byte MAP_NOTIFY = 19;
    public static final byte MAP_REQUEST = 20;
    public static final byte REPARENT_NOTIFY = 21;

    public static final byte CONFIGURE_NOTIFY = 22;
    public static final byte CONFIGURE_REQUEST = 23;
    public static final byte GRAVITY_NOTIFY = 24;
    public static final byte RESIZE_REQUEST = 25;
    public static final byte CIRCULATE_NOTIFY = 26;
    public static final byte CILCULATE_REQUEST = 27;

    public static final byte PROPERTY_NOTIFY = 28;
    public static final byte SELECTION_CLEAR = 29;
    public static final byte SELECTION_REQUEST = 30;
    public static final byte SELECTION_NOTIFY = 31;
    public static final byte COLORMAP_NOTIFY = 32;

    public static final byte CLIENT_MESSAGE = 33;
    public static final byte MAPPING_NOTIFY = 34;

    private static final String[] sNames = new String[256];

    // TODO: Probobly should be per client or something...?
    private static int sEvent = 1;

    public static void sendPropertyChange(Client client, int window, int atom, boolean deleted) {
        PacketWriter writer = new PacketWriter(client.getClientListener().getWriter());

        writer.writeCard32(window);
        writer.writeCard32(atom);
        writer.writeCard32(client.getTimestamp());
        writer.writeByte((byte) (deleted ? 1 : 0));
        writer.writePadding(15);
        client.getClientListener().sendPacket(MAP_NOTIFY, writer);
    }

    public static void sendMapNotify(Client client, int window, int event,
            boolean overrideRedirect) {
        PacketWriter writer = new PacketWriter(client.getClientListener().getWriter());
        writer.writeCard32(event);
        writer.writeCard32(window);
        writer.writeByte((byte) (overrideRedirect ? 1 : 0));
        writer.writePadding(19);
        client.getClientListener().sendPacket(MAP_NOTIFY, writer);
    }

    public static void sendUnmapNotify(Client client, int window, int event,
            boolean fromConfigure) {
        PacketWriter writer = new PacketWriter(client.getClientListener().getWriter());
        writer.writeCard32(event);
        writer.writeCard32(window);
        writer.writeByte((byte) (fromConfigure ? 1 : 0));
        writer.writePadding(19);
        client.getClientListener().sendPacket(UNMAP_NOTIFY, writer);
    }

    public static void sendSelectionRequest(Client client, int timestamp, int owner, int requestor,
            int selection, int target, int property) {
        PacketWriter writer = new PacketWriter(client.getClientListener().getWriter());
        writer.writeCard32(timestamp);
        writer.writeCard32(owner);
        writer.writeCard32(requestor);
        writer.writeCard32(selection);
        writer.writeCard32(target);
        writer.writeCard32(property);
        writer.writePadding(4);
        client.getClientListener().sendPacket(SELECTION_REQUEST, writer);
    }

    public static void sendSelectionNotify(Client client, int timestamp, int requestor,
            int selection, int target, int property) {
        PacketWriter writer = new PacketWriter(client.getClientListener().getWriter());
        writer.writeCard32(timestamp);
        writer.writeCard32(requestor);
        writer.writeCard32(selection);
        writer.writeCard32(target);
        writer.writeCard32(property);
        writer.writePadding(8);
        client.getClientListener().sendPacket(SELECTION_NOTIFY, writer);
    }

    public static void sendSelectionClear(Client client, int timestamp, int owner, int selection) {
        PacketWriter writer = new PacketWriter(client.getClientListener().getWriter());
        writer.writeCard32(timestamp);
        writer.writeCard32(owner);
        writer.writeCard32(selection);
        writer.writePadding(16);
        client.getClientListener().sendPacket(SELECTION_CLEAR, writer);
    }

    public static void sendExpose(final Client client, int window, Rect bounds) {
        final PacketWriter writer = new PacketWriter(client.getClientListener().getWriter());
        writer.writeCard32(window);
        writer.writeCard16(bounds.left);
        writer.writeCard16(bounds.top);
        writer.writeCard16(bounds.width());
        writer.writeCard16(bounds.height());
        writer.writeCard16(0); // What?
        writer.writePadding(14);

        Utils.sBgHandler.post(() -> client.getClientListener().sendPacket(EXPOSE, writer));
    }

    public static void sendConfigureWindow(final Client client, int event, int window, int sibling,
            Rect bounds, int borderWidth, boolean overrideDirect) {
        final PacketWriter writer = new PacketWriter(client.getClientListener().getWriter());
        writer.writeCard32(event);
        writer.writeCard32(window);
        writer.writeCard32(sibling);
        writer.writeCard16(bounds.left);
        writer.writeCard16(bounds.top);
        writer.writeCard16(bounds.width());
        writer.writeCard16(bounds.height());
        writer.writeCard16(borderWidth);
        writer.writeByte((byte) (overrideDirect ? 1 : 0));
        writer.writePadding(5);

        client.getClientListener().sendPacket(CONFIGURE_NOTIFY, writer);
    }

    public static void sendKeyDown(Client client, int id, int x, int y, int keyCode, int state) {
        final PacketWriter writer = new PacketWriter(client.getClientListener().getWriter());
        writer.setMinorOpCode((byte) keyCode);
        writer.writeCard32(client.getTimestamp());
        writer.writeCard32(3);
        writer.writeCard32(id);
        writer.writeCard32(0); // Child
        writer.writeCard16(x); // Root-x
        writer.writeCard16(y); // Root-y
        writer.writeCard16(x);
        writer.writeCard16(y);

        writer.writeCard16(state); // Keybutton state
        writer.writeByte((byte) 1); // Same screen.
        writer.writePadding(1);

        client.getClientListener().sendPacket(KEY_PRESS, writer);
    }

    public static void sendKeyUp(Client client, int id, int x, int y, int keyCode, int state) {
        final PacketWriter writer = new PacketWriter(client.getClientListener().getWriter());
        writer.setMinorOpCode((byte) keyCode);
        writer.writeCard32(client.getTimestamp());
        writer.writeCard32(3);
        writer.writeCard32(id);
        writer.writeCard32(0); // Child
        writer.writeCard16(0); // Root-x
        writer.writeCard16(0); // Root-y
        writer.writeCard16(x);
        writer.writeCard16(y);

        writer.writeCard16(state); // Keybutton state
        writer.writeByte((byte) 1);
        writer.writePadding(1);

        client.getClientListener().sendPacket(KEY_RELEASE, writer);
    }

    public static void sendEnter(Client client, int id, int x, int y) {
        final PacketWriter writer = new PacketWriter(client.getClientListener().getWriter());
        writer.setMinorOpCode((byte) 1);

        writer.writeCard32(client.getTimestamp());
        writer.writeCard32(id);
        writer.writeCard32(id);
        writer.writeCard32(0);

        writer.writeCard16(0); // Root-x
        writer.writeCard16(0); // Root-y
        writer.writeCard16(x);
        writer.writeCard16(y);
        writer.writeCard16(0); // Keybutton state

        writer.writeByte((byte) 0); // Normal, not grab
        writer.writeByte((byte) 3); // Same screen, focus

        client.getClientListener().sendPacket(ENTER_NOTIFY, writer);
    }

    public static void sendLeave(Client client, int id, int x, int y) {
        final PacketWriter writer = new PacketWriter(client.getClientListener().getWriter());
        writer.setMinorOpCode((byte) 1);

        writer.writeCard32(client.getTimestamp());
        writer.writeCard32(id);
        writer.writeCard32(id);
        writer.writeCard32(0);

        writer.writeCard16(0); // Root-x
        writer.writeCard16(0); // Root-y
        writer.writeCard16(x);
        writer.writeCard16(y);
        writer.writeCard16(0); // Keybutton state

        writer.writeByte((byte) 0); // Normal, not grab
        writer.writeByte((byte) 3); // Same screen, focus

        client.getClientListener().sendPacket(LEAVE_NOTIFY, writer);
    }

    public static void sendDestroy(Client client, int id, int arg) {
        final PacketWriter writer = new PacketWriter(client.getClientListener().getWriter());
        writer.writeCard32(id);
        writer.writeCard32(arg);
        writer.writePadding(20);
        client.getClientListener().sendPacket(DESTROY_NOTIFY, writer);
    }

    public static void sendCreate(Client client, int id, int child, int x, int y, int width,
            int height, int borderWidth, boolean overrideRedirect) {
        final PacketWriter writer = new PacketWriter(client.getClientListener().getWriter());

        writer.writeCard32(id);
        writer.writeCard32(child);
        writer.writeCard16(x);
        writer.writeCard16(y);
        writer.writeCard16(width);
        writer.writeCard16(height);
        writer.writeCard16(borderWidth);
        writer.writeByte((byte) (overrideRedirect ? 1 : 0));
        writer.writePadding(9);

        client.getClientListener().sendPacket(CREATE_NOTIFY, writer);
    }

    public static void populateNames() {
        Class<Event> cls = Event.class;
        for (Field field : cls.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType() == byte.class) {
                try {
                    byte index = (byte) field.get(null);
                    sNames[index] = field.getName();
                } catch (IllegalAccessException e) {
                }
            }
        }
    }

    public static String getName(int what) {
        if (sNames[what] == null) {
            return "Unknown(" + what + ")";
        }
        return sNames[what];
    }

    public static EventInfo obtainInfo(byte type, int id, int arg) {
        EventInfo e = sEventPool.obtain();
        e.type = type;
        e.id = id;
        e.arg = arg;
        return e;
    }

    public static EventInfo obtainInfo(byte type, int id, int arg, boolean flag) {
        EventInfo e = sEventPool.obtain();
        e.type = type;
        e.id = id;
        e.arg = arg;
        e.flag = flag;
        return e;
    }

    public static EventInfo obtainInfo(byte type, int id, int arg, int arg2, int arg3, int arg4,
            int arg5, int arg6, boolean flag) {
        EventInfo e = sEventPool.obtain();
        e.type = type;
        e.id = id;
        e.arg = arg;
        e.arg2 = arg2;
        e.arg3 = arg3;
        e.arg4 = arg4;
        e.arg5 = arg5;
        e.arg6 = arg6;
        e.flag = flag;
        return e;
    }

    public static EventInfo obtainInfo(byte type, int id, Rect arg, boolean flag) {
        EventInfo e = sEventPool.obtain();
        e.type = type;
        e.id = id;
        e.rect = arg;
        e.flag = flag;
        return e;
    }

    public static EventInfo obtainInfo(byte type, int id, int arg1, int arg2, int arg3, Rect rect,
            boolean flag) {
        EventInfo e = sEventPool.obtain();
        e.type = type;
        e.id = id;
        e.arg = arg1;
        e.arg2 = arg2;
        e.arg3 = arg3;
        e.rect = rect;
        e.flag = flag;
        return e;
    }

    public static EventInfo obtainInfo(byte type, int id, int arg1, int arg2, int arg3, int arg4,
            int arg5) {
        EventInfo e = sEventPool.obtain();
        e.type = type;
        e.id = id;
        e.arg = arg1;
        e.arg2 = arg2;
        e.arg3 = arg3;
        e.arg4 = arg4;
        e.arg5 = arg5;
        return e;
    }

    public static EventInfo obtainInfo(byte type, int id, int arg1, int arg2) {
        EventInfo e = sEventPool.obtain();
        e.type = type;
        e.id = id;
        e.arg = arg1;
        e.arg2 = arg2;
        return e;
    }

    public static EventInfo obtainInfo(byte type, int id, int arg1, int arg2, int arg3) {
        EventInfo e = sEventPool.obtain();
        e.type = type;
        e.id = id;
        e.arg = arg1;
        e.arg2 = arg2;
        e.arg3 = arg3;
        return e;
    }

    public static EventInfo obtainInfo(byte type, int id, int arg1, int arg2, int arg3, int arg4) {
        EventInfo e = sEventPool.obtain();
        e.type = type;
        e.id = id;
        e.arg = arg1;
        e.arg2 = arg2;
        e.arg3 = arg3;
        e.arg4 = arg4;
        return e;
    }

    private static final ObjectPool<EventInfo, Void> sEventPool = new ObjectPool<EventInfo, Void>() {
        @Override
        protected EventInfo create(Void... arg) {
            return new EventInfo();
        }
    };

    public static class EventInfo extends Recycleable {
        public byte type;
        public int id;
        public int arg;
        public int arg2;
        public int arg3;
        public int arg5;
        public int arg4;
        public int arg6;
        public boolean flag;
        public Rect rect;
    }
}
