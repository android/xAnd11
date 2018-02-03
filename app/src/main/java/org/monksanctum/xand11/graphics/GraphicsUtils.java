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

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;

import org.monksanctum.xand11.comm.PacketReader;
import org.monksanctum.xand11.comm.PacketWriter;

import static android.graphics.Color.blue;
import static android.graphics.Color.green;
import static android.graphics.Color.red;

public class GraphicsUtils {
    public static Bitmap readBitmap(PacketReader reader, byte leftPad, int width, int height,
            byte depth, GraphicsContext context) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
        if (depth == 1) {
            readDepth1Bitmap(reader, leftPad, bitmap, context);
        } else {
            throw new RuntimeException("Healp!");
        }
        return bitmap;
    }

    public static Bitmap readZBitmap(PacketReader reader, int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int b = reader.readByte();
                int g = reader.readByte();
                int r = reader.readByte();
                bitmap.setPixel(j, i, 0xff000000 | (r << 16) | (g << 8) | b);
            }
        }
        return bitmap;
    }

    private static void readDepth1Bitmap(PacketReader reader, byte leftPad, Bitmap bitmap,
            GraphicsContext context) {
        final int width = bitmap.getWidth();
        final int height = bitmap.getHeight();
        final int rightPad = -(width + leftPad) & 7;
        BitReader bitReader = new BitReader(reader);
        for (int i = 0; i < height; i++) {
            for (int k = 0; k < leftPad; k++) {
                bitReader.readBit();
            }
            for (int j = 0; j < width; j++) {
                bitmap.setPixel(j, i,
                        bitReader.readBit() ? context.foreground : context.background);
            }
            for (int k = 0; k < rightPad; k++) {
                bitReader.readBit();
            }
        }
    }

    public static void writeBitmap(PacketWriter writer, byte leftPad, int width, int height,
            byte depth, GraphicsContext context, Bitmap bitmap) {
        if (depth == 1) {
            writeDepth1Bitmap(writer, leftPad, bitmap, context);
        } else {
            throw new RuntimeException("Healp!");
        }
    }

    public static void writeZBitmap(PacketWriter writer, int width, int height, Bitmap bitmap) {
        int count = 0;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int pixel = bitmap.getPixel(j, i);
                writer.writeByte((byte) blue(pixel));
                writer.writeByte((byte) green(pixel));
                writer.writeByte((byte) red(pixel));
                count += 3;
            }
        }
        while ((count % 4) != 0) {
            writer.writeByte((byte) 0);
            count++;
        }
    }

    private static void writeDepth1Bitmap(PacketWriter writer, byte leftPad, Bitmap bitmap,
            GraphicsContext context) {
        final int width = bitmap.getWidth();
        final int height = bitmap.getHeight();
        final int rightPad = -(width + leftPad) & 7;
        BitWriter bitReader = new BitWriter(writer);
        for (int i = 0; i < height; i++) {
            for (int k = 0; k < leftPad; k++) {
                bitReader.writeBit(false);
            }
            for (int j = 0; j < width; j++) {
                bitReader.writeBit(bitmap.getPixel(j, i) == context.foreground);
            }
            for (int k = 0; k < rightPad; k++) {
                bitReader.writeBit(false);
            }
        }
    }

    private static class BitReader {
        private final PacketReader mReader;
        private int mask;
        private byte value;

        public BitReader(PacketReader reader) {
            mReader = reader;
        }

        public boolean readBit() {
            if (mask == 0) {
                mask = 0x80;
                value = mReader.readByte();
            }
            int bit = (value & mask);
            mask >>= 1;
            return bit != 0;
        }
    }

    private static class BitWriter {
        private final PacketWriter mWriter;
        private int mask = 0x80;
        private byte value;

        public BitWriter(PacketWriter writer) {
            mWriter = writer;
        }

        public void writeBit(boolean bit) {
            if (bit) value |= mask;
            mask >>= 1;
            if (mask == 0) {
                mask = 0x80;
                mWriter.writeByte(value);
                value = 0;
            }
        }
    }
}
