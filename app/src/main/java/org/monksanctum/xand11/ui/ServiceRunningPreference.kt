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

package org.monksanctum.xand11.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.preference.SwitchPreference
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.util.AttributeSet
import org.monksanctum.xand11.core.STATE_CHANGED
import org.monksanctum.xand11.core.isRunning

import org.monksanctum.xand11.XService

class ServiceRunningPreference(context: Context, attrs: AttributeSet) : SwitchPreference(context, attrs), LifecyclePreferenceFragment.LifecyclePreference {

    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            isChecked = isRunning
        }
    }

    init {
        setOnPreferenceChangeListener { preference, newValue ->
            if (newValue as Boolean) {
                getContext().startService(Intent(getContext(), XService::class.java))
            } else {
                getContext().stopService(Intent(getContext(), XService::class.java))
            }
            false
        }
    }

    override fun onResume() {
        LocalBroadcastManager.getInstance(context).registerReceiver(mReceiver,
                IntentFilter(STATE_CHANGED))
        isChecked = isRunning
    }

    override fun onPause() {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(mReceiver)
    }
}
