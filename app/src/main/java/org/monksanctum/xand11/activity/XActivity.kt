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

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.PersistableBundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.view.Window

import org.monksanctum.xand11.Utils
import org.monksanctum.xand11.XService
import org.monksanctum.xand11.atoms.AtomManager
import org.monksanctum.xand11.errors.WindowError
import org.monksanctum.xand11.windows.PropertyCallback
import org.monksanctum.xand11.windows.XWindow
import org.monksanctum.xand11.windows.XWindow.Property

class XActivity : Activity() {

    private var mService: XService? = null
    private var mWindowId: Int = 0
    private var mRootWindow: XWindow? = null
    private var mResumed: Boolean = false

    private val mPropertyCallback = object : PropertyCallback() {
        override fun onPropertyChanged(prop: Int) {
            if (prop == AtomManager.WM_NAME) {
                window.decorView.post { updateTitle() }
            }
        }
    }

    private val mServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            mService = XService.getServiceFromBinder(service)
            onCreateAndConnected()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            // Shouldn't happen, if it does the process is going down, so we don't care.
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mWindowId = intent.getIntExtra(EXTRA_WINDOW_ID, -1)
        bindService(Intent(this, XService::class.java), mServiceConnection, 0)
    }

    private fun onCreateAndConnected() {
        mService!!.activityManager!!.setTask(mWindowId, taskId)
        try {
            mRootWindow = mService!!.windowManager!!.getWindow(mWindowId).apply {
                addCallback(mPropertyCallback)
                updateTitle()
                val rootWindowView = XRootWindowView(this@XActivity, this)
                rootWindowView.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT,
                        LayoutParams.MATCH_PARENT)
                setContentView(rootWindowView)
                if (DEBUG_WINDOW_HIERARCHY) {
                    rootWindowView.postDelayed({ debugWindow(this, "") }, 3000)
                }
                if (mResumed) {
                    Utils.sBgHandler.post {
                        synchronized(this) {
                            this.notifyEnter()
                        }
                    }
                }
            }
        } catch (windowError: WindowError) {
            // TODO: UI to handle error?
            throw RuntimeException(windowError)
        }

    }

    override fun onResume() {
        super.onResume()
        mResumed = true
        mRootWindow?.let {
            Utils.sBgHandler.post {
                synchronized(it) {
                    mRootWindow!!.notifyEnter()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        mResumed = false
        mRootWindow?.let {
            Utils.sBgHandler.post {
                synchronized(it) {
                    mRootWindow!!.notifyLeave()
                }
            }
        }
    }

    private fun updateTitle() {
        mRootWindow?.let {
            synchronized(it) {
                val prop = mRootWindow!!.getPropertyLocked(AtomManager.WM_NAME, false)
                if (prop != null) {
                    synchronized(prop) {
                        val v = String(prop.value)
                        title = v
                    }
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        mRootWindow?.let {
            Utils.sBgHandler.post {
                synchronized(it) {
                    it.notifyKeyDown(keyCode)
                }
            }
            return true
        }
        return false
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        mRootWindow?.let {
            Utils.sBgHandler.post {
                synchronized(it) {
                    mRootWindow!!.notifyKeyUp(keyCode)
                }
            }
            return true
        }
        return false
    }

    private fun debugWindow(window: XWindow, prefix: String) {
        Log.d(TAG, prefix + " " + window.bounds + " " + window.borderWidth + " "
                + window.border + " " + window.background + " " + window.innerBounds
                + " " + Integer.toHexString(window.visibility))
        synchronized(window) {
            for (i in 0 until window.childCountLocked) {
                debugWindow(window.getChildAtLocked(i), "$prefix -- ")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(mServiceConnection)
        if (mRootWindow != null) {
            mRootWindow!!.removeCallback(mPropertyCallback)
        }
    }

    companion object {

        private val TAG = "XActivity"

        val EXTRA_WINDOW_ID = "windowId"

        private val DEBUG_WINDOW_HIERARCHY = true
    }
}
