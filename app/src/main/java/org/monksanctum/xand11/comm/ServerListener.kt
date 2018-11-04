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

import android.util.Log

import org.monksanctum.xand11.Client
import org.monksanctum.xand11.ClientManager
import org.monksanctum.xand11.XService

import java.io.IOException
import java.net.ServerSocket
import java.net.Socket

/**
 * Manages the [ServerSocket] and creates a new [ClientListener]
 * for each connection.
 */
class ServerListener(private val mPort: Int, private val mManager: ClientManager) : Thread() {
    private var mSocket: ServerSocket? = null
    private var mRunning: Boolean = false

    init {
        if (DEBUG) Log.d(TAG, "Create: $mPort")
    }

    override fun run() {
        if (DEBUG) Log.d(TAG, "run")
        try {
            mSocket = ServerSocket(mPort)

            while (mRunning) {
                if (DEBUG) Log.d(TAG, "Waiting for connection")
                val connection = mSocket!!.accept()
                mManager.addClient(connection)
            }
        } catch (e: IOException) {
            if (DEBUG) Log.e(TAG, "IOException", e)
        }

    }

    fun open() {
        if (DEBUG) Log.d(TAG, "open")
        mRunning = true
        start()
    }

    fun close() {
        if (DEBUG) Log.d(TAG, "close")
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
        private val DEBUG = XService.COMM_DEBUG
    }
}
