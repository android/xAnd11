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

import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.Log;

import org.monksanctum.xand11.atoms.AtomManager;

import java.util.ArrayList;
import java.util.List;

import static org.monksanctum.xand11.Utils.toCard16;

public class Font {

    public static final byte LEFT_TO_RIGHT = 0;
    public static final byte RIGHT_TO_LEFT = 1;

    static final String DEFAULT = "cursor";
    static final String FIXED = "fixed";
    private static final float FONT_SCALING = 2.5f;

    public final List<FontProperty> mFontProperties = new ArrayList<>();
    public final List<CharInfo> mChars = new ArrayList<>();
    private final FontSpec mSpec;

    public CharInfo minBounds = new CharInfo();
    public CharInfo maxBounds = new CharInfo();

    public final char minCharOrByte2 = 32; // Card16
    public final char defaultChar = 32; // Card16
    public char maxCharOrByte2 = 255; // Card16

    public boolean isRtl;

    public final byte minByte1 = 0;
    public final byte maxByte1 = 0;

    public boolean allCharsExist;

    public int fontAscent; // Int16
    public int fontDescent; // Int16

    private final Paint mPaint;

    Font(FontSpec spec, String name) {
        mSpec = spec;
        mPaint = new Paint();
        // TODO: Add default and fixed.
        if (spec.getSpec(0).equals(DEFAULT)) {
            mPaint.setTypeface(Typeface.DEFAULT);
        } else if (spec.getSpec(0).equals(FIXED)){
            mPaint.setTypeface(Typeface.MONOSPACE);
        } else {
            Typeface base = Typeface.DEFAULT;
            int style = Typeface.NORMAL;

            if (FontSpec.WEIGHT_BOLD.equals(mSpec.getSpec(FontSpec.WEIGHT_NAME))) {
                style |= Typeface.BOLD;
            }
            if (FontSpec.SLANT_ITALICS.equals(mSpec.getSpec(FontSpec.SLANT))) {
                style |= Typeface.ITALIC;
            }

            try {
                String sizeStr = mSpec.getSpec(FontSpec.PIXEL_SIZE);
                float n = Float.valueOf(sizeStr);

                if (n > 0) {
                    mPaint.setTextSize(n * FONT_SCALING);
                }
            } catch (java.lang.NumberFormatException e) {
            }

            String type = mSpec.getSpec(FontSpec.FAMILY_NAME);
            if (!FontSpec.SPACING_PROPORTIONAL.equals(mSpec.getSpec(FontSpec.SPACING))) {
                base = Typeface.MONOSPACE;
            } else if (FontSpec.FAMILY_DEFAULT.equals(type)) {
                base = Typeface.DEFAULT;
            } else if (FontSpec.FAMILY_SERIF.equals(type)) {
                base = Typeface.SERIF;
            } else if (FontSpec.FAMILY_SANS_SERIF.equals(type)) {
                base = Typeface.SANS_SERIF;
            } else {
                base = Typeface.create(type, style);
            }
            if (FontSpec.REGISTRY_10646.equals(mSpec.getSpec(FontSpec.CHARSET_REGISTRY))) {
                maxCharOrByte2 = 65534;
            }

            mPaint.setTypeface(Typeface.create(base, style));
        }

        // Calculate the minimum and maximum widths.
        char[] bytes = new char[255 - minCharOrByte2 + 1];
        float[] widths = new float[bytes.length];

        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (char) (i + minCharOrByte2);
        }

        Paint.FontMetricsInt metrics = mPaint.getFontMetricsInt();

        fontAscent = (short) -metrics.ascent;
        fontDescent = (short) metrics.descent;
        maxBounds.ascent = (short) -metrics.top;
        maxBounds.descent = (short) metrics.bottom;

        mPaint.getTextWidths(new String(bytes), widths);

        Rect bounds = new Rect();
        minBounds.characterWidth = Integer.MAX_VALUE;
        for (int i = 0; i < widths.length; i++) {
            float width = widths[i];
            if (width < minBounds.characterWidth) {
                minBounds.characterWidth = (int) width;
            }
            if (width > maxBounds.characterWidth) {
                maxBounds.characterWidth = (int) width;
            }
            // TODO: Don't hold this stuff in memory, seems wasteful.
            CharInfo info = new CharInfo();
            mPaint.getTextBounds(bytes, i, 1, bounds);
            info.leftSideBearing = toCard16(bounds.left);
            info.rightSideBearing = toCard16(bounds.right);
            info.characterWidth = (int) width;
            info.ascent = toCard16(-bounds.top);
            info.descent = toCard16(bounds.bottom);
            mChars.add(info);
        }
        maxBounds.rightSideBearing = maxBounds.characterWidth;

        if (name != null) {
            FontProperty nameProp = new FontProperty();
            nameProp.name = AtomManager.getInstance().internAtom("FONT");
            nameProp.value = AtomManager.getInstance().internAtom(name);
            mFontProperties.add(nameProp);
        }
    }

    public Paint getPaint() {
        return mPaint;
    }

    public List<CharInfo> getChars() {
//        Log.d("Font", "getChars");
//        ArrayList<CharInfo> chars = new ArrayList();
//        char[] bytes = new char[maxCharOrByte2 - minCharOrByte2];
//        float[] widths = new float[bytes.length];
//        Rect bounds = new Rect();
//        for (int i = 0; i < bytes.length; i++) {
//            bytes[i] = (char) (i + minCharOrByte2);
//        }
//        mPaint.getTextWidths(new String(bytes), widths);
//        for (int i = 0; i < bytes.length; i++) {
//            CharInfo info = new CharInfo();
//            float width = widths[i];
//            mPaint.getTextBounds(bytes, i, 1, bounds);
//            info.leftSideBearing = toCard16(bounds.left);
//            info.rightSideBearing = toCard16(bounds.right);
//            info.characterWidth = (int) width;
//            info.ascent = toCard16(-bounds.top);
//            info.descent = toCard16(bounds.bottom);
//            chars.add(info);
//        }
//        Log.d("Font", "Done");
        return mChars;
    }

    @Override
    public String toString() {
        return mSpec.toString();
    }

    public void getTextBounds(String str, int x, int y, Rect rect) {
        rect.left = x;
        rect.right = x + (int) mPaint.measureText(str);
        rect.top = y - maxBounds.ascent;
        rect.bottom = y + maxBounds.descent;
    }

    public static class FontProperty {
        public int name; // Atom
        public int value;
    }

    public static class CharInfo {
        public int leftSideBearing; // Int16
        public int rightSideBearing; // Int16
        public int characterWidth; // Int16
        public int ascent; // Int16
        public int descent; // Int16
        public int attributes; // Card16
    }
}
