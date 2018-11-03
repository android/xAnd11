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

import androidx.preference.PreferenceFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

public abstract class LifecyclePreferenceFragment extends PreferenceFragment{

    @Override
    public void onResume() {
        super.onResume();
        resumePreferences(getPreferenceScreen());
    }

    private void resumePreferences(PreferenceGroup preference) {
        if (preference == null) {
            return;
        }
        for (int i = 0; i < preference.getPreferenceCount(); i++) {
            Preference child = preference.getPreference(i);
            if (child instanceof LifecyclePreference) {
                ((LifecyclePreference) child).onResume();
            }
            if (child instanceof PreferenceGroup) {
                resumePreferences((PreferenceGroup) child);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        pausePreferences(getPreferenceScreen());
    }

    private void pausePreferences(PreferenceGroup preference) {
        if (preference == null) {
            return;
        }
        for (int i = 0; i < preference.getPreferenceCount(); i++) {
            Preference child = preference.getPreference(i);
            if (child instanceof LifecyclePreference) {
                ((LifecyclePreference) child).onPause();
            }
            if (child instanceof PreferenceGroup) {
                pausePreferences((PreferenceGroup) child);
            }
        }
    }

    public interface LifecyclePreference {
        void onResume();
        void onPause();
    }
}
