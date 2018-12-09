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


import org.monksanctum.xand11.core.Font
import org.monksanctum.xand11.core.Platform

import kotlin.math.round

class FontSpec {

    private val mSpecs = Array<String?>(NUM_FIELDS) { null }

    private val FIXED_LOOKUPS = arrayOf("5x7", "-*-fixed-medium-r-normal--7-70-75-75-c-50-ISO10646-1", "5x8", "-*-fixed-medium-r-normal--8-80-75-75-c-50-ISO10646-1", "6x9", "-*-fixed-medium-r-normal--9-90-75-75-c-60-ISO10646-1", "6x10", "-*-fixed-medium-r-normal--10-100-75-75-c-60-ISO10646-1", "7x13", "-*-fixed-medium-r-normal--13-120-75-75-c-70-ISO10646-1", "7x13B", "-*-fixed-bold-r-normal--13-120-75-75-c-70-ISO10646-1", "7x14", "-*-fixed-medium-r-normal--14-130-75-75-c-70-ISO10646-1", "7x14B", "-*-fixed-bold-r-normal--14-130-75-75-c-70-ISO10646-1", "8x13", "-*-fixed-medium-r-normal--13-120-75-75-c-80-ISO10646-1", "8x13B", "-*-fixed-bold-r-normal--13-120-75-75-c-80-ISO10646-1", "9x15", "-*-fixed-medium-r-normal--15-140-75-75-c-90-ISO10646-1", "9x15B", "-*-fixed-bold-r-normal--15-140-75-75-c-90-ISO10646-1", "10x20", "-*-fixed-medium-r-normal--20-200-75-75-c-100-ISO10646-1")// TODO: Support these.
    //            "6x12", "-*-fixed-medium-r-Semicondensed--12-110-75-75-c-60-ISO10646-1",
    //            "6x13", "-*-fixed-medium-r-SemiCondensed--13-120-75-75-c-60-ISO10646-1",
    //            "6x13B", "-*-fixed-bold-r-SemiCondensed--13-120-75-75-c-60-ISO10646-1",


