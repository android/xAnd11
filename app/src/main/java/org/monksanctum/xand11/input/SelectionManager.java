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

package org.monksanctum.xand11.input;

import android.util.SparseArray;

public class SelectionManager {

    private final SparseArray<SelectionInfo> mOwners = new SparseArray<>();

    public int getOwner(int atom) {
        SelectionInfo info = mOwners.get(atom, null);
        return info != null ? info.owner : 0;
    }

    public boolean setSelection(int atom, int window, int timestamp) {
        SelectionInfo info = mOwners.get(atom, null);
        if (info == null) {
            info = new SelectionInfo();
            mOwners.put(atom, info);
        }
        if (timestamp >= info.timestamp) {
            info.owner = window;
            info.timestamp = timestamp;
            return true;
        }
        return false;
    }

    private static class SelectionInfo {
        public int owner;
        public int timestamp;
    }
}
