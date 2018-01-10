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

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.text.TextUtils;
import android.util.Log;

import org.monksanctum.xand11.XService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FontSpec {

    private static final int PRE_FIELD = 0;
    public static final int FOUNDRY = 1;
    public static final int FAMILY_NAME = 2;
    public static final int WEIGHT_NAME = 3;
    public static final int SLANT = 4;
    public static final int SET_WIDTH_NAME = 5;
    public static final int ADD_STYLE = 6;
    public static final int PIXEL_SIZE = 7;
    public static final int POINT_SIZE = 8;
    public static final int RESOLUTION_X = 9;
    public static final int RESOLUTION_Y = 10;
    public static final int SPACING = 11;
    public static final int AVERAGE_WIDTH = 12;
    public static final int CHARSET_REGISTRY = 13;
    public static final int CHARSET_ENCODING = 14;
    public static final int NUM_FIELDS = 15;

    public static final String FOUNDRY_DEFAULT = "android";

    public static final String FAMILY_DEFAULT = "default";
    public static final String FAMILY_MONOSPACE = "monospace";
    public static final String FAMILY_SERIF = "serif";
    public static final String FAMILY_SANS_SERIF = "sans serif";
    private static final String[] FAMILIES = new String[] {
            FAMILY_DEFAULT, FAMILY_MONOSPACE, FAMILY_SERIF, FAMILY_SANS_SERIF,
    };

    public static final String WEIGHT_MEDIUM = "medium";
    public static final String WEIGHT_BOLD = "bold";
    private static final String[] WEIGHTS = new String[] {
            WEIGHT_MEDIUM, WEIGHT_BOLD,
    };

    public static final String SLANT_REGULAR = "r";
    public static final String SLANT_ITALICS = "i";
    private static final String[] SLANTS = new String[] {
            SLANT_REGULAR, SLANT_ITALICS,
    };

    public static final String SET_WIDTH_DEFAULT = "normal";

    public static final String SPACING_PROPORTIONAL = "p";
    public static final String SPACING_CHARACTER = "c";
    private static final String[] FAMILIES_SPACINGS = new String[] {
            SPACING_PROPORTIONAL, SPACING_CHARACTER, SPACING_PROPORTIONAL, SPACING_PROPORTIONAL,
    };

    public static final String REGISTRY_8859 = "iso8859";
    public static final String REGISTRY_10646 = "iso10646";
    private static final String[] REGISTRIES = new String[] {
            REGISTRY_8859, REGISTRY_10646,
    };

    public static final String ENCODING = "1";

    private static final String WILD = "*";
    private static final String NONE = "0";
    private static int sDpi = 308;

    private final String[] mSpecs = new String[NUM_FIELDS];

    public FontSpec(String spec) {
        String[] fields = spec.split("-");
        if (spec.equals(Font.DEFAULT) || spec.equals(Font.FIXED)) {
            mSpecs[0] = spec;
            return;
        }
        if (fields.length != NUM_FIELDS) {
            Log.d("FontSpec", "Invalid spec " + spec);
            mSpecs[0] = Font.DEFAULT;
            return;
        }
        for (int i = 0; i < NUM_FIELDS; i++) {
            mSpecs[i] = fields[i];
        }
    }

    private FontSpec(String[] specs) {
        for (int i = 0; i < NUM_FIELDS; i++) {
            mSpecs[i] = specs[i];
        }
    }

    // TODO: Handle ?s
    public FontSpec matches(String match) {
        String[] fields = match.split("-");
        FontSpec ret = new FontSpec(mSpecs);
        if (fields.length == NUM_FIELDS) {
            for (int i = 0; i < NUM_FIELDS; i++) {
                if (NONE.equals(ret.mSpecs[i]) && !WILD.equals(fields[i])) {
                    ret.mSpecs[i] = fields[i];
                }
            }
        }
        int offset = mSpecs.length - fields.length;
        for (int i = 0; i < fields.length; i++) {
            if (fields[i].equals(WILD)) {
                continue;
            }
            if (!fields[i].equalsIgnoreCase(mSpecs[i + offset])
                    && !NONE.equalsIgnoreCase(mSpecs[i + offset])) {
                return null;
            }
        }
        return ret;
    }

    public String getSpec(int index) {
        return mSpecs[index];
    }

    @Override
    public String toString() {
        if (NONE.equals(mSpecs[PIXEL_SIZE]) ^ NONE.equals(mSpecs[POINT_SIZE])) {
            // TODO: This better...
            if (NONE.equals(mSpecs[RESOLUTION_X])) {
                mSpecs[RESOLUTION_X] = String.valueOf(sDpi);
            }
            if (NONE.equals(mSpecs[RESOLUTION_Y])) {
                mSpecs[RESOLUTION_Y] = String.valueOf(sDpi);
            }
            if (NONE.equals(mSpecs[PIXEL_SIZE])) {
                mSpecs[PIXEL_SIZE] = String.valueOf(
                        Math.round(Integer.parseInt(mSpecs[POINT_SIZE]) * sDpi / 722.7));
            } else {
                mSpecs[POINT_SIZE] = String.valueOf(
                        Math.round(Integer.parseInt(mSpecs[PIXEL_SIZE]) * 722.7 / sDpi));
            }
        }
        return TextUtils.join("-", mSpecs);
    }

    public static FontSpec[] getDefaultSpecs() {
        List<FontSpec> fontSpecs = new ArrayList<>();
        String[] specs = new String[NUM_FIELDS];
        specs[PRE_FIELD] = "";
        specs[FOUNDRY] = FOUNDRY_DEFAULT;
        specs[SET_WIDTH_NAME] = SET_WIDTH_DEFAULT;
        specs[ADD_STYLE] = "";
        specs[PIXEL_SIZE] = NONE;
        specs[POINT_SIZE] = NONE;
        specs[RESOLUTION_X] = NONE;
        specs[RESOLUTION_Y] = NONE;
        specs[AVERAGE_WIDTH] = NONE;
        specs[CHARSET_ENCODING] = ENCODING;
        for (int i = 0; i < FAMILIES.length; i++) {
            specs[FAMILY_NAME] = FAMILIES[i];
            specs[SPACING] = FAMILIES_SPACINGS[i];
            for (String weight : WEIGHTS) {
                specs[WEIGHT_NAME] = weight;
                for (String slant : SLANTS) {
                    specs[SLANT] = slant;
                    for (String registry : REGISTRIES) {
                        specs[CHARSET_REGISTRY] = registry;
                        fontSpecs.add(new FontSpec(TextUtils.join("-", specs)));
                    }
                }
            }
        }
        return fontSpecs.toArray(new FontSpec[0]);
    }

    public static void initDpi(Context context) {
        sDpi = context.getResources().getDisplayMetrics().densityDpi;
    }
}
