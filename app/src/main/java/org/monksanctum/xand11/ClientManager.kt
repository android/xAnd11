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

package org.monksanctum.xand11

import org.monksanctum.xand11.comm.AuthManager
import org.monksanctum.xand11.comm.ClientListener

import java.net.Socket
import java.util.ArrayList

/**
 * Keeps track of current open connections to clients.
 */
class ClientManager(val authManager: AuthManager, val dispatcher: Dispatcher) {

    private val mClients = ArrayList<Client>()
    private val mStartTime: Long = System.currentTimeMillis()

    fun addClient(connection: Socket) {
        val client = Client(connection, this, mStartTime)
        mClients.add(client)
    }

    fun removeClient(client: Client) {
        mClients.remove(client)
    }
}
