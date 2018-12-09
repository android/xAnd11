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

package org.monksanctum.xand11.activity

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.SparseArray

import org.monksanctum.xand11.windows.XWindow

class XActivityManager(private val mService: Context) {

    private val mTasks = SparseArray<Int>()
    private val mActivityManager: ActivityManager

    init {
        mActivityManager = mService.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    }

    fun onWindowMapped(child: XWindow) {
        val intent = Intent().setComponent(
                ComponentName(mService.packageName, "org.monksanctum.xand11.activity.XActivity"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NEW_DOCUMENT
                or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        intent.putExtra(EXTRA_WINDOW_ID, child.id)
        mService.startActivity(intent)
    }

    fun onWindowUnmapped(child: XWindow) {
        // TODO: Handle all this.
    }

    fun setTask(windowId: Int, taskId: Int) {
        mTasks.put(windowId, taskId)
    }

    companion object {
        val EXTRA_WINDOW_ID = "windowId"
    }
}
