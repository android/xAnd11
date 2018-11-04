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

import android.util.Log

val DEBUG = XService.PROFILE_DEBUG
val TAG = "Time"

inline fun t(t: String, method: () -> Unit) {
    val start = if (DEBUG) System.currentTimeMillis() else 0
    method.invoke()
    if (DEBUG) {
        val length = System.currentTimeMillis() - start
        Log.d(TAG, String.format("%s took %d ms", t, length))
    }
}
