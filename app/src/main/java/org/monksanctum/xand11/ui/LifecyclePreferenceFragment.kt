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

import androidx.preference.PreferenceFragment
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceScreen

abstract class LifecyclePreferenceFragment : PreferenceFragment() {

    override fun onResume() {
        super.onResume()
        resumePreferences(preferenceScreen)
    }

    private fun resumePreferences(preference: PreferenceGroup?) {
        if (preference == null) {
            return
        }
        for (i in 0 until preference.preferenceCount) {
            val child = preference.getPreference(i)
            if (child is LifecyclePreference) {
                (child as LifecyclePreference).onResume()
            }
            if (child is PreferenceGroup) {
                resumePreferences(child)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        pausePreferences(preferenceScreen)
    }

    private fun pausePreferences(preference: PreferenceGroup?) {
        if (preference == null) {
            return
        }
        for (i in 0 until preference.preferenceCount) {
            val child = preference.getPreference(i)
            if (child is LifecyclePreference) {
                (child as LifecyclePreference).onPause()
            }
            if (child is PreferenceGroup) {
                pausePreferences(child)
            }
        }
    }

    interface LifecyclePreference {
        fun onResume()
        fun onPause()
    }
}
