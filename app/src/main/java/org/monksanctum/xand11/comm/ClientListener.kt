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

import android.os.HandlerThread
import android.util.Log

import org.monksanctum.xand11.Client
import org.monksanctum.xand11.XServerInfo
import org.monksanctum.xand11.XService
import org.monksanctum.xand11.extension.ExtensionManager.ExtensionInfo
import org.monksanctum.xand11.fonts.FontProtocol

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.util.LinkedList
import java.util.Queue

import org.monksanctum.xand11.Utils.unsigned
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * Manages the [Socket] for a given client.
 */
class ClientListener(private val mConnection: Socket, private val mClient: Client) : Thread() {
    private val mReader: XProtoReader
    val writer: XProtoWriter
    private var mServerConnection: Socket? = null
    private var mRunning: Boolean = false

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
                PassthreadThread(mServerConnection!!.getInputStream(),
                        mConnection.getOutputStream()).start()
            }
            mReader = if (PASSTHROUGH)
                PassthroughReader(inputStream,
                        mServerConnection!!.getOutputStream())
            else
                XProtoReader(inputStream)
            writer = if (PASSTHROUGH)
                DummyWriter()
            else
                XProtoWriter(outputStream)
        } catch (e: IOException) {
            // TODO: Handle better...
            throw RuntimeException(e)
        }

        // TODO: Do we want a handler thread for each client? Probably not.
        mHandlerThread = HandlerThread("ClientHandler:" + mConnection.inetAddress.toString())
        mHandlerThread.start()
        mHandler = PacketHandler(mClient.clientManager.dispatcher, mClient, this,
                writer, mHandlerThread.looper)
    }

    fun sendPacket(type: Byte, writer: PacketWriter) {
        mHandler.sendPacket(type, writer)
    }

    override fun run() {
        var errorCt = 0
        if (DEBUG) Log.d(TAG, "run")
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

            if (DEBUG) Log.d(TAG, "Waiting for packets...")
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
            if (DEBUG) Log.e(TAG, "IOProblem from Client", e)
            onIOProblem()
        }

        if (DEBUG) Log.d(TAG, "Listening thread stopping")
    }

    fun setBigRequestEnabled() {
        mBigRequests = true
    }

    private fun queuePacket(packetReader: PacketReader) {
        if (DEBUG) Log.d(TAG, "Queue packet: " + packetReader.majorOpCode)
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
        if (DEBUG) Log.d(TAG, "Reading $major $minor $length")
        val packet = PacketReader(major, minor)
        packet.readBytes(mReader, (length - 1) * 4)
        return packet
    }

    /**
     * Spins up a thread to start listening to the client constantly.
     */
    fun startServing() {
        if (DEBUG) Log.d(TAG, "startServing")
        mRunning = true
        start()
    }

    fun onIOProblem() {
        if (DEBUG) Log.e(TAG, "onIOProblem")
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

    private inner class PassthreadThread(inputStream: InputStream, outputStream: OutputStream) : Thread() {
        private val mInput: XProtoReader
        private val mOutput: XProtoWriter

        init {
            //            mInput = new PassthroughReader(inputStream, outputStream);
            //            mOutput = new DummyWriter();
            mInput = XProtoReader(inputStream)
            mOutput = XProtoWriter(outputStream)
        }

        override fun run() {
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
                PacketReader(0.toByte(), 0.toByte()).use { reader ->
                    reader.readBytes(mInput, length * 4)
                    info.read(reader)
                    if (reader.remaining != 0) {
                        throw RuntimeException("${reader.remaining} leftover bytes")
                    }
                }
                Log.d(TAG, info.toString())

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
                    length = if (type == Event.REPLY) after else 0
                    Log.d(TAG, "Actual Service Message " + Event.getName(unsigned(type)) + " "
                            + code + " " + sequence + " " + length)
                    mOutput.writeByte(type)
                    mOutput.writeByte(code)
                    mOutput.writeCard16(sequence)
                    mOutput.writeCard32(after)
                    var request = -1
                    if (type == Event.REPLY) {
                        try {
                            request = sReplyTypes.poll(250, TimeUnit.MILLISECONDS)
                        } catch (e: InterruptedException) {
                        }
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
                        Log.d(TAG, String.format("Press (%d, %x %x %x (%d %d) (%d %d) %x) ", after,
                                root, id, child, rootX, rootY, x, y, state));

                    } else */if (request == Request.LIST_FONTS.toInt()) {
                        val num = mInput.readCard16()
                        mOutput.writeCard16(num)
                        mOutput.writeString(mInput.readString(22))
                        var ct = 0
                        Log.d(TAG, "Reading font response")
                        for (i in 0 until num) {
                            val n = mInput.readByte()
                            mOutput.writeByte(n)
                            val str = mInput.readString(n.toInt())
                            Log.d(TAG, "Font response $str")
                            mOutput.writeString(str)
                            ct += n + 1
                        }
                        Log.d(TAG, "End font response")
                        while (ct % 4 != 0) {
                            mOutput.writeByte(mInput.readByte())
                            ct++
                        }
                    } else if (request == Request.LIST_FONTS_WITH_INFO.toInt() || request == Request.QUERY_FONT.toInt()) {
                        PacketReader(type, code).use { r ->
                            r.readBytes(mInput, N)
                            Log.d("FontManager", "Actual server font $N")
                            FontProtocol.logInfo("FontActual", r)
                            Log.d("FontManager", "Remaining: " + r.remaining)

                            r.reset()
                            for (i in 0 until N) {
                                mOutput.writeByte(r.readByte())
                            }
                        }
                    } else if (request == Request.QUERY_EXTENSION.toInt()) {
                        PacketReader(type, code).use { r ->
                            r.readBytes(mInput, N)
                            Log.d("ExtensionManager", "Actual server extension $N")
                            val e = ExtensionInfo()
                            e.read(r)
                            Log.d("ExtensionManager", "Extension: $e")

                            r.reset()
                            for (i in 0 until N) {
                                mOutput.writeByte(r.readByte())
                            }
                        }
                    } else if (request == Request.GET_INPUT_FOCUS.toInt()) {
                        PacketReader(type, code).use { r ->
                            r.readBytes(mInput, N)
                            Log.d("WindowFocus", "Focus: " + r.minorOpCode + " " + r.readCard32())

                            r.reset()
                            for (i in 0 until N) {
                                mOutput.writeByte(r.readByte())
                            }
                        }
                    } else if (request == Request.GET_PROPERTY.toInt()) {
                        PacketReader(type, code).use { r ->
                            r.readBytes(mInput, N)
                            Log.d("WindowProtocol", "Property " + r.minorOpCode + " "
                                    + r.readCard32() + " " + r.readCard32())

                            r.reset()
                            for (i in 0 until N) {
                                mOutput.writeByte(r.readByte())
                            }
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
        private val DEBUG = XService.COMM_DEBUG

        // Used to pass all bytes onto another X server to monitor the protocol.
        val PASSTHROUGH = false

        private val MSB: Byte = 0x42
        private val LSB: Byte = 0x6C

        val sReplyTypes: BlockingQueue<Int> = LinkedBlockingQueue()
    }
}
