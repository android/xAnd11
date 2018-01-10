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

import android.util.Log;

import org.monksanctum.xand11.Client;
import org.monksanctum.xand11.ClientManager;
import org.monksanctum.xand11.XService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Manages the {@link ServerSocket} and creates a new {@link ClientListener}
 * for each connection.
 */
public class ServerListener extends Thread {

    private static final String TAG = "ServerListener";
    private static final boolean DEBUG = XService.COMM_DEBUG;

    private final int mPort;
    private final ClientManager mManager;
    private ServerSocket mSocket;
    private boolean mRunning;

    public ServerListener(int port, ClientManager manager) {
        if (DEBUG) Log.d(TAG, "Create: " + port);
        mPort = port;
        mManager = manager;
    }

    @Override
    public void run() {
        if (DEBUG) Log.d(TAG, "run");
        try {
            mSocket = new ServerSocket(mPort);

            while (mRunning) {
                if (DEBUG) Log.d(TAG, "Waiting for connection");
                Socket connection = mSocket.accept();
                mManager.addClient(connection);
            }
        } catch (IOException e) {
            if (DEBUG) Log.e(TAG, "IOException", e);
        }
    }

    public void open() {
        if (DEBUG) Log.d(TAG, "open");
        mRunning = true;
        start();
    }

    public void close() {
        if (DEBUG) Log.d(TAG, "close");
        mRunning = false;
        if (mSocket != null) {
            try {
                mSocket.close();
            } catch (IOException e) {
            }
        }
    }
}
