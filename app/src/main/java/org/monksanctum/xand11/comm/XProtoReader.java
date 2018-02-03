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
import java.io.InputStream;

import static org.monksanctum.xand11.Utils.unsigned;

/**
 * Reads all of the basic types specified in the X11 Protocol.
 */
public class XProtoReader {
    private static final String TAG = "XProtoReader";
    // Enable with caution
    private static final boolean LOG_EVERY_BYTE = false;

    private final InputStream mInputStream;
    private boolean mMsb;

    private boolean mDebug;

    public XProtoReader(InputStream stream) {
        mInputStream = stream;
    }

    public void setDebug(boolean debug) {
        mDebug = debug;
    }

    public void setMsb(boolean isMsb) {
        mMsb = isMsb;
    }

    public String readPaddedString(final int n) throws ReadException {
        String ret = readString(n);
        readPadding((4 - (n % 4)) % 4);
        return ret;
    }

    public String readPaddedString16(final int n) throws ReadException {
        String ret = readString16(n);
        readPadding((4 - ((2 * n) % 4)) % 4);
        return ret;
    }

    public String readString16(final int n) throws ReadException {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < n; i++) {
            builder.append((char) readChar16());
        }
        return builder.toString();
    }

    public int readChar16() throws ReadException {
        byte first = readByte();
        byte second = readByte();
        if (mMsb) {
            return (unsigned(second) << 8) | unsigned(first);
        } else {
            return (unsigned(first) << 8) | unsigned(second);
        }
    }

    public String readString(final int n) throws ReadException {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < n; i++) {
            builder.append((char) readByte());
        }
        return builder.toString();
    }

    public void readPadding(final int n) throws ReadException {
        for (int i = 0; i < n; i++) {
            readByte();
        }
    }

    public int readCard16() throws ReadException {
        byte first = readByte();
        byte second = readByte();
        if (mMsb) {
            return (unsigned(first) << 8) | unsigned(second);
        } else {
            return (unsigned(second) << 8) | unsigned(first);
        }
    }

    public int readInt16() throws ReadException {
        byte first = readByte();
        byte second = readByte();
        if (mMsb) {
            return (first << 8) | unsigned(second);
        } else {
            return (second << 8) | unsigned(first);
        }
    }

    public int readCard32() throws ReadException {
        int first = readCard16();
        int second = readCard16();
        if (mMsb) {
            return (first << 16) | second;
        } else {
            return (second << 16) | first;
        }
    }

    public byte readByte() throws ReadException {
        try {
            int read = mInputStream.read();
            if (LOG_EVERY_BYTE || mDebug) Log.d(TAG, "Read: " + Integer.toHexString(read));
            if (read < 0) {
                throw new ReadException();
            }
            return (byte) read;
        } catch (IOException e) {
            throw new ReadException(e);
        }
    }

    public static class ReadException extends IOException {
        public ReadException(IOException e) {
            super(e);
        }

        public ReadException() {
        }
    }
}
