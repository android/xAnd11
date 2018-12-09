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

import org.monksanctum.xand11.core.XActivityManager
import org.monksanctum.xand11.windows.XWindow.WindowCallback

class RootWindowCallback(private val mActivityManager: XActivityManager) : WindowCallback {

    override fun onContentChanged() {
        // Really don't care, this shouldn't happen.
    }

    override fun windowOrderChanged() {
        // Bring window to front?
    }

    override fun onChildAdded(w: XWindow) {

    }

    override fun onChildRemoved(w: XWindow) {

    }

    override fun onVisibilityChanged() {
        // Hmm... Not sure about this one.
    }

    override fun onChildMappingChanged(child: XWindow) {
        if (child.visibility and XWindow.FLAG_MAPPED.toInt() != 0) {
            mActivityManager.onWindowMapped(child)
        } else {
            mActivityManager.onWindowUnmapped(child)
        }
    }

    override fun onLocationChanged() {
        // Shouldn't be changing, crash for now.
        // TODO: Better handling.
        throw RuntimeException("What?!")
    }

    override fun onSizeChanged() {
        // Shouldn't be changing, crash for now.
        // TODO: Better handling.
        throw RuntimeException("What?!")
    }
}
