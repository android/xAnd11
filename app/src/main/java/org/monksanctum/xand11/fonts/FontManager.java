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

package org.monksanctum.xand11.fonts;

import android.util.Log;
import android.util.SparseArray;

import org.monksanctum.xand11.XService;

import java.util.ArrayList;
import java.util.List;

public class FontManager {

    private static final String TAG = "FontManager";

    public static final boolean DEBUG = XService.FONT_DEBUG;

    private final SparseArray<Font> mFonts = new SparseArray<>();
    private final FontSpec[] mDefaultFonts;

    public FontManager() {
        mDefaultFonts = FontSpec.getDefaultSpecs();
    }

    public void openFont(int fid, String name) {
        if (DEBUG) Log.d(TAG, "openFont " + Integer.toHexString(fid) + " " + name);
        Font font = openFont(name);
        synchronized (mFonts) {
            mFonts.put(fid, font);
        }
    }

    private Font openFont(String name) {
        for (int i = 0; i < mDefaultFonts.length; i++) {
            FontSpec fontSpec = mDefaultFonts[i].matches(name);
            if (fontSpec != null) {
                if (DEBUG) Log.d(TAG, "openFont " + fontSpec.toString());
                return new Font(fontSpec, name);
            }
        }
        if (DEBUG) Log.d(TAG, "Unknown font " + name);
        return new Font(new FontSpec(name), name);
    }

    public void closeFont(int fid) {
        if (DEBUG) Log.d(TAG, "closeFont " + Integer.toHexString(fid));
        synchronized (mFonts) {
            mFonts.remove(fid);
        }
    }

    public Font getFont(int fid) {
        synchronized (mFonts) {
            if (DEBUG) Log.d(TAG, "getFont " + Integer.toHexString(fid));
            return mFonts.get(fid);
        }
    }

    public List<Font> getFontsMatching(String pattern, int max) {
        List<Font> fonts = new ArrayList<>();
        for (int i = 0; i < mDefaultFonts.length; i++) {
            FontSpec fontSpec = mDefaultFonts[i].matches(pattern);
            if (fontSpec != null) {
//                if (DEBUG) Log.d(TAG, "Adding " + fontSpec.toString());
                fonts.add(new Font(fontSpec, null));
            }
            if (fonts.size() == max) {
                break;
            }
        }
        if (DEBUG) Log.d(TAG, "getFontsMatching " + pattern + " " + fonts.size());
        return fonts;
    }
}
