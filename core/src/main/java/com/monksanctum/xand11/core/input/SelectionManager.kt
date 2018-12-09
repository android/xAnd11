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

package org.monksanctum.xand11.input


class SelectionManager {

    private val mOwners = mutableMapOf<Int, SelectionInfo>()

    fun getOwner(atom: Int): Int {
        val info = mOwners[atom]
        return info?.owner ?: 0
    }

    fun setSelection(atom: Int, window: Int, timestamp: Int): Boolean {
        var info: SelectionInfo? = mOwners[atom]
        if (info == null) {
            info = SelectionInfo()
            mOwners[atom] = info
        }
        if (timestamp >= info.timestamp) {
            info.owner = window
            info.timestamp = timestamp
            return true
        }
        return false
    }

    private class SelectionInfo {
        var owner: Int = 0
        var timestamp: Int = 0
    }
}
