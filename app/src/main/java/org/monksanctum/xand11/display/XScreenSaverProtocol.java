package org.monksanctum.xand11.display;

import org.monksanctum.xand11.Client;
import org.monksanctum.xand11.Dispatcher;
import org.monksanctum.xand11.comm.PacketReader;
import org.monksanctum.xand11.comm.PacketWriter;
import org.monksanctum.xand11.comm.Request;
import org.monksanctum.xand11.errors.XError;

public class XScreenSaverProtocol implements Dispatcher.PacketHandler{

    private static final byte[] HANDLED_OPS = new byte[] {
            Request.GET_SCREEN_SAVER,
    };

    @Override
    public byte[] getOpCodes() {
        return HANDLED_OPS;
    }

    @Override
    public void handleRequest(Client client, PacketReader reader, PacketWriter writer)
            throws XError {
        switch (reader.getMajorOpCode()) {
            case Request.GET_SCREEN_SAVER:
                handleGetScreenSaver(reader, writer);
                break;
        }
    }

    private void handleGetScreenSaver(PacketReader reader, PacketWriter writer) {
        writer.writeCard16(0); // Timeout
        writer.writeCard16(0); // interval
        writer.writeByte((byte) 0); // Prefer-blanking
        writer.writeByte((byte) 0); // Allow-exposures
        writer.writePadding(18);
    }
}
