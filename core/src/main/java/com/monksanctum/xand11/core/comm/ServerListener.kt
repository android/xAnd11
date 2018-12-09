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

package org.monksanctum.xand11.comm

import org.monksanctum.xand11.core.COMM_DEBUG
import org.monksanctum.xand11.core.IOException
import org.monksanctum.xand11.core.Platform
import org.monksanctum.xand11.core.ServerSocket


/**
 * Manages the [ServerSocket] and creates a new [ClientListener]
 * for each connection.
 */
class ServerListener(private val mPort: Int, private val mManager: ClientManager) {
    private var mSocket: ServerSocket? = null
    private var mRunning: Boolean = false

    init {
        if (DEBUG) Platform.logd(TAG, "Create: $mPort")
    }

    public fun run() {
        if (DEBUG) Platform.logd(TAG, "run")
        try {
            mSocket = ServerSocket(mPort)

            while (mRunning) {
                if (DEBUG) Platform.logd(TAG, "Waiting for connection")
                val connection = mSocket!!.accept()
                mManager.addClient(connection)
            }
        } catch (e: IOException) {
            if (DEBUG) Platform.loge(TAG, "IOException", e)
        }

    }

    fun open() {
        if (DEBUG) Platform.logd(TAG, "open")
        mRunning = true
        Platform.startThread(this::run)
    }

    fun close() {
        if (DEBUG) Platform.logd(TAG, "close")
        mRunning = false
        if (mSocket != null) {
            try {
                mSocket!!.close()
            } catch (e: IOException) {
            }

        }
    }

    companion object {

        private val TAG = "ServerListener"
        private val DEBUG = COMM_DEBUG
    }
}
