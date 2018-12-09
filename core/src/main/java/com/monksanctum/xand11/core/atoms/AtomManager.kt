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

package org.monksanctum.xand11.atoms

import org.monksanctum.xand11.core.Throws

import org.monksanctum.xand11.errors.AtomError

class AtomManager {

    private val mAtoms = ArrayList<String?>()

    init {
        addPredifinedAtoms()
    }

    private fun addPredifinedAtoms() {
        mAtoms.add(0, null)
        // TODO: Move these to constants.
        mAtoms.add(1, "PRIMARY")
        mAtoms.add(2, "SECONDARY")
        mAtoms.add(3, "ARC")
        mAtoms.add(4, "ATOM")
        mAtoms.add(5, "BITMAP")
        mAtoms.add(6, "CARDINAL")
        mAtoms.add(7, "COLORMAP")
        mAtoms.add(8, "CURSOR")
        mAtoms.add(9, "CUT_BUFFER0")
        mAtoms.add(10, "CUT_BUFFER1")
        mAtoms.add(11, "CUT_BUFFER2")
        mAtoms.add(12, "CUT_BUFFER3")
        mAtoms.add(13, "CUT_BUFFER4")
        mAtoms.add(14, "CUT_BUFFER5")
        mAtoms.add(15, "CUT_BUFFER6")
        mAtoms.add(16, "CUT_BUFFER7")
        mAtoms.add(17, "DRAWABLE")
        mAtoms.add(18, "FONT")
        mAtoms.add(19, "INTEGER")
        mAtoms.add(20, "PIXMAP")
        mAtoms.add(21, "POINT")
        mAtoms.add(22, "RECTANGLE")
        mAtoms.add(23, "RESOURCE_MANAGER")
        mAtoms.add(24, "RGB_COLOR_MAP")
        mAtoms.add(25, "RGB_BEST_MAP")
        mAtoms.add(26, "RGB_BLUE_MAP")
        mAtoms.add(27, "RGB_DEFAULT_MAP")
        mAtoms.add(28, "RGB_GRAY_MAP")
        mAtoms.add(29, "RGB_GREEN_MAP")
        mAtoms.add(30, "RGB_RED_MAP")
        mAtoms.add(31, "STRING")
        mAtoms.add(32, "VISUALID")
        mAtoms.add(33, "WINDOW")
        mAtoms.add(34, "WM_COMMAND")

        mAtoms.add(35, "WM_HINTS")
        mAtoms.add(36, "WM_CLINT_MACHINE")
        mAtoms.add(37, "WM_ICON_NAME")
        mAtoms.add(38, "WM_ICON_SIZE")
        mAtoms.add(WM_NAME, "WM_NAME")
        mAtoms.add(40, "WM_NORMAL_HINTS")
        mAtoms.add(41, "WM_SIZE_HINTS")
        mAtoms.add(42, "WM_ZOOM_HINTS")
        mAtoms.add(43, "MIN_SPACE")
        mAtoms.add(44, "NORM_SPACE")
        mAtoms.add(45, "MAX_SPACE")
        mAtoms.add(46, "END_SPACE")
        mAtoms.add(47, "SUPERSCRIPT_X")
        mAtoms.add(48, "SUPERSCRIPT_Y")
        mAtoms.add(49, "SUBSCRIPT_X")
        mAtoms.add(50, "SUBSCRIPT_Y")
        mAtoms.add(51, "UNDERLINE_POSITION")
        mAtoms.add(52, "UNDERLINE_THICKNESS")
        mAtoms.add(53, "STRIKEOUT_ASCENT")
        mAtoms.add(54, "STRIKEOUT_DESCENT")
        mAtoms.add(55, "ITALIC_ANGLE")
        mAtoms.add(56, "X_HEIGHT")
        mAtoms.add(57, "QUAD_WIDTH")
        mAtoms.add(58, "WEIGHT")
        mAtoms.add(59, "POINT_SIZE")
        mAtoms.add(60, "RESOLUTION")
        mAtoms.add(61, "COPYRIGHT")
        mAtoms.add(62, "NOTICE")
        mAtoms.add(63, "FONT_NAME")
        mAtoms.add(64, "FAMILY_NAME")
        mAtoms.add(65, "FULL_NAME")
        mAtoms.add(66, "CAP_HEIGHT")
        mAtoms.add(67, "WM_CLASS")
        mAtoms.add(68, "WM_TRANSIENT_FOR")
    }

    fun internAtom(str: String): Int {
        for (i in mAtoms.indices) {
            if (str == mAtoms[i]) {
                return i
            }
        }
        val index = mAtoms.size
        mAtoms.add(str)
        return index
    }

    @Throws(AtomError::class)
    fun getString(id: Int): String {
        if (id >= mAtoms.size) {
            throw AtomError(id)
        }
        return mAtoms[id] ?: throw AtomError(id)
    }

    companion object {

        val ATOM_NONE = 0
        val WM_NAME = 39

        // Global because they are so common.
        val instance: AtomManager by lazy { AtomManager() }

    }
}
