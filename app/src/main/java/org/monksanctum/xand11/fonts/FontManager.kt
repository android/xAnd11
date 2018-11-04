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

package org.monksanctum.xand11.fonts

import android.util.Log
import android.util.SparseArray

import org.monksanctum.xand11.XService

import java.util.ArrayList

class FontManager {

    private val mFonts = SparseArray<Font>()
    private val mDefaultFonts: Array<FontSpec>

    init {
        mDefaultFonts = FontSpec.defaultSpecs
    }

    fun openFont(fid: Int, name: String) {
        if (DEBUG) Log.d(TAG, "openFont " + Integer.toHexString(fid) + " " + name)
        val font = openFont(name)
        synchronized(mFonts) {
            mFonts.put(fid, font)
        }
    }

    private fun openFont(name: String): Font {
        for (i in mDefaultFonts.indices) {
            val fontSpec = mDefaultFonts[i].matches(name)
            if (fontSpec != null) {
                if (DEBUG) Log.d(TAG, "openFont " + fontSpec.toString())
                return Font(fontSpec, name)
            }
        }
        if (DEBUG) Log.d(TAG, "Unknown font $name")
        return Font(FontSpec(name), name)
    }

    fun closeFont(fid: Int) {
        if (DEBUG) Log.d(TAG, "closeFont " + Integer.toHexString(fid))
        synchronized(mFonts) {
            mFonts.remove(fid)
        }
    }

    fun getFont(fid: Int): Font {
        synchronized(mFonts) {
            if (DEBUG) Log.d(TAG, "getFont " + Integer.toHexString(fid))
            return mFonts.get(fid)
        }
    }

    fun getFontsMatching(pattern: String, max: Int): List<Font> {
        val fonts = ArrayList<Font>()
        for (i in mDefaultFonts.indices) {
            val fontSpec = mDefaultFonts[i].matches(pattern)
            if (fontSpec != null) {
                //                if (DEBUG) Log.d(TAG, "Adding " + fontSpec.toString());
                fonts.add(Font(fontSpec, null))
            }
            if (fonts.size == max) {
                break
            }
        }
        if (DEBUG) Log.d(TAG, "getFontsMatching " + pattern + " " + fonts.size)
        return fonts
    }

    companion object {

        private val TAG = "FontManager"

        val DEBUG = XService.FONT_DEBUG
    }
}
