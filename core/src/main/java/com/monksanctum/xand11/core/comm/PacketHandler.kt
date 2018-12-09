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

import org.monksanctum.xand11.core.*
import org.monksanctum.xand11.core.Utils.unsigned
import org.monksanctum.xand11.errors.XError

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
        (msg.obj as PacketReader).also { packet ->
            val handler = mDispatcher.getHandler(msg.what)
            if (handler != null) {
                try {
                    if (DEBUG)
                        Platform.logd(TAG, "Handling " + Request.getName(msg.what) + " "
                                + packet.length)
                    count++
                    t("Handle(${ Request.getName(msg.what)})") {
                        handler.handleRequest(mClient, packet, mPacketWriter)
                    }
                    if (packet.remaining != 0) {
                        Platform.logw(TAG, "${packet.remaining} bytes unhandled in "
                                + Request.getName(msg.what))
                    }
                    if (mPacketWriter.length != 0) {
                        if (ClientListener.PASSTHROUGH) {
                            ClientListener.sReplyTypes.put(msg.what)
                        }
                        t("Reply(${Request.getName(msg.what)})") {
                            handleReply(mPacketWriter)
                        }
                    }
                } catch (e: XError) {
                    e.setPacket(packet)
                    handleError(e, mPacketWriter)
                }

            } else {
                count++
                Platform.logw(TAG, "Unhandled message " + Request.getName(msg.what) + " " + msg.what)
                if (CRASH_UNKNOWN) {
                    throw PacketException(packet)
                }
            }
            packet.close()
        }
        mPacketWriter.reset()
    }

    private fun handleError(e: XError, writer: PacketWriter) {
        Platform.logw(TAG, "XError!", e)
        synchronized(mWriter) {
            try {
                mWriter.writeByte(Event.ERROR.code)
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
        if (DEBUG) Platform.logd(TAG, "handleReply")
        sendPacket(Event.REPLY, writer)
    }

    fun sendPacket(type: Event, writer: PacketWriter) {
        if (DEBUG)
            Platform.logd(TAG, "sendPacket " + Event.getName(type.code.toInt()) + " "
                    + writer.minorOpCode + " " + count + " " + (writer.WordCount - 6))
        synchronized(mWriter) {
            try {
                mWriter.writeByte(type.code)
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
        private val DEBUG = COMM_DEBUG
        private val CRASH_UNKNOWN = false
    }
}
