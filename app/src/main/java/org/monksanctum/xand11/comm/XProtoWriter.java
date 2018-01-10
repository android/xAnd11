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

import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Writes all of the basic types in the X11 protocol.
 */
public class XProtoWriter {

    private static final String TAG = "XProtoWriter";
    // Enable with caution
    private static final boolean LOG_EVERY_BYTE = false;

    private final OutputStream mOutputStream;
    private boolean mMsb;
    private boolean mDebug;

    public XProtoWriter(OutputStream stream) {
        mOutputStream = stream;
    }

    public void setDebug(boolean debug) {
        mDebug = debug;
    }

    public void setMsb(boolean isMsb) {
        mMsb = isMsb;
    }

    public void writePaddedString(String str) throws WriteException {
        final int N = str.length();
        writeString(str);
        writePadding((4 - (N % 4)) % 4);
    }

    public void writeString(String str) throws WriteException {
        final int N = str.length();
        for (int i = 0; i < N; i++) {
            writeByte((byte) str.charAt(i));
        }
    }

    public void writePadding(final int length) throws WriteException {
        for (int i = 0; i < length; i++) {
            writeByte((byte) 0);
        }
    }

    public void writeCard32(int card32) throws WriteException {
        if (mMsb) {
            writeByte(card32 >> 24);
            writeByte(card32 >> 16);
            writeByte(card32 >> 8);
            writeByte(card32 >> 0);
        } else {
            writeByte(card32 >> 0);
            writeByte(card32 >> 8);
            writeByte(card32 >> 16);
            writeByte(card32 >> 24);
        }
    }

    public void writeCard16(int card16) throws WriteException {
        if ((card16 & ~0xffff) != 0) {
            throw new IllegalArgumentException(card16 + " is not a card16");
        }
        if (mMsb) {
            writeByte(card16 >> 8);
            writeByte(card16);
        } else {
            writeByte(card16);
            writeByte(card16 >> 8);
        }
    }

    private void writeByte(int i) throws WriteException {
        writeByte((byte) (i & 0xff));
    }

    public void writeByte(byte b) throws WriteException {
        if (LOG_EVERY_BYTE || mDebug) Log.d(TAG, "Write: " + Integer.toHexString(b));
        try {
            mOutputStream.write(b);
        } catch (IOException e) {
            throw new WriteException(e);
        }
    }

    public void flush() throws WriteException {
        try {
            mOutputStream.flush();
        } catch (IOException e) {
            throw new WriteException(e);
        }
    }

    public static class WriteException extends IOException {
        public WriteException(IOException e) {
            super(e);
        }

        public WriteException() {
        }
    }

}
