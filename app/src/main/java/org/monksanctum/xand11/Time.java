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

package org.monksanctum.xand11;

import android.util.Log;

public class Time implements AutoCloseable {

    public static final boolean DEBUG = XService.PROFILE_DEBUG;
    private static final String TAG = "Time";

    private final String mTag;
    private final long mStart;

    public Time(String tag) {
        mTag = tag;
        mStart = System.currentTimeMillis();
    }

    @Override
    public void close() {
        long length = System.currentTimeMillis() - mStart;
        Log.d(TAG, String.format("%s took %d ms", mTag, length));
    }

    public static Time t(String t) {
        if (!DEBUG) return null;
        return new Time(t);
    }
}
