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

package org.monksanctum.xand11.hosts

class Host {

    var type: Byte = 0
    var address: ByteArray? = null

    override fun equals(o: Any?): Boolean {
        if (o !is Host) {
            return false
        }
        val other = o as Host?
        if (other!!.type != type) {
            return false
        }
        if (other.address!!.size != address!!.size) {
            return false
        }
        for (i in address!!.indices) {
            if (address!![i] != other.address!![i]) {
                return false
            }
        }
        return true
    }

    companion object {

        val TYPE_INTERNET = 0
        val TYPE_DECNET = 1
        val TYPE_CHAOS = 2
    }
}
