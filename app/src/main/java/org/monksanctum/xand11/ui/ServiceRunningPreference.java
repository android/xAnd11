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

import android.content.Context;
import android.content.Intent;
import android.support.v14.preference.SwitchPreference;
import android.util.AttributeSet;

import org.monksanctum.xand11.XService;

public class ServiceRunningPreference extends SwitchPreference implements
        LifecyclePreferenceFragment.LifecyclePreference{

    public ServiceRunningPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected boolean persistBoolean(boolean value) {
        if (value) {
            getContext().startService(new Intent(getContext(), XService.class));
        } else {
            getContext().stopService(new Intent(getContext(), XService.class));
        }
        return super.persistBoolean(value);
    }

    @Override
    public void onResume() {
        setChecked(XService.isRunning());
    }

    @Override
    public void onPause() {

    }
}
