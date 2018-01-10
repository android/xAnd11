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

package org.monksanctum.xand11.errors;

import android.util.Log;

import org.monksanctum.xand11.comm.Event;
import org.monksanctum.xand11.comm.PacketReader;
import org.monksanctum.xand11.comm.XProtoReader;
import org.monksanctum.xand11.comm.XProtoReader.ReadException;
import org.monksanctum.xand11.comm.XProtoWriter;
import org.monksanctum.xand11.comm.XProtoWriter.WriteException;
import org.monksanctum.xand11.comm.XSerializable;

public abstract class XError extends Exception implements XSerializable {

    public static final byte REQUEST= 1;
    public static final byte VALUE = 2;
    public static final byte WINDOW = 3;
    public static final byte PIXMAP = 4;
    public static final byte ATOM = 5;
    public static final byte CURSOR = 6;
    public static final byte FONT = 7;
    public static final byte MATCH = 8;
    public static final byte DRAWABLE = 9;
    public static final byte ACCESS = 10;
    public static final byte ALLOC = 11;
    public static final byte COLORMAP = 12;
    public static final byte GCONTEXT = 13;
    public static final byte IDCHOICE = 14;
    public static final byte NAME = 15;
    public static final byte LENGTH = 16;
    public static final byte IMPLEMENTATION = 17;

    private static final String TAG = "XError";

    private final byte mCode;
    private PacketReader mReader;

    public XError(byte code, String message) {
        super(message);
        mCode = code;
    }

    public byte getCode() {
        return mCode;
    }

    public void setPacket(PacketReader reader) {
        mReader = reader;
    }

    public abstract int getExtraData();

    @Override
    public void write(XProtoWriter writer) throws WriteException {
        writer.writeCard32(getExtraData());
        writer.writeCard16(mReader.getMinorOpCode());
        writer.writeByte(mReader.getMajorOpCode());
        writer.writePadding(21);
    }

    @Override
    public void read(XProtoReader reader) throws ReadException {
        Log.w(TAG, "Errors cannot be read");
        reader.readPadding(28);
    }
}
