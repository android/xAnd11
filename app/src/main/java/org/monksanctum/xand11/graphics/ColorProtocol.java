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

package org.monksanctum.xand11.graphics;

import org.monksanctum.xand11.Client;
import org.monksanctum.xand11.Dispatcher.PacketHandler;
import org.monksanctum.xand11.comm.PacketReader;
import org.monksanctum.xand11.comm.PacketWriter;
import org.monksanctum.xand11.comm.Request;
import org.monksanctum.xand11.comm.XProtoWriter.WriteException;
import org.monksanctum.xand11.errors.ColorMapError;
import org.monksanctum.xand11.errors.XError;
import org.monksanctum.xand11.graphics.ColorMaps.Color;
import org.monksanctum.xand11.graphics.ColorMaps.ColorMap;

public class ColorProtocol implements PacketHandler {

    private static final byte[] HANDLED_OPS = {
            Request.QUERY_COLORS,
            Request.ALLOC_COLOR,
            Request.LOOKUP_COLOR,
    };

    private final GraphicsManager mGraphics;
    private final ColorMaps mColorMaps;

    public ColorProtocol(GraphicsManager manager) {
        mGraphics = manager;
        mColorMaps = mGraphics.getColorMap();
    }

    @Override
    public byte[] getOpCodes() {
        return HANDLED_OPS;
    }

    @Override
    public void handleRequest(Client client, PacketReader reader, PacketWriter writer)
            throws XError {
        switch (reader.getMajorOpCode()) {
            case Request.QUERY_COLORS:
                handleQueryColors(reader, writer);
                break;
            case Request.ALLOC_COLOR:
                handleAllocColor(reader, writer);
                break;
            case Request.LOOKUP_COLOR:
                handleLookupColor(reader, writer);
                break;
        }
    }

    private void handleLookupColor(PacketReader reader, PacketWriter writer) throws XError {
        ColorMap map = mColorMaps.get(reader.readCard32());
        int len = reader.readCard16();
        reader.readPadding(2);
        String str = reader.readPaddedString(len);
        Integer color = ColorMap.sColorLookup.get(str.toLowerCase());
        if (color == null) {
            throw new XError(XError.NAME, "Color not found: " + str) {
                @Override
                public int getExtraData() {
                    return 0;
                }
            };
        }

        int	r = android.graphics.Color.red(color);
        int	g = android.graphics.Color.green(color);
        int	b = android.graphics.Color.blue(color);
        writer.writeCard16((r << 8) | r);
        writer.writeCard16((g << 8) | g);
        writer.writeCard16((b << 8) | b);
        writer.writeCard16((r << 8) | r);
        writer.writeCard16((g << 8) | g);
        writer.writeCard16((b << 8) | b);
        writer.writePadding(12);
    }

    private void handleQueryColors(PacketReader reader, PacketWriter writer) throws XError {
        ColorMap map = mColorMaps.get(reader.readCard32());
        int n = reader.getLength() / 4 - 1;
        writer.writeCard16(n);
        writer.writePadding(22);
        synchronized (map) {
            for (int i = 0; i < n; i++) {
                int index = reader.readCard32();
                Color color = map.getColor(index);
                try {
                    color.write(writer);
                } catch (WriteException e) {
                }
            }
        }
    }

    private void handleAllocColor(PacketReader reader, PacketWriter writer)
            throws ColorMapError {
        ColorMap map = mColorMaps.get(reader.readCard32());
        int r = reader.readCard16();
        int g = reader.readCard16();
        int b = reader.readCard16();
        reader.readPadding(2);

        synchronized (map) {
            Color color = map.getColor(r, g, b);

            try {
                //Color color = map.colors.get(color);
                color.write(writer);
                int c = color.color();
                writer.writeCard32(c);
            } catch (WriteException e) {
            }
        }
        //writer.writeCard32(index);
        writer.writePadding(12);
    }
}
