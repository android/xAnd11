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

import org.monksanctum.xand11.activity.XActivityManager;
import org.monksanctum.xand11.windows.XWindow.WindowCallback;

public class RootWindowCallback implements WindowCallback {

    private final XActivityManager mActivityManager;

    public RootWindowCallback(XActivityManager activityManager) {
        mActivityManager = activityManager;
    }

    @Override
    public void onContentChanged() {
        // Really don't care, this shouldn't happen.
    }

    @Override
    public void windowOrderChanged() {
        // Bring window to front?
    }

    @Override
    public void onChildAdded(XWindow w) {

    }

    @Override
    public void onChildRemoved(XWindow w) {

    }

    @Override
    public void onVisibilityChanged() {
        // Hmm... Not sure about this one.
    }

    @Override
    public void onChildMappingChanged(XWindow child) {
        if ((child.getVisibility() & XWindow.FLAG_MAPPED) != 0) {
            mActivityManager.onWindowMapped(child);
        } else {
            mActivityManager.onWindowUnmapped(child);
        }
    }

    @Override
    public void onLocationChanged() {
        // Shouldn't be changing, crash for now.
        // TODO: Better handling.
        throw new RuntimeException("What?!");
    }

    @Override
    public void onSizeChanged() {
        // Shouldn't be changing, crash for now.
        // TODO: Better handling.
        throw new RuntimeException("What?!");
    }
}
