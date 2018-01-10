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

package org.monksanctum.xand11.atoms;

import org.monksanctum.xand11.Client;
import org.monksanctum.xand11.Dispatcher;
import org.monksanctum.xand11.comm.PacketReader;
import org.monksanctum.xand11.comm.PacketWriter;
import org.monksanctum.xand11.comm.Request;
import org.monksanctum.xand11.errors.AtomError;
import org.monksanctum.xand11.errors.XError;

public class AtomProtocol implements Dispatcher.PacketHandler {

    private static final byte[] HANDLED_OPS = new byte[] {
            Request.INTERN_ATOM,
            Request.GET_ATOM_NAME,
    };

    private final AtomManager mAtomManager;

    public AtomProtocol() {
        mAtomManager = AtomManager.getInstance();
    }

    @Override
    public byte[] getOpCodes() {
        return HANDLED_OPS;
    }

    @Override
    public void handleRequest(Client client, PacketReader reader, PacketWriter writer)
            throws XError {
        switch (reader.getMajorOpCode()) {
            case Request.INTERN_ATOM:
                handleInternAtom(reader, writer);
                break;
            case Request.GET_ATOM_NAME:
                handleGetAtomName(reader, writer);
                break;
        }
    }

    private void handleInternAtom(PacketReader reader, PacketWriter writer) {
        int length = reader.readCard16();
        reader.readPadding(2);
        String name = reader.readPaddedString(length);
        writer.writeCard32(mAtomManager.internAtom(name));
        writer.writePadding(20);
    }

    private void handleGetAtomName(PacketReader reader, PacketWriter writer) throws AtomError {
        int atom = reader.readCard32();
        String str = mAtomManager.getString(atom);
        writer.writeCard16(str.length());
        writer.writePadding(22);
        writer.writePaddedString(str);
    }
}
