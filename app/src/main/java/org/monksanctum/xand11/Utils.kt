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

import android.os.Handler
import android.os.HandlerThread

object Utils {

    private val sHandlerThread: HandlerThread
    val sBgHandler: Handler

    init {
        sHandlerThread = HandlerThread("BgWorker")
        sHandlerThread.start()
        sBgHandler = Handler(sHandlerThread.looper)
    }

    fun unsigned(b: Byte): Int {
        var i = b.toInt()
        if (i < 0) {
            i += 256
        }
        return i
    }

    fun toCard16(i: Int): Int {
        return if (i > 0) {
            i
        } else i and 0xffff
        // TODO: Figure out if this gets written out properly.
    }

}
