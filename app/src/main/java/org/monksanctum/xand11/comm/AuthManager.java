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

import android.text.TextUtils;
import android.util.Log;

import org.monksanctum.xand11.XServerInfo;
import org.monksanctum.xand11.XService;
import org.monksanctum.xand11.hosts.HostsManager;

import java.net.Socket;

/**
 * Handles the auth for the server.
 */
public class AuthManager {

    private static final String TAG = "AuthManager";
    private static final boolean DEBUG = XService.COMM_DEBUG;

    private static final boolean CARE_ABOUT_AUTH = false;

    public static final byte CONN_FAILED = 0;
    public static final byte CONN_SUCCEEDED = 1;
    public static final byte CONN_AUTH = 2;

    private final XServerInfo mInfo;
    // TODO: Actually use hosts.
    private final HostsManager mHosts;

    public AuthManager(XServerInfo info, HostsManager hostsManager) {
        mInfo = info;
        mHosts = hostsManager;
    }

    public boolean handleAuth(XProtoReader reader, XProtoWriter writer, Socket connection)
            throws XProtoReader.ReadException, XProtoWriter.WriteException {
        int majorVersion = reader.readCard16();
        int minorVersion = reader.readCard16();
        int n = reader.readCard16();
        int d = reader.readCard16();
        reader.readPadding(2);
        String authProtoName = reader.readPaddedString(n);
        String authProtoData = reader.readPaddedString(d);
        String host = connection.getInetAddress().getHostAddress();

        if (DEBUG) Log.d(TAG, "Incoming connection, host: " + host
                + " protocol: " + majorVersion + "," + minorVersion
                + " auth: " + authProtoName + " auth data: " + authProtoData);

        if (!CARE_ABOUT_AUTH || TextUtils.isEmpty(authProtoName)) {
            acceptConnection(writer);
            return true;
        } else {
            rejectConnection(writer, "Unknown auth " + authProtoName);
            return false;
        }
    }

    private void acceptConnection(XProtoWriter writer) throws XProtoWriter.WriteException {
        if (DEBUG) Log.d(TAG, "Writing accept");
        writer.writeByte(CONN_SUCCEEDED);
        writer.writePadding(1);
        writer.writeCard16(XService.MAJOR_VERSION);
        writer.writeCard16(XService.MINOR_VERSION);
        PacketWriter packet = new PacketWriter(writer);
        mInfo.write(packet);
        if (DEBUG) Log.d(TAG, "Packet length: " + packet.get4Length());
        writer.writeCard16(packet.get4Length());
        packet.flush();
    }

    private void rejectConnection(XProtoWriter writer, String reason)
            throws XProtoWriter.WriteException {
        if (DEBUG) Log.d(TAG, "Writing reject");
        writer.writeByte(CONN_FAILED);
        writer.writeByte((byte) reason.length());
        writer.writeCard16(XService.MAJOR_VERSION);
        writer.writeCard16(XService.MINOR_VERSION);
        writer.writeCard16((reason.length() + 3) / 4);
        writer.writePaddedString(reason);
    }
}
