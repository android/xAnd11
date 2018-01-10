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

package org.monksanctum.xand11.windows;

import org.monksanctum.xand11.Client;
import org.monksanctum.xand11.comm.Event;
import org.monksanctum.xand11.comm.Event.EventInfo;
import org.monksanctum.xand11.windows.XWindow.EventCallback;

public class ClientWindowCallback implements EventCallback {

    private final Client mClient;
    private int mMask;

    public ClientWindowCallback(Client client) {
        mClient = client;
        mMask = 0;
    }

    public void setEventMask(int mask) {
        mMask = mask;
    }

    @Override
    public void onEvent(EventInfo info) {
        switch (info.type) {
            case Event.PROPERTY_NOTIFY:
                Event.sendPropertyChange(mClient, info.id, info.arg, info.flag);
                break;
            case Event.MAP_NOTIFY:
                Event.sendMapNotify(mClient, info.id, info.arg, info.flag);
                break;
            case Event.UNMAP_NOTIFY:
                Event.sendUnmapNotify(mClient, info.id, info.arg, info.flag);
                break;
            case Event.EXPOSE:
                Event.sendExpose(mClient, info.id, info.rect);
                break;
            case Event.CONFIGURE_NOTIFY:
                Event.sendConfigureWindow(mClient, info.id, info.arg, info.arg2, info.rect,
                        info.arg3, info.flag);
                break;
            case Event.KEY_PRESS:
                Event.sendKeyDown(mClient, info.id, info.arg, info.arg2, info.arg3, info.arg4);
                break;
            case Event.KEY_RELEASE:
                Event.sendKeyUp(mClient, info.id, info.arg, info.arg2, info.arg3, info.arg4);
                break;
            case Event.ENTER_NOTIFY:
                Event.sendEnter(mClient, info.id, info.arg, info.arg2);
                break;
            case Event.LEAVE_NOTIFY:
                Event.sendLeave(mClient, info.id, info.arg, info.arg2);
                break;
            case Event.SELECTION_REQUEST:
                Event.sendSelectionRequest(mClient, info.id, info.arg, info.arg2, info.arg3,
                        info.arg4, info.arg5);
                break;
            case Event.SELECTION_NOTIFY:
                Event.sendSelectionNotify(mClient, info.id, info.arg, info.arg2, info.arg3,
                        info.arg4);
                break;
            case Event.SELECTION_CLEAR:
                Event.sendSelectionClear(mClient, info.id, info.arg, info.arg2);
                break;
            case Event.DESTROY_NOTIFY:
                Event.sendDestroy(mClient, info.id, info.arg);
                break;
            case Event.CREATE_NOTIFY:
                Event.sendCreate(mClient, info.id, info.arg, info.arg2, info.arg3, info.arg4,
                        info.arg5, info.arg6, info.flag);
                break;
        }
    }

    @Override
    public int getMask() {
        return mMask;
    }

    public Client getClient() {
        return mClient;
    }
}
