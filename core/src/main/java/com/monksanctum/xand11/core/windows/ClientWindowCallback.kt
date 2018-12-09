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

import org.monksanctum.xand11.comm.Client
import org.monksanctum.xand11.comm.Event
import org.monksanctum.xand11.comm.Event.EventInfo
import org.monksanctum.xand11.windows.XWindow.EventCallback

class ClientWindowCallback(val client: Client) : EventCallback {
    override var mask: Int = 0
        private set

    init {
        mask = 0
    }

    fun setEventMask(mask: Int) {
        this.mask = mask
    }

    override fun onEvent(info: EventInfo) {
        when (info.type) {
            Event.PROPERTY_NOTIFY -> Event.sendPropertyChange(client, info.id, info.arg, info.flag)
            Event.MAP_NOTIFY -> Event.sendMapNotify(client, info.id, info.arg, info.flag)
            Event.UNMAP_NOTIFY -> Event.sendUnmapNotify(client, info.id, info.arg, info.flag)
            Event.EXPOSE -> Event.sendExpose(client, info.id, info.rect!!)
            Event.CONFIGURE_NOTIFY -> Event.sendConfigureWindow(client, info.id, info.arg, info.arg2, info.rect!!,
                    info.arg3, info.flag)
            Event.KEY_PRESS -> Event.sendKeyDown(client, info.id, info.arg, info.arg2, info.arg3, info.arg4)
            Event.KEY_RELEASE -> Event.sendKeyUp(client, info.id, info.arg, info.arg2, info.arg3, info.arg4)
            Event.ENTER_NOTIFY -> Event.sendEnter(client, info.id, info.arg, info.arg2)
            Event.LEAVE_NOTIFY -> Event.sendLeave(client, info.id, info.arg, info.arg2)
            Event.SELECTION_REQUEST -> Event.sendSelectionRequest(client, info.id, info.arg, info.arg2, info.arg3,
                    info.arg4, info.arg5)
            Event.SELECTION_NOTIFY -> Event.sendSelectionNotify(client, info.id, info.arg, info.arg2, info.arg3,
                    info.arg4)
            Event.SELECTION_CLEAR -> Event.sendSelectionClear(client, info.id, info.arg, info.arg2)
            Event.DESTROY_NOTIFY -> Event.sendDestroy(client, info.id, info.arg)
            Event.CREATE_NOTIFY -> Event.sendCreate(client, info.id, info.arg, info.arg2, info.arg3, info.arg4,
                    info.arg5, info.arg6, info.flag)
        }
    }
}
