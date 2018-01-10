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

import android.content.Context;
import android.util.Log;
import android.util.SparseArray;

import org.monksanctum.xand11.R;
import org.monksanctum.xand11.comm.XProtoReader;
import org.monksanctum.xand11.comm.XProtoReader.ReadException;
import org.monksanctum.xand11.comm.XProtoWriter;
import org.monksanctum.xand11.comm.XProtoWriter.WriteException;
import org.monksanctum.xand11.comm.XSerializable;
import org.monksanctum.xand11.errors.ColorMapError;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

public class ColorMaps {

    private final SparseArray<ColorMap> mMaps = new SparseArray<>();

    public ColorMaps() {
        createColormap(4);
    }

    public ColorMap get(int cmap) throws ColorMapError {
        synchronized (mMaps) {
            ColorMap colorMap = mMaps.get(cmap);
            if (colorMap == null) {
                throw new ColorMapError(cmap);
            }
            return colorMap;
        }
    }

    public void createColormap(int cmap) {
        synchronized (mMaps) {
            mMaps.put(cmap, new ColorMap());
        }
    }

    public void freeColormap(int cmap) {
        synchronized (mMaps) {
            mMaps.remove(cmap);
        }
    }

    public static class ColorMap {
        public static final HashMap<String, Integer> sColorLookup = new HashMap<>();
        private static final String TAG = "ColorMap";
        private final Color temp = new Color();

        public Color getColor(int color) {
            return getColor(dupe((color >> 16) & 0xff), dupe((color >> 8) & 0xff),
                    dupe((color >> 0) & 0xff));
        }

        private int dupe(int i) {
            return i | (i << 8);
        }

        public Color getColor(int r, int g, int b) {
            temp.r = r;
            temp.g = g;
            temp.b = b;
            return temp;
        }

        public static void initNames(Context context) {
            InputStream input = context.getResources().openRawResource(R.raw.rgb);
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            String line;
            try {
                while ((line = reader.readLine()) != null) {
                    String[] fields = line.split(",");
                    if (fields.length != 4) {
                        Log.e(TAG, "Invalid line: " + line);
                        continue;
                    }
                    int color = android.graphics.Color.rgb(Integer.parseInt(fields[0]), Integer
                            .parseInt(fields[1]), Integer.parseInt(fields[2]));
                    sColorLookup.put(fields[3].toLowerCase(), color);
                }
            } catch (IOException e) {
                throw new RuntimeException("Unable to load resource");
            }
        }
    }

    public static class Color implements XSerializable {
        private int r; // Card16
        private int g; // Card16
        private int b; // Card16

        public Color() {
        }

        public Color(int r, int g, int b) {
            this.r = r;
            this.g = g;
            this.b = b;
        }

        public int color() {
            return android.graphics.Color.rgb(r >> 8, g >> 8, b >> 8);
        }

        public Color copy() {
            return new Color(r, g, b);
        }

        @Override
        public void write(XProtoWriter writer) throws WriteException {
            writer.writeCard16(r);
            writer.writeCard16(g);
            writer.writeCard16(b);
            writer.writePadding(2);
        }

        @Override
        public void read(XProtoReader reader) throws ReadException {
            r = reader.readCard16();
            g = reader.readCard16();
            b = reader.readCard16();
            reader.readPadding(2);
        }

        @Override
        public boolean equals(Object o) {
            try {
                Color other = (Color) o;
                return other.r == r && other.g == g && other.b == b;
            } catch (ClassCastException e) {
                return false;
            }
        }
    }
}
