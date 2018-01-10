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

import android.os.HandlerThread;
import android.util.Log;

import org.monksanctum.xand11.Client;
import org.monksanctum.xand11.XServerInfo;
import org.monksanctum.xand11.XService;
import org.monksanctum.xand11.extension.ExtensionManager.ExtensionInfo;
import org.monksanctum.xand11.fonts.FontProtocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;

import static org.monksanctum.xand11.Utils.unsigned;

/**
 * Manages the {@link Socket} for a given client.
 */
public class ClientListener extends Thread {

    private static final String TAG = "ClientListener";
    private static final boolean DEBUG = XService.COMM_DEBUG;

    // Used to pass all bytes onto another X server to monitor the protocol.
    public static final boolean PASSTHROUGH = false;

    private static final byte MSB = 0x42;
    private static final byte LSB = 0x6C;

    private final Socket mConnection;
    private final Client mClient;
    private final XProtoReader mReader;
    private final XProtoWriter mWriter;
    private Socket mServerConnection = null;
    private boolean mRunning;

    private HandlerThread mHandlerThread;
    private PacketHandler mHandler;
    private boolean mBigRequests = false;

    public ClientListener(Socket connection, Client client) {
        mConnection = connection;
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            inputStream = mConnection.getInputStream();
            outputStream = mConnection.getOutputStream();
            if (PASSTHROUGH) {
                mServerConnection = new Socket("127.0.0.1", 6000);
                new PassthreadThread(mServerConnection.getInputStream(),
                        mConnection.getOutputStream()).start();
            }
            mReader = PASSTHROUGH ? new PassthroughReader(inputStream,
                    mServerConnection.getOutputStream())
                    : new XProtoReader(inputStream);
            mWriter = PASSTHROUGH ? new DummyWriter()
                    : new XProtoWriter(outputStream);
        } catch (IOException e) {
            // TODO: Handle better...
            throw new RuntimeException(e);
        }
        mClient = client;
        // TODO: Do we want a handler thread for each client? Probably not.
        mHandlerThread = new HandlerThread("ClientHandler:"
                + connection.getInetAddress().toString());
        mHandlerThread.start();
        mHandler = new PacketHandler(client.getClientManager().getDispatcher(), client, this,
                mWriter, mHandlerThread.getLooper());
    }

    public XProtoWriter getWriter() {
        return mWriter;
    }

    public int getRequestCount() {
        return mHandler.getCount();
    }

    public void sendPacket(byte type, PacketWriter writer) {
        mHandler.sendPacket(type, writer);
    }

    @Override
    public void run() {
        int errorCt = 0;
        if (DEBUG) Log.d(TAG, "run");
        try {
            byte byteOrder = mReader.readByte();
            if (byteOrder == MSB) {
                mReader.setMsb(true);
                mWriter.setMsb(true);
            } else if (byteOrder == LSB) {
                mReader.setMsb(false);
                mWriter.setMsb(false);
            } else {
                throw new IOException("Invalid byte order");
            }
            mReader.readByte();
            AuthManager authManager = mClient.getClientManager().getAuthManager();
            mRunning = authManager.handleAuth(mReader, mWriter, mConnection)
                    || PASSTHROUGH;

            if (DEBUG) Log.d(TAG, "Waiting for packets...");
            while (mRunning) {
                try {
                    queuePacket(readPacket());
                } catch (XProtoReader.ReadException e) {
                    if (++errorCt == 4) {
                        throw e;
                    }
                }
            }
        } catch (IOException e) {
            if (DEBUG) Log.e(TAG, "IOProblem from Client", e);
            onIOProblem();
        }
        if (DEBUG) Log.d(TAG, "Listening thread stopping");
    }

    public void setBigRequestEnabled() {
        mBigRequests = true;
    }

    private void queuePacket(PacketReader packetReader) {
        if (DEBUG) Log.d(TAG, "Queue packet: " + packetReader.getMajorOpCode());
        mHandler.sendPacket(packetReader);
    }

    private PacketReader readPacket() throws XProtoReader.ReadException {
        byte major = mReader.readByte();
        byte minor = mReader.readByte();
        int length = mReader.readCard16();
        if (mBigRequests && length == 0) {
            length = mReader.readCard32();
        }
        if (DEBUG) Log.d(TAG, "Reading " + major + " " + minor + " " + length);
        PacketReader packet = new PacketReader(major, minor);
        packet.readBytes(mReader, (length - 1) * 4);
        return packet;
    }

    /**
     * Spins up a thread to start listening to the client constantly.
     */
    public void startServing() {
        if (DEBUG) Log.d(TAG, "startServing");
        mRunning = true;
        start();
    }

    public void onIOProblem() {
        if (DEBUG) Log.e(TAG, "onIOProblem");
        mRunning = false;
        try {
            mConnection.close();
            if (PASSTHROUGH) {
                mServerConnection.close();
            }
        } catch (IOException e) {
        }
        mHandlerThread.quitSafely();
        mClient.onDeath();
    }

    public static final Queue<Integer> sReplyTypes = new LinkedList<>();

    private class PassthreadThread extends Thread {
        private final XProtoReader mInput;
        private final XProtoWriter mOutput;

        public PassthreadThread(InputStream inputStream, OutputStream outputStream) {
//            mInput = new PassthroughReader(inputStream, outputStream);
//            mOutput = new DummyWriter();
            mInput = new XProtoReader(inputStream);
            mOutput = new XProtoWriter(outputStream);
        }

        @Override
        public void run() {
            try {
                if (false) {
                    while (true) {
                        mOutput.writeByte(mInput.readByte());
                    }
                }
                mOutput.writeByte(mInput.readByte());
                mOutput.writeByte(mInput.readByte());
                mOutput.writeCard16(mInput.readCard16());
                mOutput.writeCard16(mInput.readCard16());
                int length = mInput.readCard16();
                mOutput.writeCard16(length);
                XServerInfo info = new XServerInfo();
                try (PacketReader reader = new PacketReader((byte) 0, (byte) 0)) {
                    reader.readBytes(mInput, length * 4);
                    info.read(reader);
                    if (reader.getRemaining() != 0) {
                        throw new RuntimeException(reader.getRemaining() + " leftover bytes");
                    }
                }
                Log.d(TAG, info.toString());

                PacketWriter writer = new PacketWriter(mOutput);
                info.write(writer);
                if (writer.get4Length() != length) {
                    throw new RuntimeException("Different lengths " + writer.get4Length() + " "
                            + length);
                }
                writer.flush();
//                mOutput.writePaddedString(mInput.readPaddedString(length));
//                for (int i = 0; i < length; i++) {
//                    mOutput.writeByte(mInput.readByte());
//                }
//                mInput.setDebug(true);
//                mOutput.setDebug(true);
                while (true) {
                    byte type = mInput.readByte();
                    byte code = mInput.readByte();
                    int sequence = mInput.readCard16();
                    int after = mInput.readCard32();
                    length = type == Event.REPLY ? after : 0;
                    Log.d(TAG, "Actual Service Message " + Event.getName(unsigned(type)) + " "
                            + code + " " + sequence + " " + length);
                    mOutput.writeByte(type);
                    mOutput.writeByte(code);
                    mOutput.writeCard16(sequence);
                    mOutput.writeCard32(after);
                    int request = -1;
                    if (type == Event.REPLY) {
                        synchronized (sReplyTypes) {
                            if (sReplyTypes.size() == 0) {
                                try {
                                    sReplyTypes.wait(250);
                                } catch (InterruptedException e) {
                                }
                            }
                            if (sReplyTypes.size() != 0) {
                                request = sReplyTypes.poll();
                            }
                        }
                    }
                    final int N = length * 4 + 24;
                    /*if (type == Event.KEY_PRESS) {
                        int root = mInput.readCard32();
                        int id = mInput.readCard32();
                        int child = mInput.readCard32();
                        int rootX = mInput.readCard16();
                        int rootY = mInput.readCard16();
                        int x = mInput.readCard16();
                        int y = mInput.readCard16();
                        int state = mInput.readCard16();
                        mInput.readByte();
                        mInput.readByte();
                        mOutput.writeCard32(root);
                        mOutput.writeCard32(id);
                        mOutput.writeCard32(child);
                        mOutput.writeCard16(rootX);
                        mOutput.writeCard16(rootY);
                        mOutput.writeCard16(x);
                        mOutput.writeCard16(y);
                        mOutput.writeCard16(state);
                        writer.writeByte((byte) 1); // Same screen.
                        writer.writePadding(1);
                        Log.d(TAG, String.format("Press (%d, %x %x %x (%d %d) (%d %d) %x) ", after,
                                root, id, child, rootX, rootY, x, y, state));

                    } else */if (request == Request.LIST_FONTS) {
                        int num = mInput.readCard16();
                        mOutput.writeCard16(num);
                        mOutput.writeString(mInput.readString(22));
                        int ct = 0;
                        Log.d(TAG, "Reading font response");
                        for (int i = 0; i < num; i++) {
                            byte n = mInput.readByte();
                            mOutput.writeByte(n);
                            String str = mInput.readString(n);
                            Log.d(TAG, "Font response " + str);
                            mOutput.writeString(str);
                            ct += n + 1;
                        }
                        Log.d(TAG, "End font response");
                        while ((ct % 4) != 0) {
                            mOutput.writeByte(mInput.readByte());
                            ct++;
                        }
                    } else if (request == Request.LIST_FONTS_WITH_INFO || request == Request.QUERY_FONT) {
                        try (PacketReader r = new PacketReader(type, code)) {
                            r.readBytes(mInput, N);
                            Log.d("FontManager", "Actual server font " + N);
                            FontProtocol.logInfo("FontActual", r);
                            Log.d("FontManager", "Remaining: " + r.getRemaining());

                            r.reset();
                            for (int i = 0; i < N; i++) {
                                mOutput.writeByte(r.readByte());
                            }
                        }
                    } else if (request == Request.QUERY_EXTENSION) {
                        try (PacketReader r = new PacketReader(type, code)) {
                            r.readBytes(mInput, N);
                            Log.d("ExtensionManager", "Actual server extension " + N);
                            ExtensionInfo e = new ExtensionInfo();
                            e.read(r);
                            Log.d("ExtensionManager", "Extension: " + e);

                            r.reset();
                            for (int i = 0; i < N; i++) {
                                mOutput.writeByte(r.readByte());
                            }
                        }
                    } else if (request == Request.GET_INPUT_FOCUS) {
                        try (PacketReader r = new PacketReader(type, code)) {
                            r.readBytes(mInput, N);
                            Log.d("WindowFocus", "Focus: " + r.getMinorOpCode() + " " + r.readCard32());

                            r.reset();
                            for (int i = 0; i < N; i++) {
                                mOutput.writeByte(r.readByte());
                            }
                        }
                    } else if (request == Request.GET_PROPERTY) {
                        try (PacketReader r = new PacketReader(type, code)) {
                            r.readBytes(mInput, N);
                            Log.d("WindowProtocol", "Property " + r.getMinorOpCode() + " "
                                    + r.readCard32() + " " + r.readCard32());

                            r.reset();
                            for (int i = 0; i < N; i++) {
                                mOutput.writeByte(r.readByte());
                            }
                        }
                    } else {
                        for (int i = 0; i < N; i++) {
                            mOutput.writeByte(mInput.readByte());
                        }
                    }
//                    mOutput.writeString(mInput.readString(length * 4 + 24));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
