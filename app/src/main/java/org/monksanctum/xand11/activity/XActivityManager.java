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

package org.monksanctum.xand11.activity;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.util.SparseArray;

import org.monksanctum.xand11.XService;
import org.monksanctum.xand11.windows.XWindow;

public class XActivityManager {

    private final SparseArray<Integer> mTasks = new SparseArray<>();
    private final XService mService;
    private final ActivityManager mActivityManager;

    public XActivityManager(XService service) {
        mService = service;
        mActivityManager = (ActivityManager) mService.getSystemService(Context.ACTIVITY_SERVICE);
    }

    public void onWindowMapped(XWindow window) {
        Intent intent = new Intent(mService, XActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NEW_DOCUMENT
                | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        intent.putExtra(XActivity.EXTRA_WINDOW_ID, window.getId());
        mService.startActivity(intent);
    }

    public void onWindowUnmapped(XWindow window) {
        // TODO: Handle all this.
    }

    public void setTask(int windowId, int taskId) {
        mTasks.put(windowId, taskId);
    }
}
