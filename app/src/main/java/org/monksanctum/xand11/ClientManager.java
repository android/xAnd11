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

import org.monksanctum.xand11.comm.AuthManager;
import org.monksanctum.xand11.comm.ClientListener;

import java.net.Socket;
import java.util.ArrayList;

/**
 * Keeps track of current open connections to clients.
 */
public class ClientManager {

    private final ArrayList<Client> mClients = new ArrayList<>();
    private final AuthManager mAuthManager;
    private final Dispatcher mDispatcher;
    private final long mStartTime;

    public ClientManager(AuthManager authManager, Dispatcher dispatcher) {
        mAuthManager = authManager;
        mDispatcher = dispatcher;
        mStartTime = System.currentTimeMillis();
    }

    public AuthManager getAuthManager() {
        return mAuthManager;
    }

    public void addClient(Socket connection) {
        Client client = new Client(connection, this, mStartTime);
        mClients.add(client);
    }

    public void removeClient(Client client) {
        mClients.remove(client);
    }

    public Dispatcher getDispatcher() {
        return mDispatcher;
    }
}
