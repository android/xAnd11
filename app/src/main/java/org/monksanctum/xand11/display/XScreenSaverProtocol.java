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

package org.monksanctum.xand11.display;

import org.monksanctum.xand11.Client;
import org.monksanctum.xand11.Dispatcher;
import org.monksanctum.xand11.comm.PacketReader;
import org.monksanctum.xand11.comm.PacketWriter;
import org.monksanctum.xand11.comm.Request;
import org.monksanctum.xand11.errors.XError;

public class XScreenSaverProtocol implements Dispatcher.PacketHandler{

    private static final byte[] HANDLED_OPS = new byte[] {
            Request.GET_SCREEN_SAVER,
    };

    @Override
    public byte[] getOpCodes() {
        return HANDLED_OPS;
    }

    @Override
    public void handleRequest(Client client, PacketReader reader, PacketWriter writer)
            throws XError {
        switch (reader.getMajorOpCode()) {
            case Request.GET_SCREEN_SAVER:
                handleGetScreenSaver(reader, writer);
                break;
        }
    }

    private void handleGetScreenSaver(PacketReader reader, PacketWriter writer) {
        writer.writeCard16(0); // Timeout
        writer.writeCard16(0); // interval
        writer.writeByte((byte) 0); // Prefer-blanking
        writer.writeByte((byte) 0); // Allow-exposures
        writer.writePadding(18);
    }
}
