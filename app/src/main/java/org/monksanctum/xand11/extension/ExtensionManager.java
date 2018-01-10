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

package org.monksanctum.xand11.extension;

import android.util.ArrayMap;
import android.util.Log;
import android.util.SparseArray;

import org.monksanctum.xand11.Dispatcher;
import org.monksanctum.xand11.comm.PacketWriter;
import org.monksanctum.xand11.comm.XProtoReader;
import org.monksanctum.xand11.comm.XProtoReader.ReadException;
import org.monksanctum.xand11.comm.XProtoWriter;
import org.monksanctum.xand11.comm.XProtoWriter.WriteException;
import org.monksanctum.xand11.comm.XSerializable;

import java.util.Map;
import java.util.Set;

public class ExtensionManager {
    private static final String TAG = "ExtensionManager";

    public static final byte EXTENSION_OP_START = -126;

    private final Map<String, ExtensionInfo> mExtensions = new ArrayMap<>();
    private final SparseArray<ExtensionInfo> mExtensionLookup = new SparseArray<>();
    private final Dispatcher mDispatcher;
    private byte mExtensionOp = EXTENSION_OP_START;

    public ExtensionManager(Dispatcher dispatcher) {
        mDispatcher = dispatcher;
        addExtension(new BigRequests());
    }

    public Extension getExtensionForOp(int op) {
        ExtensionInfo info = mExtensionLookup.get(op);
        return info != null ? info.mExtension : null;
    }

    private void addExtension(Extension extension) {
        ExtensionInfo info = new ExtensionInfo(extension.getName(), extension);
        info.mIsPresent = true;
        info.mMajorOpCode = mExtensionOp++;
        mExtensions.put(extension.getName(), info);
        mExtensionLookup.append(info.mMajorOpCode, info);
        extension.setOpCode(info.mMajorOpCode);
        mDispatcher.addPacketHandler(extension);
    }

    public ExtensionInfo queryExtension(String name) {
        ExtensionInfo info = mExtensions.get(name);
        if (info == null) {
            Log.d(TAG, "Query for absent extension " + name);
            info = new ExtensionInfo("", null);
        }
        return info;
    }

    public Set<String> getExtensions() {
        return mExtensions.keySet();
    }

    public static class ExtensionInfo implements XSerializable {
        private final String mName;
        private final Extension mExtension;
        boolean mIsPresent = false;
        byte mMajorOpCode = -1;
        byte mFirstEvent = -1;
        byte mFirstError = -1;

        public ExtensionInfo(String name, Extension extension) {
            mName = name;
            mExtension = extension;
        }

        public ExtensionInfo() {
            // Used for reading.
            mName = "";
            mExtension = null;
        }

        @Override
        public void write(XProtoWriter writer) throws WriteException {
            writer.writeByte((byte) (mIsPresent ? 1 : 0));
            writer.writeByte(mMajorOpCode);
            writer.writeByte(mFirstEvent);
            writer.writeByte(mFirstError);
            writer.writePadding(20);
        }

        @Override
        public void read(XProtoReader reader) throws ReadException {
            mIsPresent = reader.readByte() != 0;
            mMajorOpCode = reader.readByte();
            mFirstEvent = reader.readByte();
            mFirstError = reader.readByte();
            reader.readPadding(20);
        }

        @Override
        public String toString() {
            return mName + "(" + mIsPresent + "," + mMajorOpCode + ")";
        }
    }
}
