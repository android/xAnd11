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

package org.monksanctum.xand11.display;

import android.text.TextUtils;
import android.util.DisplayMetrics;

import org.monksanctum.xand11.comm.XProtoReader;
import org.monksanctum.xand11.comm.XProtoReader.ReadException;
import org.monksanctum.xand11.comm.XProtoWriter;
import org.monksanctum.xand11.comm.XSerializable;
import org.monksanctum.xand11.windows.XWindowManager;

import java.util.ArrayList;
import java.util.List;

public class Screen implements XSerializable {

    private final List<Depth> mDepths = new ArrayList<>();

    private int mRootWindow;
    private int mColorMap;
    private int mWhitePixel;
    private int mBlackPixel;
    private int mEventMasks;

    private int mWidth; // Card16
    private int mHeight; // Card16
    private int mWidthMillis; // Card16
    private int mHeightMillis; // Card16

    private int mMinInstalledMaps; // Card16
    private int mMaxInstalledMaps; // Card16

    private int mVisialId;

    private byte mBackingStore; // BackingStore

    private byte mSaveUnders; // Bool
    private byte mRootDepth;

    public Screen(DisplayMetrics metrics) {
        int size = Math.max(metrics.widthPixels, metrics.heightPixels);
        mVisialId = 1;
        mWidth = mHeight = size;
        mWidthMillis = mHeightMillis = 300;
        mMinInstalledMaps = 1;
        mMaxInstalledMaps = 4;
        mRootWindow = XWindowManager.ROOT_WINDOW;
        mRootDepth = 32;
        mWhitePixel = 0xffffffff;
        mBlackPixel = 0xff000000;
        mEventMasks = 0;
        mColorMap = 4;
        mBackingStore = BackingStore.ALWAYS;
        mSaveUnders = 0;
        mRootDepth = 32;
        mDepths.add(new Depth((byte) 32));
    }

    public Screen() {
        // For reading.
    }

    public void write(XProtoWriter writer) throws XProtoWriter.WriteException {
        writer.writeCard32(mRootWindow);
        writer.writeCard32(mColorMap);
        writer.writeCard32(mWhitePixel);
        writer.writeCard32(mBlackPixel);
        writer.writeCard32(mEventMasks);

        writer.writeCard16(mWidth);
        writer.writeCard16(mHeight);
        writer.writeCard16(mWidthMillis);
        writer.writeCard16(mHeightMillis);

        writer.writeCard16(mMinInstalledMaps);
        writer.writeCard16(mMaxInstalledMaps);
        writer.writeCard32(mVisialId);

        writer.writeByte(mBackingStore);
        writer.writeByte(mSaveUnders);
        final int N = mDepths.size();
        writer.writeByte(mRootDepth);
        writer.writeByte((byte) N);
        for (int i = 0; i < N; i++) {
            mDepths.get(i).write(writer);
        }
    }

    @Override
    public void read(XProtoReader reader) throws ReadException {
        mRootWindow = reader.readCard32();
        mColorMap = reader.readCard32();
        mWhitePixel = reader.readCard32();
        mBlackPixel = reader.readCard32();
        mEventMasks = reader.readCard32();

        mWidth = reader.readCard16();
        mHeight = reader.readCard16();
        mWidthMillis = reader.readCard16();
        mHeightMillis = reader.readCard16();

        mMinInstalledMaps = reader.readCard16();
        mMaxInstalledMaps = reader.readCard16();
        mVisialId = reader.readCard32();

        mBackingStore = reader.readByte();
        mSaveUnders = reader.readByte();
        mRootDepth = reader.readByte();
        final int N = reader.readByte();
        for (int i = 0; i < N; i++) {
            Depth depth = new Depth();
            depth.read(reader);
            mDepths.add(depth);
        }
    }

    public int getSize() {
        return mWidth;
    }

    public static class Depth implements XSerializable {
        private byte mDepth;
        private final List<VisualType> mVisualTypes = new ArrayList<>();

        public Depth() {
            // Used for reading.
        }

        public Depth(byte depth) {
            mDepth = depth;
            mVisualTypes.add(new VisualType());
        }