    constructor(spec: String?) {
        var spec = spec
        spec = checkFixed(spec)
        val fields = spec!!.split("-".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
        if (spec == Font.DEFAULT || spec == Font.FIXED) {
            mSpecs[0] = spec
            return
        }
        if (fields.size != NUM_FIELDS) {
            Platform.logd("FontSpec", "Invalid spec $spec")
            mSpecs[0] = Font.DEFAULT
            return
        }
        for (i in 0 until NUM_FIELDS) {
            mSpecs[i] = fields[i]
        }
    }

    internal constructor(specs: Array<String?>) {
        for (i in 0 until NUM_FIELDS) {
            mSpecs[i] = specs[i]
        }
    }

    private fun checkFixed(spec: String?): String? {
        var i = 0
        while (i < FIXED_LOOKUPS.size) {
            if (FIXED_LOOKUPS[i] == spec) {
                return FIXED_LOOKUPS[i + 1]
            }
            i++
            i++
        }
        return spec
    }

    // TODO: Handle ?s
    fun matches(match: String): FontSpec? {
        val fields : Array<String> = match.split("-".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
        val ret = FontSpec(mSpecs)
        if (fields.size == NUM_FIELDS) {
            for (i in 0 until NUM_FIELDS) {
                if (NONE == ret.mSpecs[i] && WILD != fields[i]) {
                    ret.mSpecs[i] = fields[i]
                }
            }
        }
        val offset = mSpecs.size - fields.size
        for (i in fields.indices) {
            if (fields[i] == WILD) {
                continue
            }
            if (!fields[i].equals(mSpecs[i + offset], ignoreCase = true) && !NONE.equals(mSpecs[i + offset], ignoreCase = true)) {
                return null
            }
        }
        return ret
    }

    fun getSpec(index: Int): String? {
        return mSpecs[index]
    }

    override fun toString(): String {
        if ((NONE == mSpecs[PIXEL_SIZE]) xor (NONE == mSpecs[POINT_SIZE])) {
            // TODO: This better...
            if (NONE == mSpecs[RESOLUTION_X]) {
                mSpecs[RESOLUTION_X] = sDpi.toString()
            }
            if (NONE == mSpecs[RESOLUTION_Y]) {
                mSpecs[RESOLUTION_Y] = sDpi.toString()
            }
            if (NONE == mSpecs[PIXEL_SIZE]) {
                mSpecs[PIXEL_SIZE] = round(mSpecs[POINT_SIZE]!!.toInt() * sDpi / 722.7).toString()
            } else {
                mSpecs[POINT_SIZE] = round(mSpecs[PIXEL_SIZE]!!.toInt() * 722.7 / sDpi).toString()
            }
        }
        return Platform.join("-", mSpecs.toList())
    }

    companion object {

        private val PRE_FIELD = 0
        val FOUNDRY = 1
        val FAMILY_NAME = 2
        val WEIGHT_NAME = 3
        val SLANT = 4
        val SET_WIDTH_NAME = 5
        val ADD_STYLE = 6
        val PIXEL_SIZE = 7
        val POINT_SIZE = 8
        val RESOLUTION_X = 9
        val RESOLUTION_Y = 10
        val SPACING = 11
        val AVERAGE_WIDTH = 12
        val CHARSET_REGISTRY = 13
        val CHARSET_ENCODING = 14
        val NUM_FIELDS = 15

        val FOUNDRY_DEFAULT = "android"

        val FAMILY_DEFAULT = "default"
        val FAMILY_MONOSPACE = "monospace"
        val FAMILY_SERIF = "serif"
        val FAMILY_SANS_SERIF = "sans serif"
        private val FAMILIES = arrayOf(FAMILY_DEFAULT, FAMILY_MONOSPACE, FAMILY_SERIF, FAMILY_SANS_SERIF)

        val WEIGHT_MEDIUM = "medium"
        val WEIGHT_BOLD = "bold"
        private val WEIGHTS = arrayOf(WEIGHT_MEDIUM, WEIGHT_BOLD)

        val SLANT_REGULAR = "r"
        val SLANT_ITALICS = "i"
        private val SLANTS = arrayOf(SLANT_REGULAR, SLANT_ITALICS)

        val SET_WIDTH_DEFAULT = "normal"

        val SPACING_PROPORTIONAL = "p"
        val SPACING_CHARACTER = "c"
        private val FAMILIES_SPACINGS = arrayOf(SPACING_PROPORTIONAL, SPACING_CHARACTER, SPACING_PROPORTIONAL, SPACING_PROPORTIONAL)

        val REGISTRY_8859 = "iso8859"
        val REGISTRY_10646 = "iso10646"
        private val REGISTRIES = arrayOf(REGISTRY_8859, REGISTRY_10646)

        val ENCODING = "1"

        private val WILD = "*"
        private val NONE = "0"
        private var sDpi = 308

        val defaultSpecs: Array<FontSpec>
            get() {
                val fontSpecs = ArrayList<FontSpec>()
                val specs = arrayOfNulls<String>(NUM_FIELDS)
                specs[PRE_FIELD] = ""
                specs[FOUNDRY] = FOUNDRY_DEFAULT
                specs[SET_WIDTH_NAME] = SET_WIDTH_DEFAULT
                specs[ADD_STYLE] = ""
                specs[PIXEL_SIZE] = NONE
                specs[POINT_SIZE] = NONE
                specs[RESOLUTION_X] = NONE
                specs[RESOLUTION_Y] = NONE
                specs[AVERAGE_WIDTH] = NONE
                specs[CHARSET_ENCODING] = ENCODING
                for (i in FAMILIES.indices) {
                    specs[FAMILY_NAME] = FAMILIES[i]
                    specs[SPACING] = FAMILIES_SPACINGS[i]
                    for (weight in WEIGHTS) {
                        specs[WEIGHT_NAME] = weight
                        for (slant in SLANTS) {
                            specs[SLANT] = slant
                            for (registry in REGISTRIES) {
                                specs[CHARSET_REGISTRY] = registry
                                fontSpecs.add(FontSpec(Platform.join("-", specs.toList())))
                            }
                        }
                    }
                }
                return fontSpecs.toTypedArray<FontSpec>()
            }

        fun initDpi(densityDpi: Int) {
            sDpi = densityDpi
        }
    }
}
