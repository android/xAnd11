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

package org.monksanctum.xand11.windows

import org.monksanctum.xand11.comm.Event.EventInfo
import org.monksanctum.xand11.windows.XWindow.EventCallback

abstract class PropertyCallback : EventCallback {

    override val mask: Int
        get() = XWindow.EVENT_MASK_PROPERTY_CHANGE

    override fun onEvent(info: EventInfo) {
        val prop = info.arg
        onPropertyChanged(prop)
    }

    protected abstract fun onPropertyChanged(prop: Int)
}
