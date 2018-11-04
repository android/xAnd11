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

package org.monksanctum.xand11.graphics

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.Cap
import android.graphics.Paint.Join
import android.graphics.Path
import android.graphics.Xfermode

import org.monksanctum.xand11.errors.ValueError
import org.monksanctum.xand11.fonts.Font
import org.monksanctum.xand11.fonts.FontManager

class GraphicsContext(private val mId: Int) {

    var drawable: Int = 0
    var function: Byte = 0
    var planeMask: Int = 0
    var foreground = -0x1000000
    var background = -0x1
    var lineWidth: Int = 0 // Card16
    var lineStyle: Byte = 0
    var capStyle: Byte = 0
    var joinStyle: Byte = 0
    var fillStyle: Byte = 0
    var fillRule: Byte = 0
    var tile: Int = 0 // PIXMAP
    var stipple: Int = 0 // PIXMAP
    var tileStippleX: Int = 0 // Card16
    var tileStippleY: Int = 0 // Card16
    var font: Int = 0 // Font
    var subwindowMode: Byte = 0
    var graphicsExposures: Boolean = false
    var clipX: Int = 0 // Card16
    var clipY: Int = 0 // Card16
    var clipMask: Int = 0 // PIXMAP
    var dashOffset: Int = 0 // Card16
    var dashes: Byte = 0
    var arcMode: Byte = 0

    var paint: Paint? = null
        private set
    private var p: Path? = null

    fun applyToPaint(p: Paint): Paint {
        p.color = foreground or -0x1000000
        p.strokeWidth = lineWidth.toFloat()
        when (function) {
            FUNCTION_XOR -> {
            }
            else -> p.xfermode = null
        }// TODO: Support this.
        when (capStyle) {
            CAP_STYLE_NOT_LAST, CAP_STYLE_BUTT -> p.strokeCap = Cap.BUTT
            CAP_STYLE_ROUND -> p.strokeCap = Cap.ROUND
            CAP_STYLE_PROJECTING -> p.strokeCap = Cap.SQUARE
        }
        when (joinStyle) {
            JOIN_STYLE_MITER -> p.strokeJoin = Join.MITER
            JOIN_STYLE_ROUND -> p.strokeJoin = Join.ROUND
            JOIN_STYLE_BEVEL -> p.strokeJoin = Join.BEVEL
        }
        return p
    }

    @Throws(ValueError::class)
    fun createPaint(fontManager: FontManager) {
        val f = fontManager.getFont(font)
        paint = applyToPaint(if (f != null) f.paint else Paint())
    }

    fun setClipPath(p: Path) {
        this.p = p
    }

    fun init(c: Canvas) {
        if (p != null) {
            c.clipPath(p!!)
        }
    }

    companion object {

        val FUNCTION_CLEAR: Byte = 0
        val FUNCTION_AND: Byte = 1
        val FUNCTION_AND_REVERSE: Byte = 2
        val FUNCTION_COPY: Byte = 3
        val FUNCTION_AND_INVERTED: Byte = 4
        val FUNCTION_NOOP: Byte = 5
        val FUNCTION_XOR: Byte = 6
        val FUNCTION_OR: Byte = 7
        val FUNCTION_NOR: Byte = 8
        val FUNCTION_EQUIV: Byte = 9
        val FUNCTION_INVERT: Byte = 10
        val FUNCTION_OR_REVERSE: Byte = 11
        val FUNCTION_COPY_INVERTED: Byte = 12
        val FUNCTION_OR_INVERTED: Byte = 13
        val FUNCTION_NAND: Byte = 14
        val FUNCTION_SET: Byte = 15

        val STYLE_SOLID: Byte = 0
        val STYLE_ON_OFF_DASH: Byte = 1
        val STYLE_DOUBLE_DASH: Byte = 2

        val CAP_STYLE_NOT_LAST: Byte = 0
        val CAP_STYLE_BUTT: Byte = 1
        val CAP_STYLE_ROUND: Byte = 2
        val CAP_STYLE_PROJECTING: Byte = 3

        val JOIN_STYLE_MITER: Byte = 0
        val JOIN_STYLE_ROUND: Byte = 1
        val JOIN_STYLE_BEVEL: Byte = 2

        val FILL_STYLE_SOLID: Byte = 0
        val FILL_STYLE_TILED: Byte = 1
        val FILL_STYLE_STIPPLED: Byte = 2
        val FILL_STYLE_OPAQUE_STIPPLED: Byte = 3

        val FILL_RULE_EVEN_ODD: Byte = 0
        val FILL_RULE_WINDING: Byte = 1

        val SUBWINDOW_MODE_CLIP_BY_CHILDREN: Byte = 0
        val SUBWINDOW_MODE_INCLUDE_INFERIORS: Byte = 1

        val ARC_MODE_CHORD: Byte = 0
        val ARC_MODE_PIE_SLICE: Byte = 1
    }
}
