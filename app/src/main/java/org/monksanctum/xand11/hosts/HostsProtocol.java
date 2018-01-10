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

package org.monksanctum.xand11.hosts;

import org.monksanctum.xand11.Client;
import org.monksanctum.xand11.Dispatcher;
import org.monksanctum.xand11.comm.PacketReader;
import org.monksanctum.xand11.comm.PacketWriter;
import org.monksanctum.xand11.comm.Request;
import org.monksanctum.xand11.comm.XProtoReader;
import org.monksanctum.xand11.comm.XProtoReader.ReadException;
import org.monksanctum.xand11.comm.XProtoWriter;
import org.monksanctum.xand11.comm.XProtoWriter.WriteException;
import org.monksanctum.xand11.errors.XError;

import java.util.List;

public class HostsProtocol implements Dispatcher.PacketHandler {

    private static final byte[] HANDLED_OPS = new byte[] {
            Request.LIST_HOSTS,
            Request.CHANGE_HOSTS,
    };

    private final HostsManager mHostManager;

    public HostsProtocol(HostsManager manager) {
        mHostManager = manager;
    }

    @Override
    public byte[] getOpCodes() {
        return HANDLED_OPS;
    }

    private void handleListHosts(PacketReader reader, PacketWriter writer) {
        List<Host> hosts = mHostManager.getHosts();
        final int N = hosts.size();
        writer.writeCard16(hosts.size());
        writer.writePadding(22);
        for (int i = 0; i < N; i++) {
            try {
                writeHost(hosts.get(i), writer);
            } catch (WriteException e) {
                // Writing to PacketWriter, this can't happen.
            }
        }
    }

    private void handleChangeHosts(PacketReader reader, PacketWriter writer) {
        boolean delete = reader.getMinorOpCode() != 0;
        try {
            Host host = readHost(reader);
            if (delete) {
                mHostManager.remHost(host);
            } else {
                mHostManager.addHost(host);
            }
        } catch (ReadException e) {
            // Can't happen from PacketReader.
        }
    }

    @Override
    public void handleRequest(Client client, PacketReader reader, PacketWriter writer)
            throws XError {
        switch (reader.getMajorOpCode()) {
            case Request.LIST_HOSTS:
                handleListHosts(reader, writer);
                break;
            case Request.CHANGE_HOSTS:
                handleChangeHosts(reader, writer);
        }
    }

    public static void writeHost(Host host, XProtoWriter writer)
            throws XProtoWriter.WriteException {
        writer.writeByte(host.type);
        writer.writePadding(1);
        writer.writeCard16(host.address.length);
        writer.writePaddedString(new String(host.address));
    }

    public static Host readHost(XProtoReader reader) throws XProtoReader.ReadException {
        Host host = new Host();
        host.type = reader.readByte();
        reader.readPadding(1);
        String bytes = reader.readPaddedString(reader.readCard16());
        host.address = bytes.getBytes();
        return host;
    }
}
