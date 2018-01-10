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

import org.monksanctum.xand11.ObjectPool;
import org.monksanctum.xand11.ObjectPool.Recycleable;

import java.util.PriorityQueue;

/**
 * Reads all of the bytes of a packet into a buffer, so that it can
 * be passed off to another thread for processing, and not block
 * the reader.
 */
public class PacketReader extends XProtoReader implements AutoCloseable {

    private final byte mMajorOpCode;
    private final byte mMinorOpCode;
    private ByteArray mBuffer;
    private int mIndex;
    private int mLength;

    public PacketReader(byte majorOpCode, byte minorOpCode) {
        super(null);
        mMajorOpCode = majorOpCode;
        mMinorOpCode = minorOpCode;
    }

    PacketReader(byte major, byte minor, byte[] bytes, int len) {
        this(major, minor);
        mLength = bytes.length;
        mBuffer = sArrays.obtain(mLength);
        for (int i = 0; i < bytes.length; i++) {
            mBuffer.mArray[i] = bytes[i];
        }
    }

    @Override
    public void close() {
        if (mBuffer != null) {
            mBuffer.close();
            mBuffer = null;
        }
    }

    public byte getMajorOpCode() {
        return mMajorOpCode;
    }

    public byte getMinorOpCode() {
        return mMinorOpCode;
    }

    public int getLength() {
        return mLength;
    }

    public int getRemaining() {
        return mLength - mIndex;
    }

    /**
     * Note: May only be called once per PacketReader.
     */
    public void readBytes(XProtoReader reader, int length) throws ReadException {
        mLength = length;
        mBuffer = sArrays.obtain(length);
        mIndex = 0;
        for (int i = 0; i < length; i++) {
            mBuffer.mArray[i] = reader.readByte();
        }
    }

    public void reset() {
        mIndex = 0;
    }

    @Override
    public byte readByte() {
        return mBuffer.mArray[mIndex++];
    }

    // --- Versions of readX that don't throw exceptions because they arent possible ---
    @Override
    public int readCard16() {
        try {
            return super.readCard16();
        } catch (ReadException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int readChar16() {
        try {
            return super.readChar16();
        } catch (ReadException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int readCard32() {
        try {
            return super.readCard32();
        } catch (ReadException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String readPaddedString(int n) {
        try {
            return super.readPaddedString(n);
        } catch (ReadException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String readString(int n) {
        try {
            return super.readString(n);
        } catch (ReadException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String readPaddedString16(int n) {
        try {
            return super.readPaddedString16(n);
        } catch (ReadException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String readString16(int n) {
        try {
            return super.readString16(n);
        } catch (ReadException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void readPadding(int n) {
        try {
            super.readPadding(n);
        } catch (ReadException e) {
            throw new RuntimeException(e);
        }
    }

    private static class ByteArray extends Recycleable {
        private byte[] mArray;

        public ByteArray(int size) {
            mArray = new byte[size];
        }
    }

    private static final ObjectPool<ByteArray, Integer> sArrays =
            new ObjectPool<ByteArray, Integer>() {
        @Override
        protected boolean validate(ByteArray inst, Integer... arg) {
            int length = inst.mArray.length;
            if (length > arg[0] * 4) {
                return false; // Save really big ones for big packets.
            }
            return length >= arg[0];
        }

        @Override
        protected ByteArray create(Integer... arg) {
            return new ByteArray(arg[0]);
        }
    };
}
