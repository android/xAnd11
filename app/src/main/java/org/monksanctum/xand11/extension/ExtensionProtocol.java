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

package org.monksanctum.xand11.extension;

import org.monksanctum.xand11.Client;
import org.monksanctum.xand11.Dispatcher;
import org.monksanctum.xand11.comm.PacketReader;
import org.monksanctum.xand11.comm.PacketWriter;
import org.monksanctum.xand11.comm.Request;
import org.monksanctum.xand11.comm.XProtoWriter.WriteException;

import java.util.Set;

public class ExtensionProtocol implements Dispatcher.PacketHandler {

    private static final byte[] HANDLED_OPS = new byte[] {
            Request.QUERY_EXTENSION,
            Request.LIST_EXTENSIONS,
    };

    private ExtensionManager mExtensionManager;

    public ExtensionProtocol(ExtensionManager manager) {
        mExtensionManager = manager;
    }

    @Override
    public byte[] getOpCodes() {
        return HANDLED_OPS;
    }

    @Override
    public void handleRequest(Client client, PacketReader reader, PacketWriter writer) {
        switch (reader.getMajorOpCode()) {
            case Request.QUERY_EXTENSION:
                handleQueryExtension(reader, writer);
                break;
            case Request.LIST_EXTENSIONS:
                handleListExtension(reader, writer);
                break;
        }
    }

    private void handleQueryExtension(PacketReader packet, PacketWriter writer) {
        int n = packet.readCard16();
        packet.readPadding(2);
        String name = packet.readPaddedString(n);
        ExtensionManager.ExtensionInfo info = mExtensionManager.queryExtension(name);
        try {
            info.write(writer);
        } catch (WriteException e) {
        }
    }

    private void handleListExtension(PacketReader packet, PacketWriter writer) {
        int count = 0;
        Set<String> exts = mExtensionManager.getExtensions();
        writer.setMinorOpCode((byte) exts.size());
        writer.writePadding(24);
        for (String name : exts) {
            writer.writeString(name);
            writer.writeByte((byte) 0);
            count += name.length() + 1;
        }
        while ((count++ % 4) != 0) {
            writer.writePadding(1);
        }
    }
}
