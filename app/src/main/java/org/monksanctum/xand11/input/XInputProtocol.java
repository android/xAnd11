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

package org.monksanctum.xand11.input;

import org.monksanctum.xand11.Client;
import org.monksanctum.xand11.Dispatcher;
import org.monksanctum.xand11.comm.PacketReader;
import org.monksanctum.xand11.comm.PacketWriter;
import org.monksanctum.xand11.comm.Request;
import org.monksanctum.xand11.errors.WindowError;
import org.monksanctum.xand11.errors.XError;
import org.monksanctum.xand11.windows.XWindow;
import org.monksanctum.xand11.windows.XWindowManager;

public class XInputProtocol implements Dispatcher.PacketHandler {

    private static final byte[] HANDLED_OPS = new byte[] {
            Request.GET_INPUT_FOCUS,
            Request.GET_KEYBOARD_MAPPING,
            Request.GET_MODIFIER_MAPPING,
            Request.GET_KEYBOARD_CONTROL,
            Request.GET_SELECTION_OWNER,
            Request.SET_SELECTION_OWNER,
            Request.CONVERT_SELECTION,
            Request.QUERY_POINTER,
            Request.GET_POINTER_CONTROL,
    };

    private final XInputManager mManager;
    private final XWindowManager mWindowManager;

    public XInputProtocol(XInputManager manager, XWindowManager windowManager) {
        mManager = manager;
        mWindowManager = windowManager;
    }

    @Override
    public byte[] getOpCodes() {
        return HANDLED_OPS;
    }

    @Override
    public void handleRequest(Client client, PacketReader reader, PacketWriter writer)
            throws XError {
        switch (reader.getMajorOpCode()) {
            case Request.GET_INPUT_FOCUS:
                handleGetInputFocus(reader, writer);
                break;
            case Request.GET_KEYBOARD_MAPPING:
                handleGetKeyboardMapping(reader, writer);
                break;
            case Request.GET_MODIFIER_MAPPING:
                handleGetModifierMapping(reader, writer);
                break;
            case Request.GET_KEYBOARD_CONTROL:
                handleGetKeyboardControl(reader, writer);
                break;
            case Request.GET_SELECTION_OWNER:
                handleGetSelectionOwner(reader, writer);
                break;
            case Request.SET_SELECTION_OWNER:
                handleSetSelectionOwner(client, reader, writer);
                break;
            case Request.CONVERT_SELECTION:
                handleConvertSelectionOwner(client, reader, writer);
                break;
            case Request.QUERY_POINTER:
                handleQueryPointer(reader, writer);
                break;
            case Request.GET_POINTER_CONTROL:
                handleGetPointerControl(reader, writer);
                break;
        }
    }

    private void handleConvertSelectionOwner(Client client, PacketReader reader,
            PacketWriter writer) throws WindowError {
        int requestor = reader.readCard32();
        int selection = reader.readCard32();
        int target = reader.readCard32();
        int prop = reader.readCard32();
        int timestamp = reader.readCard32();

        int owner = mManager.getSelection().getOwner(selection);
        if (owner != 0) {
            // Send SelectionRequest
            XWindow window = mWindowManager.getWindow(owner);
            window.sendSelectionRequest(timestamp, owner, requestor, selection, target, prop);
        } else {
            // Send SelectionNotify
            XWindow window = mWindowManager.getWindow(requestor);
            if (timestamp == 0) timestamp = client.getTimestamp();
            requestor = 0;
            window.sendSelectionNotify(timestamp, requestor, selection, target, prop);
        }
    }

    private void handleSetSelectionOwner(Client client, PacketReader reader, PacketWriter writer)
            throws WindowError {
        int owner = reader.readCard32();
        int atom = reader.readCard32();
        int timestamp = reader.readCard32();
        if (timestamp == 0) timestamp = client.getTimestamp();
        int currentOwner = mManager.getSelection().getOwner(atom);
        if (currentOwner == owner) {
            return;
        }
        if (mManager.getSelection().setSelection(atom, owner, timestamp)) {
            if (currentOwner != 0) {
                XWindow window = mWindowManager.getWindow(currentOwner);
                window.sendSelectionClear(timestamp, owner, atom);
            }
        }
    }

    private void handleGetSelectionOwner(PacketReader reader, PacketWriter writer) {
        int atom = reader.readCard32();
        int window = mManager.getSelection().getOwner(atom);
        writer.writeCard32(window);
        writer.writePadding(20);
    }

    private void handleGetInputFocus(PacketReader reader, PacketWriter writer) {
        FocusState focus = mManager.getFocusState();
        writer.setMinorOpCode(focus.revert);
        writer.writeCard32(focus.current);
        writer.writePadding(20);
    }

    private void handleGetKeyboardMapping(PacketReader reader, PacketWriter writer) {
        reader.readPadding(4);
        // TODO: Implement.
        writer.setMinorOpCode((byte) mManager.getKeyboardManager().getKeysPerSym());
        writer.writePadding(24);
        int[] keyboardMap = mManager.getKeyboardManager().getKeyboardMap();
        for (int i = 0; i < keyboardMap.length; i++) {
            writer.writeCard32(keyboardMap[i]);
        }
    }

    private void handleGetModifierMapping(PacketReader reader, PacketWriter writer) {
        // TODO: Implement.
        writer.setMinorOpCode((byte) 2);
        writer.writePadding(24);
        byte[] modifiers = mManager.getKeyboardManager().getModifiers();
        for (int i = 0; i < modifiers.length; i++) {
            if (modifiers[i] != 0) {
                writer.writeByte((byte) mManager.translate(modifiers[i]));
            } else {
                writer.writeByte((byte) 0);
            }
        }
    }

    private void handleGetKeyboardControl(PacketReader reader, PacketWriter writer) {
        writer.setMinorOpCode((byte) 1);
        writer.writeCard32(0); // LED Mask.
        writer.writeByte((byte) 0);
        writer.writeByte((byte) 50);
        writer.writeCard16(400);
        writer.writeCard16(100);
        writer.writePadding(2);
        writer.writePadding(32);
    }

    private void handleQueryPointer(PacketReader reader, PacketWriter writer) {
        writer.setMinorOpCode((byte) 1);
        writer.writeCard32(3); // Root window
        writer.writeCard32(0); // No child
        // Location, 0
        writer.writeCard16(0); // Global X
        writer.writeCard16(0); // Global Y
        writer.writeCard16(0); // Local X
        writer.writeCard16(0); // Local Y

        writer.writeCard16(0); // Button mask
        writer.writePadding(6);
    }

    private void handleGetPointerControl(PacketReader reader, PacketWriter writer) {
        writer.writeCard16(0); // Acceleration-numerator
        writer.writeCard16(0); // Acceleration-denominator
        writer.writeCard16(0); // threshold
        writer.writePadding(18);
    }
}
