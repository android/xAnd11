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

package org.monksanctum.xand11.graphics;

import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Xfermode;

import org.monksanctum.xand11.errors.ValueError;

public class GraphicsContext {

    private final int mId;

    public int drawable;
    public byte function;
    public int planeMask;
    public int foreground = 0xff000000;
    public int background = 0xffffffff;
    public int lineWidth; // Card16
    public byte lineStyle;
    public byte capStyle;
    public byte joinStyle;
    public byte fillStyle;
    public byte fillRule;
    public int tile; // PIXMAP
    public int stipple; // PIXMAP
    public int tileStippleX; // Card16
    public int tileStippleY; // Card16
    public int font; // Font
    public byte subwindowMode;
    public boolean graphicsExposures;
    public int clipX; // Card16
    public int clipY; // Card16
    public int clipMask; // PIXMAP
    public int dashOffset; // Card16
    public byte dashes;
    public byte arcMode;

    private Paint mPaint;

    public GraphicsContext(int id) {
        mId = id;
    }

    public void createPaint() throws ValueError {
        mPaint = new Paint();
        mPaint.setColor(foreground | 0xff000000);
        mPaint.setStrokeWidth(lineWidth);
        switch (function) {
            case FUNCTION_XOR:
                try {
                    mPaint.setXfermode((Xfermode) Class.forName("android.graphics.PixelXorXfermode")
                            .getConstructor(int.class).newInstance(0xffffffff));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            default:
                mPaint.setXfermode(null);
                break;
        }
        switch (capStyle) {
            case CAP_STYLE_NOT_LAST:
            case CAP_STYLE_BUTT:
                mPaint.setStrokeCap(Cap.BUTT);
                break;
            case CAP_STYLE_ROUND:
                mPaint.setStrokeCap(Cap.ROUND);
                break;
            case CAP_STYLE_PROJECTING:
                mPaint.setStrokeCap(Cap.SQUARE);
                break;
        }
        switch (joinStyle) {
            case JOIN_STYLE_MITER:
                mPaint.setStrokeJoin(Join.MITER);
                break;
            case JOIN_STYLE_ROUND:
                mPaint.setStrokeJoin(Join.ROUND);
                break;
            case JOIN_STYLE_BEVEL:
                mPaint.setStrokeJoin(Join.BEVEL);
                break;
        }
    }

    public static final byte FUNCTION_CLEAR = 0;
    public static final byte FUNCTION_AND = 1;
    public static final byte FUNCTION_AND_REVERSE = 2;
    public static final byte FUNCTION_COPY = 3;
    public static final byte FUNCTION_AND_INVERTED = 4;
    public static final byte FUNCTION_NOOP = 5;
    public static final byte FUNCTION_XOR = 6;
    public static final byte FUNCTION_OR = 7;
    public static final byte FUNCTION_NOR = 8;
    public static final byte FUNCTION_EQUIV = 9;
    public static final byte FUNCTION_INVERT = 10;
    public static final byte FUNCTION_OR_REVERSE = 11;
    public static final byte FUNCTION_COPY_INVERTED = 12;
    public static final byte FUNCTION_OR_INVERTED = 13;
    public static final byte FUNCTION_NAND = 14;
    public static final byte FUNCTION_SET = 15;

    public static final byte STYLE_SOLID = 0;
    public static final byte STYLE_ON_OFF_DASH = 1;
    public static final byte STYLE_DOUBLE_DASH = 2;

    public static final byte CAP_STYLE_NOT_LAST = 0;
    public static final byte CAP_STYLE_BUTT = 1;
    public static final byte CAP_STYLE_ROUND = 2;
    public static final byte CAP_STYLE_PROJECTING = 3;

    public static final byte JOIN_STYLE_MITER = 0;
    public static final byte JOIN_STYLE_ROUND = 1;
    public static final byte JOIN_STYLE_BEVEL = 2;

    public static final byte FILL_STYLE_SOLID = 0;
    public static final byte FILL_STYLE_TILED = 1;
    public static final byte FILL_STYLE_STIPPLED = 2;
    public static final byte FILL_STYLE_OPAQUE_STIPPLED = 3;

    public static final byte FILL_RULE_EVEN_ODD = 0;
    public static final byte FILL_RULE_WINDING = 1;

    public static final byte SUBWINDOW_MODE_CLIP_BY_CHILDREN = 0;
    public static final byte SUBWINDOW_MODE_INCLUDE_INFERIORS = 1;

    public static final byte ARC_MODE_CHORD = 0;
    public static final byte ARC_MODE_PIE_SLICE = 1;

    public Paint getPaint() {
        return mPaint;
    }
}
