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

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import org.monksanctum.xand11.Client;
import org.monksanctum.xand11.Dispatcher;
import org.monksanctum.xand11.Time;
import org.monksanctum.xand11.XService;
import org.monksanctum.xand11.errors.XError;

import static org.monksanctum.xand11.Time.t;
import static org.monksanctum.xand11.Utils.unsigned;

public class PacketHandler extends Handler {

    private static final String TAG = "PacketHandler";
    private static final boolean DEBUG = XService.COMM_DEBUG;
    private static final boolean CRASH_UNKNOWN = false;

    private final Dispatcher mDispatcher;
    private final Client mClient;
    private final XProtoWriter mWriter;
    private final ClientListener mListener;
    private final PacketWriter mPacketWriter;
    private int mCount = 0;

    public PacketHandler(Dispatcher dispatcher, Client client, ClientListener listener,
            XProtoWriter writer, Looper looper) {
        super(looper);
        mDispatcher = dispatcher;
        mClient = client;
        mListener = listener;
        mWriter = writer;
        mPacketWriter = new PacketWriter(mWriter);
    }

    public void sendPacket(PacketReader reader) {
        obtainMessage(unsigned(reader.getMajorOpCode()), reader).sendToTarget();
    }

    @Override
    public void handleMessage(Message msg) {
        try (PacketReader packet = (PacketReader) msg.obj) {
            Dispatcher.PacketHandler handler = mDispatcher.getHandler(msg.what);
            if (handler != null) {
                try {
                    if (DEBUG) Log.d(TAG, "Handling " + Request.getName(msg.what) + " "
                            + packet.getLength());
                    mCount++;
                    try (Time ignored = t(String.format("Handle(%s)", Request.getName(msg.what)))) {
                        handler.handleRequest(mClient, packet, mPacketWriter);
                    }
                    if (packet.getRemaining() != 0) {
                        Log.w(TAG, packet.getRemaining() + " bytes unhandled in "
                                + Request.getName(msg.what));
                    }
                    if (mPacketWriter.getLength() != 0) {
                        if (ClientListener.PASSTHROUGH) {
                            synchronized (ClientListener.sReplyTypes) {
                                ClientListener.sReplyTypes.add(msg.what);
                                ClientListener.sReplyTypes.notify();
                            }
                        }
                        try (Time ignored
                                     = t(String.format("Reply(%s)", Request.getName(msg.what)))) {
                            handleReply(mPacketWriter);
                        }
                    }
                } catch (XError e) {
                    e.setPacket(packet);
                    handleError(e, mPacketWriter);
                }
            } else {
                mCount++;
                Log.w(TAG, "Unhandled message " + Request.getName(msg.what) + " " + msg.what);
                if (CRASH_UNKNOWN) {
                    throw new PacketException(packet);
                }
            }
        }
        mPacketWriter.reset();
    }

    public int getCount() {
        return mCount;
    }

    private void handleError(XError e, PacketWriter writer) {
        Log.w(TAG, "XError!", e);
        synchronized (mWriter) {
            try {
                mWriter.writeByte(Event.ERROR);
                mWriter.writeByte(e.getCode());
                mWriter.writeCard16(mCount & 0xffff);
                e.write(writer);
                writer.flush();
            } catch (XProtoWriter.WriteException exception) {
                mListener.onIOProblem();
            }
        }
    }

    private void handleReply(PacketWriter writer) {
        if (DEBUG) Log.d(TAG, "handleReply");
        sendPacket(Event.REPLY, writer);
    }

    public void sendPacket(byte type, PacketWriter writer) {
        if (DEBUG) Log.d(TAG, "sendPacket " + Event.getName(type) + " "
                + writer.getMinorOpCode() + " " + mCount + " " + (writer.get4Length() - 6));
        synchronized (mWriter) {
            try {
                mWriter.writeByte(type);
                mWriter.writeByte(writer.getMinorOpCode());
                mWriter.writeCard16(mCount & 0xffff);
                if (type == Event.REPLY) {
                    mWriter.writeCard32(writer.get4Length() - 6);
                }
                writer.flush();
            } catch (XProtoWriter.WriteException e) {
                // This one can.
                mListener.onIOProblem();
            }
        }
    }

    private static class PacketException extends RuntimeException {
        // TODO: Print usefulness from the packet
        private final PacketReader mPacket;

        public PacketException(PacketReader packet) {
            super("Unhandled packet " + Request.getName(packet.getMajorOpCode()));
            mPacket = packet;
        }
    }
}
