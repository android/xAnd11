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

package org.monksanctum.xand11.extension

import android.util.ArrayMap
import android.util.Log
import android.util.SparseArray

import org.monksanctum.xand11.Dispatcher
import org.monksanctum.xand11.comm.PacketWriter
import org.monksanctum.xand11.comm.XProtoReader
import org.monksanctum.xand11.comm.XProtoReader.ReadException
import org.monksanctum.xand11.comm.XProtoWriter
import org.monksanctum.xand11.comm.XProtoWriter.WriteException
import org.monksanctum.xand11.comm.XSerializable

class ExtensionManager(private val mDispatcher: Dispatcher) {

    private val mExtensions = ArrayMap<String, ExtensionInfo>()
    private val mExtensionLookup = SparseArray<ExtensionInfo>()
    private var mExtensionOp = EXTENSION_OP_START

    val extensions: Set<String>
        get() = mExtensions.keys

    init {
        addExtension(BigRequests())
    }

    fun getExtensionForOp(op: Int): Extension? {
        val info = mExtensionLookup.get(op)
        return info?.mExtension
    }

    private fun addExtension(extension: Extension) {
        val info = ExtensionInfo(extension.name, extension)
        info.mIsPresent = true
        info.mMajorOpCode = mExtensionOp++
        mExtensions[extension.name] = info
        mExtensionLookup.append(info.mMajorOpCode.toInt(), info)
        extension.setOpCode(info.mMajorOpCode)
        mDispatcher.addPacketHandler(extension)
    }

    fun queryExtension(name: String): ExtensionInfo {
        var info = mExtensions[name]
        if (info == null) {
            Log.d(TAG, "Query for absent extension $name")
            info = ExtensionInfo("", null)
        }
        return info
    }

    class ExtensionInfo : XSerializable {
        private val mName: String
        internal val mExtension: Extension?
        internal var mIsPresent = false
        internal var mMajorOpCode: Byte = -1
        internal var mFirstEvent: Byte = -1
        internal var mFirstError: Byte = -1

        constructor(name: String, extension: Extension?) {
            mName = name
            mExtension = extension
        }

        constructor() {
            // Used for reading.
            mName = ""
            mExtension = null
        }

        @Throws(WriteException::class)
        override fun write(writer: XProtoWriter) {
            writer.writeByte((if (mIsPresent) 1 else 0).toByte())
            writer.writeByte(mMajorOpCode)
            writer.writeByte(mFirstEvent)
            writer.writeByte(mFirstError)
            writer.writePadding(20)
        }

        @Throws(ReadException::class)
        override fun read(reader: XProtoReader) {
            mIsPresent = reader.readByte().toInt() != 0
            mMajorOpCode = reader.readByte()
            mFirstEvent = reader.readByte()
            mFirstError = reader.readByte()
            reader.readPadding(20)
        }

        override fun toString(): String {
            return "$mName($mIsPresent,$mMajorOpCode)"
        }
    }

    companion object {
        private val TAG = "ExtensionManager"

        val EXTENSION_OP_START: Byte = -126
    }
}
