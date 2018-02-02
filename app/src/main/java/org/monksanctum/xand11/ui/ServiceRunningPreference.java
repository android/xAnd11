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

package org.monksanctum.xand11.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v14.preference.SwitchPreference;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.preference.Preference;
import android.util.AttributeSet;

import org.monksanctum.xand11.XService;

public class ServiceRunningPreference extends SwitchPreference implements
        LifecyclePreferenceFragment.LifecyclePreference {

    public ServiceRunningPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnPreferenceChangeListener((preference, newValue) -> {
            if (((Boolean) newValue)) {
                getContext().startService(new Intent(getContext(), XService.class));
            } else {
                getContext().stopService(new Intent(getContext(), XService.class));
            }
            return false;
        });
    }

    @Override
    public void onResume() {
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mReceiver,
                new IntentFilter(XService.STATE_CHANGED));
        setChecked(XService.isRunning());
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mReceiver);
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            setChecked(XService.isRunning());
        }
    };
}
