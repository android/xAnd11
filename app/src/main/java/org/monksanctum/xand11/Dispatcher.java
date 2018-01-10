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

package org.monksanctum.xand11;

import android.util.Log;

import org.monksanctum.xand11.comm.PacketReader;
import org.monksanctum.xand11.comm.PacketWriter;
import org.monksanctum.xand11.comm.XProtoWriter.WriteException;
import org.monksanctum.xand11.errors.XError;

import static org.monksanctum.xand11.Utils.unsigned;

/**
 * Connects up the input with all of the things that want to receive stuffs from the input.
 */
public class Dispatcher {

    private static final String TAG = "Dispatcher";

    private final PacketHandler[] mHandlers = new PacketHandler[256];

    public void addPacketHandler(PacketHandler handler) {
        for (byte b : handler.getOpCodes()) {
            mHandlers[unsigned(b)] = handler;
        }
    }

    public PacketHandler getHandler(int opCode) {
        return mHandlers[opCode];
    }

    public interface PacketHandler {
        byte[] getOpCodes();
        void handleRequest(Client client, PacketReader reader, PacketWriter writer) throws XError;
    }
}