        @Override
        public String toString() {
            return "depth=" + mDepth + ",types=" + TextUtils.join(";", mVisualTypes);
        }

        public void write(XProtoWriter writer) throws XProtoWriter.WriteException {
            writer.writeByte(mDepth);
            writer.writePadding(1);
            final int N = mVisualTypes.size();
            writer.writeCard16(N);
            writer.writePadding(4);
            for (int i = 0; i < N; i++) {
                mVisualTypes.get(i).write(writer);
            }
        }

        @Override
        public void read(XProtoReader reader) throws ReadException {
            mDepth = reader.readByte();
            reader.readPadding(1);
            final int N = reader.readCard16();
            reader.readPadding(4);
            for (int i = 0; i < N; i++) {
                VisualType type = new VisualType();
                type.read(reader);
                mVisualTypes.add(type);
            }
        }
    }

    public static class VisualType implements XSerializable {
        private int mVisualId;
        private byte mClass; // VisualClass
        private byte mBitsPerRgb;
        private int mColorMapEntries; // Card16
        private int mRedMask;
        private int mGreenMask;
        private int mBlueMask;

        public VisualType() {
            mVisualId = 1;
            mClass = VisualClass.TRUE_COLOR;
            mBitsPerRgb = 8;
            mColorMapEntries = 256;
            mRedMask = 0xff0000;
            mGreenMask = 0x00ff00;
            mBlueMask = 0x0000ff;
        }

        @Override
        public String toString() {
            return "(" + mVisualId + "," + mClass + "," + mBitsPerRgb + "," + mColorMapEntries
                    + "," + Integer.toHexString(mRedMask) + "," + Integer.toHexString(mGreenMask)
                    + "," + Integer.toHexString(mBlueMask) + ")";
        }

        public void write(XProtoWriter writer) throws XProtoWriter.WriteException {
            writer.writeCard32(mVisualId);
            writer.writeByte(mClass);
            writer.writeByte(mBitsPerRgb);
            writer.writeCard16(mColorMapEntries);
            writer.writeCard32(mRedMask);
            writer.writeCard32(mGreenMask);
            writer.writeCard32(mBlueMask);
            writer.writePadding(4);
        }

        @Override
        public void read(XProtoReader reader) throws ReadException {
            mVisualId = reader.readCard32();
            mClass = reader.readByte();
            mBitsPerRgb = reader.readByte();
            mColorMapEntries = reader.readCard16();
            mRedMask = reader.readCard32();
            mGreenMask = reader.readCard32();
            mBlueMask = reader.readCard32();
            reader.readPadding(4);
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Screen(").append(mVisialId).append(',').append(mRootDepth).append(")\n");
        builder.append("    rootWindow=").append(mRootWindow).append('\n');
        builder.append("    colorMap=").append(mColorMap).append(",min=").append(mMinInstalledMaps)
                .append(",max=").append(mMaxInstalledMaps).append('\n');
        builder.append("    white=").append(Integer.toHexString(mWhitePixel))
                .append(",black=").append(Integer.toHexString(mBlackPixel))
                .append('\n');
        builder.append("    eventMasks=").append(mEventMasks).append('\n');
        builder.append("    width=").append(mWidth).append(",height=").append(mHeight).append('\n');
        builder.append("    millis;width=").append(mWidthMillis)
                .append(",height=").append(mHeightMillis).append('\n');
        builder.append("    backing=").append(mBackingStore).append(",save=").append(mSaveUnders)
                .append('\n');
        builder.append("    depths={");
        for (Depth depth : mDepths) {
            builder.append(depth.toString()).append(", ");
        }
        builder.append("}\n");
        return builder.toString();
    }

    public static class BackingStore {
        public static final int NEVER = 0;
        public static final int WHEN_MAPPED = 1;
        public static final int ALWAYS = 2;
    }

    public static class VisualClass {
        public static final byte STATIC_GRAY = 0;
        public static final byte GRAY_SCALE = 1;
        public static final byte STATIC_COLOR = 2;
        public static final byte PSEUDO_COLOR = 3;
        public static final byte TRUE_COLOR = 4;
        public static final byte DIRECT_COLOR = 5;
    }

}
