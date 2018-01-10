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

package org.monksanctum.xand11.comm;

/**
 * Writes all bytes into a buffer so they can be easily counted
 * before being written to the XProtoWriter.
 */
public class PacketWriter extends XProtoWriter {

    // TODO: Something more efficient than ArrayList.
    private final XProtoWriter mWriter;

    private byte mMinorOpCode = 0;
    private byte[] mPacket = new byte[32];
    private int mIndex = 0;

    public PacketWriter(XProtoWriter writer) {
        super(null);
        mWriter = writer;
    }

    public PacketReader copyToReader() {
        return new PacketReader((byte) 0, mMinorOpCode, mPacket, mIndex);
    }

    public byte getMinorOpCode() {
        return mMinorOpCode;
    }

    public void setMinorOpCode(byte minorOpCode) {
        mMinorOpCode = minorOpCode;
    }

    public int getLength() {
        return mIndex;
    }

    // TODO: Better name than get4Length.
    public int get4Length() {
        return mIndex / 4;
    }

    @Override
    public void writeByte(byte b) {
        mPacket[mIndex++] = b;
        if (mIndex == mPacket.length) {
            byte[] nPacket = new byte[mPacket.length * 2];
            for (int i = 0; i < mPacket.length; i++) {
                nPacket[i] = mPacket[i];
            }
            mPacket = nPacket;
        }
    }

    void reset() {
        mIndex = 0;
    }

    @Override
    public void flush() throws WriteException {
        for (int i = 0; i < mIndex; i++) {
            mWriter.writeByte(mPacket[i]);
        }
        mWriter.flush();
    }

    // --- Versions of writeX that don't throw exceptions because they aren't possible ---

    @Override
    public void writeCard16(int card16) {
        try {
            super.writeCard16(card16);
        } catch (WriteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void writeCard32(int card32) {
        try {
            super.writeCard32(card32);
        } catch (WriteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void writePaddedString(String str) {
        try {
            super.writePaddedString(str);
        } catch (WriteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void writePadding(int length) {
        try {
            super.writePadding(length);
        } catch (WriteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void writeString(String str) {
        try {
            super.writeString(str);
        } catch (WriteException e) {
            throw new RuntimeException(e);
        }
    }
}
