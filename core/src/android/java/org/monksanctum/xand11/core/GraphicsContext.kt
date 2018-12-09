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

package org.monksanctum.xand11.core

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.Cap
import android.graphics.Paint.Join
import android.graphics.Xfermode

import org.monksanctum.xand11.errors.ValueError
import org.monksanctum.xand11.fonts.FontManager

actual class GraphicsContext actual constructor(private val mId: Int) {

    actual var drawable: Int = 0
    actual var function: Byte = 0
    actual var planeMask: Int = 0
    actual var foreground = -0x1000000
    actual var background = -0x1
    actual var lineWidth: Int = 0 // Card16
    actual var lineStyle: Byte = 0
    actual var capStyle: Byte = 0
    actual var joinStyle: Byte = 0
    actual var fillStyle: Byte = 0
    actual var fillRule: Byte = 0
    actual var tile: Int = 0 // PIXMAP
    actual var stipple: Int = 0 // PIXMAP
    actual var tileStippleX: Int = 0 // Card16
    actual var tileStippleY: Int = 0 // Card16
    actual var font: Int = 0 // Font
    actual var subwindowMode: Byte = 0
    actual var graphicsExposures: Boolean = false
    actual var clipX: Int = 0 // Card16
    actual var clipY: Int = 0 // Card16
    actual var clipMask: Int = 0 // PIXMAP
    actual var dashOffset: Int = 0 // Card16
    actual var dashes: Byte = 0
    actual var arcMode: Byte = 0

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
    actual fun createPaint(fontManager: FontManager) {
        val f = fontManager.getFont(font)
        paint = applyToPaint(if (f != null) f.paint else Paint())
    }

    actual fun setClipPath(p: Path) {
        this.p = p
    }

    actual fun init(c: Canvas) {
        if (p != null) {
            c.clipPath(p!!)
        }
    }

    actual fun drawRect(canvas: Canvas, rect: Rect, stroke: Boolean) {
        val p = Paint(paint)
        p!!.style = if (stroke) Paint.Style.STROKE else Paint.Style.FILL
        canvas.drawRect(rect, p)
    }

    actual fun drawText(canvas: Canvas, f: Font?, v: String, x: Int, y: Int, bounds: Rect) {
        var paint = f?.paint?.let { applyToPaint(it) } ?: this.paint;

        Font.getTextBounds(v, paint!!, x, y, bounds)

        paint.color = foreground
        if (DEBUG) Platform.logd("GraphicsContext", "Draw text $v $x $y")
        canvas.drawText(v, x.toFloat(), y.toFloat(), paint)
    }

    actual fun drawPath(canvas: Canvas, p: Path, fill: Boolean) {
        val paint = Paint(this.paint)
        paint!!.style = if (fill) Paint.Style.FILL else Paint.Style.STROKE
        canvas.drawPath(p, paint)
    }

    actual fun drawLine(canvas: Canvas, fx: Float, fy: Float, sx: Float, sy: Float) {
        canvas.drawLine(fx, fy, sx, sy, paint!!)
    }

    actual fun drawBitmap(canvas: Canvas, bitmap: Bitmap, x: Float, y: Float) {
        canvas.drawBitmap(bitmap, x, y, paint)
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

actual fun Canvas.drawBitmap(context: GraphicsContext?, bitmap: Bitmap, src: Rect, bounds: Rect) {
    drawBitmap(bitmap, src, bounds, context?.paint ?: Paint())
}

actual fun Canvas.drawBitmap(context: GraphicsContext?, bitmap: Bitmap, x: Float, y: Float) {
    drawBitmap(bitmap, x, y, context?.paint ?: Paint())
}
