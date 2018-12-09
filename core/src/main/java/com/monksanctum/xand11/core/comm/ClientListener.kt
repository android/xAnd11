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
import org.monksanctum.xand11.extension.ExtensionManager.ExtensionInfo
import org.monksanctum.xand11.fonts.FontProtocol

/**
 * Manages the [Socket] for a given client.
 */
class ClientListener(private val mConnection: Socket,
                     private val mClient: Client) : () -> Unit {
    val writer: XProtoWriter
    private var mServerConnection: Socket? = null
    private var mRunning: Boolean = false
    private val mReader: XProtoReader

    private val mHandlerThread: HandlerThread
    private val mHandler: PacketHandler
    private var mBigRequests = false

    val requestCount: Int
        get() = mHandler.count

    init {
        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null
        try {
            inputStream = mConnection.getInputStream()
            outputStream = mConnection.getOutputStream()
            if (PASSTHROUGH) {
                mServerConnection = Socket("127.0.0.1", 6000)
                Platform.startThread(PassthreadThread(mServerConnection!!.getInputStream(),
                        mConnection.getOutputStream())::run)
            }
            mReader = if (PASSTHROUGH) PassthroughReader(inputStream,
                        mServerConnection!!.getOutputStream())
                    else XProtoReader(inputStream)
            writer = if (PASSTHROUGH) DummyWriter()
                    else XProtoWriter(outputStream)
        } catch (e: IOException) {
            // TODO: Handle better...
            throw RuntimeException(e)
        }

        // TODO: Do we want a handler thread for each client? Probably not.
        mHandlerThread = HandlerThread("ClientHandler:" + mConnection.getHostAddress())
        mHandlerThread.start()
        mHandler = PacketHandler(mClient.clientManager.dispatcher, mClient, this,
                writer, mHandlerThread.getLooper())
    }

    fun sendPacket(type: Event, writer: PacketWriter) {
        mHandler.sendPacket(type, writer)
    }

    override fun invoke() {
        var errorCt = 0
        if (DEBUG) Platform.logd(TAG, "run")
        try {
            val byteOrder = mReader.readByte()
            if (byteOrder == MSB) {
                mReader.setMsb(true)
                writer.setMsb(true)
            } else if (byteOrder == LSB) {
                mReader.setMsb(false)
                writer.setMsb(false)
            } else {
                throw IOException("Invalid byte order")
            }
            mReader.readByte()
            val authManager = mClient.clientManager.authManager
            mRunning = authManager.handleAuth(mReader, writer, mConnection) || PASSTHROUGH

            if (DEBUG) Platform.logd(TAG, "Waiting for packets...")
            while (mRunning) {
                try {
                    queuePacket(readPacket())
                } catch (e: XProtoReader.ReadException) {
                    if (++errorCt == 4) {
                        throw e
                    }
                }

            }
        } catch (e: IOException) {
            if (DEBUG) Platform.logd(TAG, "IOProblem from Client", e)
            onIOProblem()
        }

        if (DEBUG) Platform.logd(TAG, "Listening thread stopping")
    }

    fun setBigRequestEnabled() {
        mBigRequests = true
    }

    private fun queuePacket(packetReader: PacketReader) {
        if (DEBUG) Platform.logd(TAG, "Queue packet: " + packetReader.majorOpCode)
        mHandler.sendPacket(packetReader)
    }

    @Throws(XProtoReader.ReadException::class)
    private fun readPacket(): PacketReader {
        val major = mReader.readByte()
        val minor = mReader.readByte()
        var length = mReader.readCard16()
        if (mBigRequests && length == 0) {
            length = mReader.readCard32()
        }
        if (DEBUG) Platform.logd(TAG, "Reading $major $minor $length")
        val packet = PacketReader(major, minor)
        packet.readBytes(mReader, (length - 1) * 4)
        return packet
    }

    /**
     * Spins up a thread to start listening to the client constantly.
     */
    fun startServing() {
        if (DEBUG) Platform.logd(TAG, "startServing")
        mRunning = true
        Platform.startThread(this)
    }

    fun onIOProblem() {
        if (DEBUG) Platform.logd(TAG, "onIOProblem")
        mRunning = false
        try {
            mConnection.close()
            if (PASSTHROUGH) {
                mServerConnection!!.close()
            }
        } catch (e: IOException) {
        }

        mHandlerThread.quitSafely()
        mClient.onDeath()
    }

    private inner class PassthreadThread(inputStream: InputStream, outputStream: OutputStream)  {
        private val mInput: XProtoReader
        private val mOutput: XProtoWriter

        init {
            //            mInput = new PassthroughReader(inputStream, outputStream);
            //            mOutput = new DummyWriter();
            mInput = XProtoReader(inputStream)
            mOutput = XProtoWriter(outputStream)
        }

        fun run() {
            try {
                if (false) {
                    while (true) {
                        mOutput.writeByte(mInput.readByte())
                    }
                }
                mOutput.writeByte(mInput.readByte())
                mOutput.writeByte(mInput.readByte())
                mOutput.writeCard16(mInput.readCard16())
                mOutput.writeCard16(mInput.readCard16())
                var length = mInput.readCard16()
                mOutput.writeCard16(length)
                val info = XServerInfo()
                PacketReader(0.toByte(), 0.toByte()).also { reader ->
                    reader.readBytes(mInput, length * 4)
                    info.read(reader)
                    if (reader.remaining != 0) {
                        throw RuntimeException("${reader.remaining} leftover bytes")
                    }
                    reader.close()
                }
                Platform.logd(TAG, info.toString())

                val writer = PacketWriter(mOutput)
                info.write(writer)
                if (writer.WordCount != length) {
                    throw RuntimeException("Different lengths ${writer.WordCount} $length")
                }
                writer.flush()
                //                mOutput.writePaddedString(mInput.readPaddedString(length));
                //                for (int i = 0; i < length; i++) {
                //                    mOutput.writeByte(mInput.readByte());
                //                }
                //                mInput.setDebug(true);
                //                mOutput.setDebug(true);
                while (true) {
                    val type = mInput.readByte()
                    val code = mInput.readByte()
                    val sequence = mInput.readCard16()
                    val after = mInput.readCard32()
                    length = if (type == Event.REPLY.code) after else 0
                    Platform.logd(TAG, "Actual Service Message " + Event.getName(unsigned(type))
                            + " " + code + " " + sequence + " " + length)
                    mOutput.writeByte(type)
                    mOutput.writeByte(code)
                    mOutput.writeCard16(sequence)
                    mOutput.writeCard32(after)
                    var request = -1
                    if (type == Event.REPLY.code) {
                        request = sReplyTypes.poll(250)
                    }
                    val N = length * 4 + 24
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
                        Platform.logd(TAG, String.format("Press (%d, %x %x %x (%d %d) (%d %d) %x) ", after,
                                root, id, child, rootX, rootY, x, y, state));

                    } else */if (request == Request.LIST_FONTS.code.toInt()) {
                        val num = mInput.readCard16()
                        mOutput.writeCard16(num)
                        mOutput.writeString(mInput.readString(22))
                        var ct = 0
                        Platform.logd(TAG, "Reading font response")
                        for (i in 0 until num) {
                            val n = mInput.readByte()
                            mOutput.writeByte(n)
                            val str = mInput.readString(n.toInt())
                            Platform.logd(TAG, "Font response $str")
                            mOutput.writeString(str)
                            ct += n + 1
                        }
                        Platform.logd(TAG, "End font response")
                        while (ct % 4 != 0) {
                            mOutput.writeByte(mInput.readByte())
                            ct++
                        }
                    } else if (request == Request.LIST_FONTS_WITH_INFO.code.toInt() || request == Request.QUERY_FONT.code.toInt()) {
                        PacketReader(type, code).also { r ->
                            r.readBytes(mInput, N)
                            Platform.logd("FontManager", "Actual server font $N")
                            FontProtocol.logInfo("FontActual", r)
                            Platform.logd("FontManager", "Remaining: " + r.remaining)

                            r.reset()
                            for (i in 0 until N) {
                                mOutput.writeByte(r.readByte())
                            }
                            r.close()
                        }
                    } else if (request == Request.QUERY_EXTENSION.code.toInt()) {
                        PacketReader(type, code).also { r ->
                            r.readBytes(mInput, N)
                            Platform.logd("ExtensionManager", "Actual server extension $N")
                            val e = ExtensionInfo()
                            e.read(r)
                            Platform.logd("ExtensionManager", "Extension: $e")

                            r.reset()
                            for (i in 0 until N) {
                                mOutput.writeByte(r.readByte())
                            }
                            r.close()
                        }
                    } else if (request == Request.GET_INPUT_FOCUS.code.toInt()) {
                        PacketReader(type, code).also { r ->
                            r.readBytes(mInput, N)
                            Platform.logd("WindowFocus", "Focus: " + r.minorOpCode + " " + r.readCard32())

                            r.reset()
                            for (i in 0 until N) {
                                mOutput.writeByte(r.readByte())
                            }
                            r.close()
                        }
                    } else if (request == Request.GET_PROPERTY.code.toInt()) {
                        PacketReader(type, code).also { r ->
                            r.readBytes(mInput, N)
                            Platform.logd("WindowProtocol", "Property " + r.minorOpCode + " "
                                    + r.readCard32() + " " + r.readCard32())

                            r.reset()
                            for (i in 0 until N) {
                                mOutput.writeByte(r.readByte())
                            }
                            r.close()
                        }
                    } else {
                        for (i in 0 until N) {
                            mOutput.writeByte(mInput.readByte())
                        }
                    }
                    //                    mOutput.writeString(mInput.readString(length * 4 + 24));
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
    }

    companion object {

        private val TAG = "ClientListener"
        private val DEBUG = COMM_DEBUG

        // Used to pass all bytes onto another X server to monitor the protocol.
        val PASSTHROUGH = false

        private val MSB: Byte = 0x42
        private val LSB: Byte = 0x6C

        val sReplyTypes: BlockingQueue = BlockingQueue()
    }
}
