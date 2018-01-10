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

package org.monksanctum.xand11;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import org.monksanctum.xand11.comm.XProtoReader;
import org.monksanctum.xand11.comm.XProtoWriter;
import org.monksanctum.xand11.comm.XSerializable;
import org.monksanctum.xand11.display.Format;
import org.monksanctum.xand11.display.Screen;

import java.util.ArrayList;
import java.util.List;

public class XServerInfo implements XSerializable {

    private static final String TAG = "XServerInfo";

    private final List<Format> mFormats = new ArrayList<>();
    private final List<Screen> mScreens = new ArrayList<>();

    private int mReleaseNumber = 1;
    private int mMaxPacketLength = 0x7FFF;
    private int mResourceIdMask = 0x00fffff;
    private int mResourceIdBase = 0x1f00000;
    private int mMotionBufferSize = 5;
    private byte mBitmapScanlineUnit = (byte) 8;
    private byte mBitmapScanlinePad = (byte) 8;
    private byte mMinKeycode = (byte) 8;
    private byte mMaxKeycode = (byte) -127;
    private byte mImageOrder = ImageOrder.LSB_FIRST;
    private byte mBitmapOrder = BitmapOrder.MOST_BIT_FIRST;
    private String mVendor = "org.monksactum"; // ?

    public XServerInfo(Context context) {
        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        mScreens.add(new Screen(metrics));
        mFormats.add(new Format());
    }

    public XServerInfo() {
        // For reading.
    }

    public List<Format> getFormats() {
        return mFormats;
    }

    public List<Screen> getScreens() {
        return mScreens;
    }

    public void read(XProtoReader reader) throws XProtoReader.ReadException {
        mReleaseNumber = reader.readCard32();
        mResourceIdBase = reader.readCard32();
        mResourceIdMask = reader.readCard32();
        mMotionBufferSize = reader.readCard32();

        final int vendorLength = reader.readCard16();
        mMaxPacketLength = reader.readCard16();
        final int numScreens = reader.readByte();
        final int numFormats = reader.readByte();

        mImageOrder = reader.readByte();
        mBitmapOrder = reader.readByte();

        mBitmapScanlineUnit = reader.readByte();
        mBitmapScanlinePad = reader.readByte();
        mMinKeycode = reader.readByte();
        mMaxKeycode = reader.readByte();

        reader.readPadding(4);
        mVendor = reader.readPaddedString(vendorLength);
        mFormats.clear();
        mScreens.clear();
        for (int i = 0; i < numFormats; i++) {
            Format format = new Format();
            format.read(reader);
            mFormats.add(format);
        }
        for (int i = 0; i < numScreens; i++) {
            Screen screen = new Screen();
            screen.read(reader);
            mScreens.add(screen);
        }
    }

    public void write(XProtoWriter writer) throws XProtoWriter.WriteException {
        List<Format> formats = getFormats();
        List<Screen> screens = getScreens();

        writer.writeCard32(mReleaseNumber);
        writer.writeCard32(mResourceIdBase); // reseource-id-base
        writer.writeCard32(mResourceIdMask); // reseource-id-mask
        writer.writeCard32(mMotionBufferSize); // motion-buffer-size

        writer.writeCard16(mVendor.length());
        writer.writeCard16(mMaxPacketLength);
        writer.writeByte((byte) screens.size());
        writer.writeByte((byte) formats.size());

        writer.writeByte(mImageOrder);
        writer.writeByte(mBitmapOrder);

        writer.writeByte(mBitmapScanlineUnit); // bitmap-format-scanline-unit
        writer.writeByte(mBitmapScanlinePad); // bitmap-format-scanline-upad
        writer.writeByte(mMinKeycode); // min-keycode
        writer.writeByte(mMaxKeycode); // max-keycode

        writer.writePadding(4);
        writer.writePaddedString(mVendor);
        for (int i = 0; i < formats.size(); i++) {
            formats.get(i).write(writer);
        }
        for (int i = 0; i < screens.size(); i++) {
            screens.get(i).write(writer);
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("XServerInfo(" + mReleaseNumber + ") {");
        builder.append("  vendor=" + mVendor).append('\n');
        builder.append("  resources;base=").append(Integer.toHexString(mResourceIdBase))
                .append(",mask=").append(Integer.toHexString(mResourceIdMask)).append('\n');
        builder.append("  motionBufferSize=").append(mMotionBufferSize).append('\n');
        builder.append("  maxPacket=").append(mMaxPacketLength).append('\n');
        builder.append("  order;image=").append(mImageOrder).append(",bitmap=").append(mBitmapOrder)
                .append('\n');
        builder.append("  bitmapScan;unit=").append(mBitmapScanlineUnit).append(",pad=")
                .append(mBitmapScanlinePad).append('\n');
        builder.append("  keycode;min=").append(mMinKeycode).append(",max=").append(mMaxKeycode)
                .append('\n');
        builder.append("  screens=[ \n");
        for (Screen screen : mScreens) {
            builder.append("    ").append(screen.toString()).append(",\n");
        }
        builder.append("]\n");
        builder.append("  formats=[ ");
        for (Format format : mFormats) {
            builder.append(format.toString()).append(", ");
        }
        builder.append("]\n");
        builder.append("}");
        return builder.toString();
    }

    public static class ImageOrder {
        public static final byte LSB_FIRST = 0;
        public static final byte MSB_FIRST = 1;
    }

    public static class BitmapOrder {
        public static final byte LEAST_BIT_FIRST = 0;
        public static final byte MOST_BIT_FIRST = 1;
    }
}
