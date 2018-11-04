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

package org.monksanctum.xand11.comm

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log

import org.monksanctum.xand11.Client
import org.monksanctum.xand11.Dispatcher
import org.monksanctum.xand11.XService
import org.monksanctum.xand11.errors.XError

import org.monksanctum.xand11.Utils.unsigned
import org.monksanctum.xand11.t

class PacketHandler(private val mDispatcher: Dispatcher, private val mClient: Client, private val mListener: ClientListener,
                    private val mWriter: XProtoWriter, looper: Looper) : Handler(looper) {
    private val mPacketWriter: PacketWriter
    var count = 0
        private set

    init {
        mPacketWriter = PacketWriter(mWriter)
    }

    fun sendPacket(reader: PacketReader) {
        obtainMessage(unsigned(reader.majorOpCode), reader).sendToTarget()
    }

    override fun handleMessage(msg: Message) {
        (msg.obj as PacketReader).use { packet ->
            val handler = mDispatcher.getHandler(msg.what)
            if (handler != null) {
                try {
                    if (DEBUG)
                        Log.d(TAG, "Handling " + Request.getName(msg.what) + " "
                                + packet.length)
                    count++
                    t(String.format("Handle(%s)", Request.getName(msg.what))) {
                        handler.handleRequest(mClient, packet, mPacketWriter)
                    }
                    if (packet.remaining != 0) {
                        Log.w(TAG, "${packet.remaining} bytes unhandled in "
                                + Request.getName(msg.what))
                    }
                    if (mPacketWriter.length != 0) {
                        if (ClientListener.PASSTHROUGH) {
                            ClientListener.sReplyTypes.put(msg.what)
                        }
                        t(String.format("Reply(%s)", Request.getName(msg.what))) {
                            handleReply(mPacketWriter)
                        }
                    }
                } catch (e: XError) {
                    e.setPacket(packet)
                    handleError(e, mPacketWriter)
                }

            } else {
                count++
                Log.w(TAG, "Unhandled message " + Request.getName(msg.what) + " " + msg.what)
                if (CRASH_UNKNOWN) {
                    throw PacketException(packet)
                }
            }
        }
        mPacketWriter.reset()
    }

    private fun handleError(e: XError, writer: PacketWriter) {
        Log.w(TAG, "XError!", e)
        synchronized(mWriter) {
            try {
                mWriter.writeByte(Event.ERROR)
                mWriter.writeByte(e.code)
                mWriter.writeCard16(count and 0xffff)
                e.write(writer)
                writer.flush()
            } catch (exception: XProtoWriter.WriteException) {
                mListener.onIOProblem()
            }

        }
    }

    private fun handleReply(writer: PacketWriter) {
        if (DEBUG) Log.d(TAG, "handleReply")
        sendPacket(Event.REPLY, writer)
    }

    fun sendPacket(type: Byte, writer: PacketWriter) {
        if (DEBUG)
            Log.d(TAG, "sendPacket " + Event.getName(type.toInt()) + " "
                    + writer.minorOpCode + " " + count + " " + (writer.WordCount - 6))
        synchronized(mWriter) {
            try {
                mWriter.writeByte(type)
                mWriter.writeByte(writer.minorOpCode)
                mWriter.writeCard16(count and 0xffff)
                if (type == Event.REPLY) {
                    mWriter.writeCard32(writer.WordCount - 6)
                }
                writer.flush()
            } catch (e: XProtoWriter.WriteException) {
                // This one can.
                mListener.onIOProblem()
            }

        }
    }

    private class PacketException(// TODO: Print usefulness from the packet
            private val mPacket: PacketReader) : RuntimeException("Unhandled packet " + Request.getName(mPacket.majorOpCode.toInt()))

    companion object {

        private val TAG = "PacketHandler"
        private val DEBUG = XService.COMM_DEBUG
        private val CRASH_UNKNOWN = false
    }
}
