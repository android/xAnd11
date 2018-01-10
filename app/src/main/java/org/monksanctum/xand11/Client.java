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

import org.monksanctum.xand11.comm.ClientListener;
import org.monksanctum.xand11.comm.XProtoReader;

import java.net.Socket;

public class Client {

    private final ClientListener mClientListener;
    private final ClientManager mManager;
    private final long mStartTime;

    public Client(Socket socket, ClientManager manager, long startTime) {
        mManager = manager;
        mStartTime = startTime;
        mClientListener = new ClientListener(socket, this);
        mClientListener.startServing();
    }

    public void onDeath() {
        // TODO: Deathy things.
        mManager.removeClient(this);
    }

    public ClientManager getClientManager() {
        return mManager;
    }

    public ClientListener getClientListener() {
        return mClientListener;
    }

    public int getTimestamp() {
        return (int) (System.currentTimeMillis() - mStartTime);
    }
}
